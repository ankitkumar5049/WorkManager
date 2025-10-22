package com.practice.workmanager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.practice.workmanager.ui.theme.WorkManagerTheme
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.UUID

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private var uploadWorkId: UUID? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        createReminderChannel()
        // Initialize notification channel (required for NotifyWorker)
        createNotificationChannel()

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        viewModel.scheduleHourlyReminders(this)

        // Example: hardcoded image paths for testing
//        val samplePaths = listOf(
//            "/storage/emulated/0/Download/test1.jpg",
//            "/storage/emulated/0/Download/test2.png"
//        )

        // Start the WorkManager chain
//        uploadWorkId = viewModel.startWork(this, samplePaths)

        // Observe progress of upload work
//        uploadWorkId?.let { id ->
//            WorkManager.getInstance(this)
//                .getWorkInfoByIdLiveData(id)
//                .observe(this) { info ->
//                    val progress = info.progress.getInt("PROGRESS", 0)
//                    Log.d("WorkProgress", "Upload: $progress%")
//                    if(progress==100) text = "Upload Complete"
//                }
//        }

        setContent {
            WorkManagerTheme {
                val insets = WindowInsets.systemBars.asPaddingValues()

                // 🎯 Target time — e.g., 6:00 PM today
                val targetTime = remember {
                    Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 18)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                }

                var remainingTime by remember { mutableStateOf(viewModel.getTimeLeft(targetTime)) }

                LaunchedEffect(Unit) {
                    viewModel.triggerInitialNotification(applicationContext, remainingTime)
                    while (true) {
                        remainingTime = viewModel.getTimeLeft(targetTime)
                        delay(1000)
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(insets)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Time left to leave office:",
                        modifier = Modifier
                            .padding(top = 32.dp, start = 16.dp),
                        fontSize = 24.sp
                    )

                    Text(
                        text = "${viewModel.formatDuration(remainingTime)} hrs",
                        modifier = Modifier
                            .padding(16.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 60.sp
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

    private fun createReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Office Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

}
