package com.example.spendly

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Background service that checks budget status and sends notifications if needed
 */
class BudgetCheckService : Service() {

    private lateinit var prefsManager: PrefsManager
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        prefsManager = PrefsManager(this)
        transactionRepository = TransactionRepository(this)
        notificationHelper = NotificationHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Run the budget check and then stop the service
        checkBudgetStatus()
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkBudgetStatus() {
        if (!prefsManager.shouldNotifyBudgetWarning()) {
            return
        }

        val monthlyBudget = prefsManager.getMonthlyBudget()
        if (monthlyBudget <= 0) {
            return
        }

        val totalExpense = transactionRepository.getTotalExpenseForCurrentMonth()
        val percentSpent = ((totalExpense / monthlyBudget) * 100).toInt().coerceIn(0, 100)
        val currencySymbol = prefsManager.getCurrencySymbol()

        when {
            percentSpent >= 100 && totalExpense > monthlyBudget -> {
                val exceededAmount = totalExpense - monthlyBudget
                notificationHelper.showBudgetExceededNotification(exceededAmount, currencySymbol)
            }
            percentSpent >= 80 && percentSpent < 100 -> {
                notificationHelper.showBudgetWarningNotification(percentSpent)
            }
        }
    }
}