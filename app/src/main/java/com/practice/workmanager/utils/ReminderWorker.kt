package com.practice.workmanager.utils

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

//    override suspend fun doWork(): Result {
//        // your notification logic here
//        showNotification("This is a test reminder!")
//
//        // re-schedule for 1 minute later
//        val nextWork = OneTimeWorkRequestBuilder<ReminderWorker>()
//            .setInitialDelay(1, TimeUnit.MINUTES)
//            .build()
//
//        WorkManager.getInstance(applicationContext).enqueue(nextWork)
//
//        return Result.success()
//    }

    override suspend fun doWork(): Result {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)

        val message = when {
            hour in 9..17 -> { // between 9:00 and 17:59 (working hours)
                val hoursLeft = 18 - hour - if (minute > 0) 1 else 0
                "$hoursLeft hour${if (hoursLeft != 1) "s" else ""} left to leave office!"
            }
            hour < 9 || (hour == 9 && minute < 30) -> {
                val target = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 30)
                }
                val diffMillis = target.timeInMillis - now.timeInMillis
                val hoursLeft = (diffMillis / (1000 * 60 * 60)).toInt()
                "$hoursLeft hour${if (hoursLeft != 1) "s" else ""} left to go to office!"
            }
            else -> {
                "Workday over! See you tomorrow 👋"
            }
        }

        showNotification(message)
        return Result.success()
    }

    private fun showNotification(message: String) {
        val notification = NotificationCompat.Builder(applicationContext, "reminder_channel")
            .setContentTitle("Office Reminder")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
