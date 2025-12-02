package com.example.smsbulk


import android.app.PendingIntent
import android.content.Intent
import android.telephony.SmsManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader


class SmsWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {


override fun doWork(): Result {
try {
val file = applicationContext.openFileInput("sms_queue.txt")
BufferedReader(InputStreamReader(file)).use { br ->
var line: String?
val sms = SmsManager.getDefault()
while (br.readLine().also { line = it } != null) {
val parts = line!!.split("\t")
if (parts.size < 1) continue
val phone = parts[0].trim()
val msg = if (parts.size > 1) parts[1] else ""
if (phone.isEmpty()) continue


val sentIntent = PendingIntent.getBroadcast(applicationContext, 0, Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE)
val deliveredIntent = PendingIntent.getBroadcast(applicationContext, 0, Intent("SMS_DELIVERED"), PendingIntent.FLAG_IMMUTABLE)


if (msg.length > 160) {
val partsMsgs = sms.divideMessage(msg)
sms.sendMultipartTextMessage(phone, null, partsMsgs, listOf(sentIntent), listOf(deliveredIntent))
} else {
sms.sendTextMessage(phone, null, msg, sentIntent, deliveredIntent)
}


Thread.sleep(1500)
}
}
return Result.success()
} catch (ex: Exception) {
Log.e("SmsWorker","Error sending SMS: ${ex.message}")
return Result.failure()
}
}
}
