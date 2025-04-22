package com.example.spendly

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isFirstLaunch(): Boolean {
        return sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchComplete() {
        sharedPreferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun getCurrencySymbol(): String {
        return sharedPreferences.getString(KEY_CURRENCY_SYMBOL, "$") ?: "$"
    }

    fun setCurrencySymbol(symbol: String) {
        sharedPreferences.edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
    }

    fun getMonthlyBudget(): Double {
        return sharedPreferences.getFloat(KEY_MONTHLY_BUDGET, 0f).toDouble()
    }

    fun setMonthlyBudget(budget: Double) {
        sharedPreferences.edit().putFloat(KEY_MONTHLY_BUDGET, budget.toFloat()).apply()
    }

    fun shouldNotifyBudgetWarning(): Boolean {
        return sharedPreferences.getBoolean(KEY_NOTIFY_BUDGET_WARNING, true)
    }

    fun setShouldNotifyBudgetWarning(shouldNotify: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_NOTIFY_BUDGET_WARNING, shouldNotify).apply()
    }


    // Add these methods to your existing PrefsManager class

    // Budget Management


    fun getCategoryBudget(category: String): Double {
        return sharedPreferences.getFloat("${KEY_CATEGORY_BUDGET_PREFIX}$category", 0f).toDouble()
    }

    fun setCategoryBudget(category: String, budget: Double) {
        sharedPreferences.edit().putFloat("${KEY_CATEGORY_BUDGET_PREFIX}$category", budget.toFloat()).apply()
    }


    fun shouldShowDailyReminders(): Boolean {
        return sharedPreferences.getBoolean(KEY_SHOW_DAILY_REMINDERS, false)
    }

    fun setShouldShowDailyReminders(shouldShow: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SHOW_DAILY_REMINDERS, shouldShow).apply()
    }

    fun getReminderTime(): String {
        return sharedPreferences.getString(KEY_REMINDER_TIME, "20:00") ?: "20:00"
    }

    fun setReminderTime(time: String) {
        sharedPreferences.edit().putString(KEY_REMINDER_TIME, time).apply()
    }



    companion object {
        private const val PREFS_NAME = "spendly_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"

        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_CATEGORY_BUDGET_PREFIX = "category_budget_"
        private const val KEY_NOTIFY_BUDGET_WARNING = "notify_budget_warning"
        private const val KEY_SHOW_DAILY_REMINDERS = "show_daily_reminders"
        private const val KEY_REMINDER_TIME = "reminder_time"

    }
}