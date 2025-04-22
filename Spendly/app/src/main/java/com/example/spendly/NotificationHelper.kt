package com.example.spendly

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.spendly.AddTransactionActivity
import java.text.SimpleDateFormat
import java.util.*

class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"

        const val CHANNEL_BUDGET_ALERTS = "budget_alerts"
        const val CHANNEL_DAILY_REMINDERS = "daily_reminders"

        const val NOTIFICATION_ID_BUDGET_WARNING = 1001
        const val NOTIFICATION_ID_BUDGET_EXCEEDED = 1002
        const val NOTIFICATION_ID_DAILY_REMINDER = 1003

        // Specific date and login for the user
        private val SPECIFIC_DATE = Calendar.getInstance().apply {
            timeInMillis = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .parse("2025-04-22 06:16:31")?.time ?: System.currentTimeMillis()
        }

        // User information
        const val USER_LOGIN = "luqmanbooso"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create notification channels
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Budget alerts channel
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET_ALERTS,
                "Budget Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about your budget status"
                enableVibration(true)
                enableLights(true)
            }

            // Daily reminders channel
            val reminderChannel = NotificationChannel(
                CHANNEL_DAILY_REMINDERS,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders to record expenses"
            }

            notificationManager.createNotificationChannel(budgetChannel)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun showBudgetWarningNotification(percentSpent: Int) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Budget Warning")
            .setContentText("You've used $percentSpent% of your monthly budget")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You've used $percentSpent% of your monthly budget. Consider reducing spending for the rest of the month."))
            .setSubText(USER_LOGIN)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BUDGET_WARNING, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}")
        }
    }

    fun showBudgetExceededNotification(amountExceeded: Double, currencySymbol: String) {
        val intent = Intent(context, BudgetActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val exceededAmount = CurrencyFormatter.formatAmount(amountExceeded, currencySymbol)

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Budget Exceeded!")
            .setContentText("You've exceeded your monthly budget by $exceededAmount")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("You've exceeded your monthly budget by $exceededAmount. It's time to review your spending."))
            .setSubText(USER_LOGIN)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BUDGET_EXCEEDED, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}")
        }
    }

    fun showDailyReminderNotification() {
        val intent = Intent(context, AddTransactionActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Format current date
        val dateFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
        val formattedDate = dateFormat.format(SPECIFIC_DATE.time)

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_REMINDERS)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Track Today's Expenses")
            .setContentText("Don't forget to record your expenses for $formattedDate")
            .setSubText(USER_LOGIN)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DAILY_REMINDER, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing notification: ${e.message}")
        }
    }
}