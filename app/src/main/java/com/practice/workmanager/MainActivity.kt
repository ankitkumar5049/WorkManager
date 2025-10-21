package com.practice.workmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.work.WorkManager
import com.practice.workmanager.ui.theme.WorkManagerTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private var uploadWorkId: UUID? = null
    var text by mutableStateOf("Uploading images... check logs for progress.")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        // Initialize notification channel (required for NotifyWorker)
        createNotificationChannel()

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        // Example: hardcoded image paths for testing
        val samplePaths = listOf(
            "/storage/emulated/0/Download/test1.jpg",
            "/storage/emulated/0/Download/test2.png"
        )

        // Start the WorkManager chain
        uploadWorkId = viewModel.startWork(this, samplePaths)

        // Observe progress of upload work
        uploadWorkId?.let { id ->
            WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(id)
                .observe(this) { info ->
                    val progress = info.progress.getInt("PROGRESS", 0)
                    Log.d("WorkProgress", "Upload: $progress%")
                    if(progress==100) text = "Upload Complete"
                }
        }

        setContent {
            WorkManagerTheme {
                val insets = WindowInsets.systemBars.asPaddingValues()

                Column(
                    modifier = Modifier
                        .padding(insets)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "upload_channel"
            val channelName = "Upload Notifications"
            val channelDescription = "Notifications for upload completion"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
