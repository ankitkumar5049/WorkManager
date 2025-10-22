package com.practice.workmanager

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.practice.workmanager.utils.CompressWorker
import com.practice.workmanager.utils.NotifyWorker
import com.practice.workmanager.utils.ReminderWorker
import com.practice.workmanager.utils.UploadWorker
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainViewModel : ViewModel() {

    fun startWork(context: Context, imagePaths: List<String>): UUID {
        val compressWork = OneTimeWorkRequestBuilder<CompressWorker>()
            .setInputData(workDataOf("IMAGE_PATHS" to imagePaths.toTypedArray()))
            .build()

        val uploadWork = OneTimeWorkRequestBuilder<UploadWorker>().build()
        val notifyWork = OneTimeWorkRequestBuilder<NotifyWorker>().build()

        WorkManager.getInstance(context)
            .beginWith(compressWork)
            .then(uploadWork)
            .then(notifyWork)
            .enqueue()

        return uploadWork.id
    }


    fun scheduleHourlyReminders(context: Context) {

        val work = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.MINUTES)
            .setInitialDelay(getInitialDelayForNextHour(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "HourlyReminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                work
            )
    }

    private fun getInitialDelayForNextHour(): Long {
        val now = Calendar.getInstance()
        val nextHour = (now.clone() as Calendar).apply {
            add(Calendar.HOUR_OF_DAY, 1)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return nextHour.timeInMillis - now.timeInMillis
    }

    fun getTimeLeft(targetMillis: Long): Long {
        val now = System.currentTimeMillis()
        return (targetMillis - now).coerceAtLeast(0)
    }

    @SuppressLint("DefaultLocale")
    fun formatDuration(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun triggerInitialNotification(applicationContext: Context, remainingTime: Long){
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(applicationContext, "upload_channel")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("Hello World!")
            .setContentText("You'll notify hourly for your remining work hour")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        notificationManager.notify(1001, builder.build())
    }



}
