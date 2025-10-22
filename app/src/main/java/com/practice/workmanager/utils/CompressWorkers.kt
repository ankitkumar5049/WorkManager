package com.practice.workmanager.utils

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import java.io.File

class CompressWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val imagePaths = inputData.getStringArray("IMAGE_PATHS") ?: return Result.failure()

        val compressedPaths = mutableListOf<String>()
        for (path in imagePaths) {
            // Compress logic (e.g. BitmapFactory + compress to file)
            val compressedFile = compressImage(File(path))
            compressedPaths.add(compressedFile.path)
        }

        val output = workDataOf("COMPRESSED_PATHS" to compressedPaths.toTypedArray())
        return Result.success(output)
    }

    private fun compressImage(file: File): File {
        // Basic compression simulation
        Thread.sleep(1000) // simulate work
        return file // Return same file for now
    }
}


class UploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val compressedPaths = inputData.getStringArray("COMPRESSED_PATHS") ?: return Result.failure()

        for ((index, path) in compressedPaths.withIndex()) {
            setProgress(workDataOf("PROGRESS" to ((index + 1) * 100 / compressedPaths.size)))
//            uploadImage(File(path))
        }

        return Result.success(workDataOf("UPLOAD_DONE" to true))

    }

    private suspend fun uploadImage(file: File) {
        delay(500) // Simulate upload
        // API call via Retrofit could go here
    }
}

class NotifyWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(applicationContext, "upload_channel")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setContentTitle("Upload Complete")
            .setContentText("All images uploaded successfully!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())

        Log.d("NotifyWorker", "Notification shown successfully")
        return Result.success()
    }
}

