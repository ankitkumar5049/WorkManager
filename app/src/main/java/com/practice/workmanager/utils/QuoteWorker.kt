package com.practice.workmanager.utils

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class QuoteWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val fallbackAdvice = listOf(
        "Stay positive, work hard, make it happen.",
        "Do one thing every day that scares you.",
        "Take breaks. Your mind needs it."
    )


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val advice = try {
                fetchAdvice()
            } catch (e: Exception) {
                fallbackAdvice.random() // fallback if API fails
            }
            showNotification("Advice of the Hour", advice)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun fetchAdvice(): String {
        val url = URL("https://api.adviceslip.com/advice")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        return connection.inputStream.bufferedReader().use { reader ->
            val response = reader.readText()
            val json = JSONObject(response)
            json.getJSONObject("slip").getString("advice")
        }
    }

    private fun showNotification(title: String, message: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(applicationContext, "quote_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
