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
                val notificationHelper = NotificationHelper(context)

                if (prefsManager.shouldShowDailyReminders()) {
                    Log.d(TAG, "Daily reminders are enabled, scheduling after boot")

                    val reminderIntent = Intent(context, BudgetCheckService::class.java).apply {
                        action = BudgetCheckService.ACTION_SCHEDULE_DAILY_REMINDER
                    }
                    context.startService(reminderIntent)
                    Log.d(TAG, "Daily reminder scheduled after boot")
                }

                if (prefsManager.shouldNotifyBudgetWarning()) {
                    Log.d(TAG, "Budget alerts are enabled")
                    val budgetIntent = Intent(context, BudgetCheckService::class.java).apply {
                        action = BudgetCheckService.ACTION_CHECK_BUDGET
                    }
                    context.startService(budgetIntent)
                    Log.d(TAG, "Budget check scheduled after boot")
                }

                Log.d(TAG, "Boot initialization complete for user: ${notificationHelper.getCurrentUser()}")
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring notifications after boot: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}