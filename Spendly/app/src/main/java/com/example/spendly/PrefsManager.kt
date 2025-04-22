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

    companion object {
        private const val PREFS_NAME = "spendly_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_NOTIFY_BUDGET_WARNING = "notify_budget_warning"
    }
}