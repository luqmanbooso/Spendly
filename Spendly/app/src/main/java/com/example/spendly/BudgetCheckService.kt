package com.example.spendly

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.util.*


class BudgetCheckService : Service() {

    private lateinit var prefsManager: PrefsManager
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationHelper: NotificationHelper

    private val calendarCache = ThreadLocal<Calendar>()

    override fun onCreate() {
        super.onCreate()
        prefsManager = PrefsManager(this)
        transactionRepository = TransactionRepository(this)
        notificationHelper = NotificationHelper(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createServiceNotificationChannel()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")

        val action = intent?.action
        val startedAsForeground = intent?.getBooleanExtra(EXTRA_START_AS_FOREGROUND, false) ?: false

        try {
            if (startedAsForeground) {
                Log.d(TAG, "Starting as foreground service")
                val notification = createForegroundNotification()
                startForeground(FOREGROUND_SERVICE_ID, notification)
            }

            when (action) {
                ACTION_CHECK_BUDGET -> checkBudget()
                ACTION_DAILY_REMINDER -> sendDailyReminder()
                ACTION_SCHEDULE_DAILY_REMINDER -> scheduleDailyReminder()
                ACTION_CANCEL_DAILY_REMINDER -> cancelDailyReminder()
                else -> Log.d(TAG, "Unknown action: $action")
            }

            if (startedAsForeground) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in service: ${e.message}", e)
        }

        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createServiceNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_FOREGROUND_SERVICE,
            "Budget Service Channel",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Background processes for budget monitoring"
            enableLights(false)
            setSound(null, null)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_FOREGROUND_SERVICE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Spendly")
            .setContentText("Managing your budget notifications")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun checkBudget() {
        if (!prefsManager.shouldNotifyBudgetWarning()) {
            Log.d(TAG, "Budget notifications are disabled for user: ${notificationHelper.getCurrentUser()}")
            return
        }

        val monthlyBudget = prefsManager.getMonthlyBudget()
        if (monthlyBudget <= 0) {
            Log.d(TAG, "No monthly budget set for user: ${notificationHelper.getCurrentUser()}")
            return
        }

        val totalExpense = transactionRepository.getTotalExpenseForCurrentMonth()
        val percentSpent = ((totalExpense / monthlyBudget) * 100).toInt()
        val warningThreshold = prefsManager.getBudgetWarningThreshold()

        if (percentSpent >= 100) {
            val exceededAmount = totalExpense - monthlyBudget
            notificationHelper.showBudgetExceededNotification(
                exceededAmount,
                prefsManager.getCurrencySymbol()
            )
            Log.d(TAG, "Budget exceeded by ${CurrencyFormatter.formatAmount(exceededAmount, prefsManager.getCurrencySymbol())} for user: ${notificationHelper.getCurrentUser()}")
        } else if (percentSpent >= warningThreshold) {
            notificationHelper.showBudgetWarningNotification(percentSpent)
            Log.d(TAG, "Budget warning at $percentSpent% for user: ${notificationHelper.getCurrentUser()}")
        }

        val categories = listOf("food", "transport", "bills", "entertainment",
            "shopping", "health", "education", "other")
        
        for (category in categories) {
            val categoryBudget = prefsManager.getCategoryBudget(category)
            if (categoryBudget > 0) {
                val categoryExpense = transactionRepository.getExpenseForCategory(category)
                val categoryPercentSpent = ((categoryExpense / categoryBudget) * 100).toInt()

                if (categoryPercentSpent >= 100) {
                    val exceededAmount = categoryExpense - categoryBudget
                    notificationHelper.showCategoryBudgetExceededNotification(
                        category,
                        exceededAmount,
                        prefsManager.getCurrencySymbol()
                    )
                    Log.d(TAG, "$category budget exceeded by ${CurrencyFormatter.formatAmount(exceededAmount, prefsManager.getCurrencySymbol())} for user: ${notificationHelper.getCurrentUser()}")
                } else if (categoryPercentSpent >= warningThreshold) {
                    notificationHelper.showCategoryBudgetWarningNotification(category, categoryPercentSpent)
                    Log.d(TAG, "$category budget warning at $categoryPercentSpent% for user: ${notificationHelper.getCurrentUser()}")
                }
            }
        }
    }

    private fun sendDailyReminder() {
        if (prefsManager.shouldShowDailyReminders()) {
            notificationHelper.showDailyReminderNotification()
            Log.d(TAG, "Daily reminder notification sent for user: ${notificationHelper.getCurrentUser()}")
        }
    }

    private fun cancelDailyReminder() {
        val intent = Intent(this, BudgetCheckService::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }

        val pendingIntent = PendingIntent.getService(
            this,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Daily reminder cancelled")
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleDailyReminder() {
        if (!prefsManager.shouldShowDailyReminders()) {
            Log.d(TAG, "Daily reminders are disabled for user: ${notificationHelper.getCurrentUser()}")
            return
        }

        val timeString = prefsManager.getReminderTime()
        Log.d(TAG, "Scheduling daily reminder with time: $timeString")

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, BudgetCheckService::class.java).apply {
            action = ACTION_DAILY_REMINDER
        }

        val pendingIntent = PendingIntent.getService(
            this,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)

        val parts = timeString.split(":")
        if (parts.size != 2) {
            Log.e(TAG, "Invalid reminder time format: $timeString")
            return
        }

        try {
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()

            val calendar = calendarCache.get() ?: Calendar.getInstance().also { calendarCache.set(it) }
            calendar.apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)

                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                Log.d(TAG, "Attempting to schedule exact alarm for ${calendar.time}")
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Daily reminder scheduled for ${calendar.time}")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling exact alarm: ${e.message}")
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Fallback daily reminder scheduled for ${calendar.time}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder: ${e.message}")

            fallbackSchedule(alarmManager, pendingIntent)
        }
    }

    private fun fallbackSchedule(alarmManager: AlarmManager, pendingIntent: PendingIntent) {
        val calendar = calendarCache.get() ?: Calendar.getInstance().also { calendarCache.set(it) }
        calendar.apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Log.d(TAG, "Emergency fallback daily reminder scheduled for ${calendar.time}")
    }
    companion object {
        private const val TAG = "BudgetCheckService"
        const val ACTION_CHECK_BUDGET = "com.example.spendly.ACTION_CHECK_BUDGET"
        const val ACTION_DAILY_REMINDER = "com.example.spendly.DAILY_REMINDER"
        const val ACTION_SCHEDULE_DAILY_REMINDER = "com.example.spendly.ACTION_SCHEDULE_DAILY_REMINDER"
        const val EXTRA_START_AS_FOREGROUND = "start_as_foreground"
        private const val REMINDER_REQUEST_CODE = 1001
        const val ACTION_CANCEL_DAILY_REMINDER = "com.example.spendly.CANCEL_DAILY_REMINDER"

        const val CHANNEL_FOREGROUND_SERVICE = "budget_service_channel"
        private const val FOREGROUND_SERVICE_ID = 9999
    }
}