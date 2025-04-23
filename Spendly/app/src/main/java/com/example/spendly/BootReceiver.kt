package com.example.spendly

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device boot completed - restoring notification schedules")

            try {
                val prefsManager = PrefsManager(context)

                if (prefsManager.shouldShowDailyReminders()) {
                    val reminderIntent = Intent(context, BudgetCheckService::class.java).apply {
                        action = BudgetCheckService.ACTION_SCHEDULE_DAILY_REMINDER
                    }
                    startServiceSafely(context, reminderIntent)
                    Log.d(TAG, "Daily reminder scheduled after boot")
                }

                Log.d(TAG, "Boot initialization complete for user: ${NotificationHelper.USER_LOGIN}")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring notifications after boot: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun startServiceSafely(context: Context, intent: Intent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Starting foreground service for action: ${intent.action}")
                context.startForegroundService(intent)
            } else {
                Log.d(TAG, "Starting normal service for action: ${intent.action}")
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service: ${e.message}", e)
        }
    }
}