package com.example.smsbulk


private val REQUEST_SMS_PERMISSION = 1001


private val pickCsv = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
if (uri != null) {
readCsvAndQueue(uri)
} else {
Toast.makeText(this, "No file picked", Toast.LENGTH_SHORT).show()
}
}


override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)
val btn = Button(this).apply { text = "Pick CSV and Start Sending" }
setContentView(btn)


btn.setOnClickListener {
if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS_PERMISSION)
} else {
pickCsv.launch(arrayOf("text/*","text/csv","application/vnd.ms-excel"))
}
}
}


override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
super.onRequestPermissionsResult(requestCode, permissions, grantResults)
if (requestCode == REQUEST_SMS_PERMISSION) {
if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
pickCsv.launch(arrayOf("text/*","text/csv","application/vnd.ms-excel"))
} else {
Toast.makeText(this, "SMS permission required to send messages", Toast.LENGTH_LONG).show()
}
}
}


private fun readCsvAndQueue(uri: Uri) {
try {
contentResolver.openInputStream(uri)?.use { stream ->
val reader = CSVReader(InputStreamReader(stream))
val rows = reader.readAll()
val entries = mutableListOf<Pair<String,String>>()
for (i in 1 until rows.size) {
val r = rows[i]
if (r.isEmpty()) continue
val phone = r.getOrNull(0)?.trim() ?: continue
val msg = r.getOrNull(1)?.trim() ?: ""
if (phone.isNotEmpty()) entries.add(phone to msg)
}
if (entries.isEmpty()) {
Toast.makeText(this, "No valid rows found", Toast.LENGTH_SHORT).show()
return
}
openFileOutput("sms_queue.txt", MODE_PRIVATE).use { out ->
entries.forEach { out.write("${it.first}\t${it.second}\n".toByteArray()) }
}
val work = OneTimeWorkRequestBuilder<SmsWorker>().build()
WorkManager.getInstance(this).enqueue(work)
Toast.makeText(this, "Queued ${entries.size} messages; worker started", Toast.LENGTH_SHORT).show()
} ?: run {
Toast.makeText(this, "Unable to open file", Toast.LENGTH_SHORT).show()
}
} catch (e: Exception) {
Toast.makeText(this, "Error reading CSV: ${e.message}", Toast.LENGTH_LONG).show()
}
}
}
