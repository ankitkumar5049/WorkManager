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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.practice.workmanager.ui.theme.WorkManagerTheme
import com.practice.workmanager.utils.MainViewModelFactory
import com.practice.workmanager.utils.QuoteWorker
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit

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

        createQuoteChannel()

//         ✅ Schedule the periodic WorkManager task here
        scheduleHourlyQuoteWork()

        // Initialize ViewModel
        viewModel = ViewModelProvider(this, MainViewModelFactory(this))[MainViewModel::class.java]

        viewModel.scheduleHourlyReminders(this)


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
//                    viewModel.triggerInitialNotification(applicationContext, remainingTime)
                    while (true) {
                        remainingTime = viewModel.getTimeLeft(targetTime)
                        delay(1000)
                    }
                }

                var posts by remember { mutableStateOf<List<String>>(emptyList()) }

                LaunchedEffect(Unit) {
                    viewModel.loadPosts()
                    viewModel.posts.collect { posts = it }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(WindowInsets.systemBars.asPaddingValues()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = { viewModel.loadPosts() }) {
                        Text("Fetch Posts")
                    }

                    Spacer(Modifier.height(16.dp))

                    LazyColumn {
                        items(posts) { title ->
                            Text(
                                text = "• $title",
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

//                Column(
//                    modifier = Modifier
//                        .padding(insets)
//                        .fillMaxSize(),
//                    verticalArrangement = Arrangement.Center,
//                    horizontalAlignment = Alignment.CenterHorizontally
//                ) {
//                    Text(
//                        text = "${viewModel.formatDuration(remainingTime)} hrs",
//                        modifier = Modifier
//                            .padding(16.dp),
//                        style = MaterialTheme.typography.headlineMedium,
//                        fontSize = 60.sp
//                    )
//                }
//            }
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

    private fun createQuoteChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "quote_channel",
                "Hourly Quotes",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shows motivational quotes every hour"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun scheduleHourlyQuoteWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val hourlyWork = PeriodicWorkRequestBuilder<QuoteWorker>(
            1, TimeUnit.HOURS // Every 1 hour
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "hourly_quote_work", // unique name
            ExistingPeriodicWorkPolicy.UPDATE, // replace if already scheduled
            hourlyWork
        )
    }


}
