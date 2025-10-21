package com.practice.workmanager

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.practice.workmanager.utils.CompressWorker
import com.practice.workmanager.utils.NotifyWorker
import com.practice.workmanager.utils.UploadWorker
import java.util.UUID

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

}
