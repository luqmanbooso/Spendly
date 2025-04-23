package com.example.spendly

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager

class PrefsManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Also access default preferences where settings are stored
    private val settingsPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val context = context

    init {
        // Migrate preferences if needed
        migratePreferences()
    }

    private fun migratePreferences() {
        try {
            // Migrate old keys to new keys if needed
            if (sharedPreferences.contains(KEY_NOTIFY_BUDGET_WARNING) &&
                !settingsPreferences.contains("budget_alerts")) {

                val value = sharedPreferences.getBoolean(KEY_NOTIFY_BUDGET_WARNING, true)
                settingsPreferences.edit()
                    .putBoolean("budget_alerts", value)
                    .apply()
            }

            if (sharedPreferences.contains(KEY_SHOW_DAILY_REMINDERS) &&
                !settingsPreferences.contains("daily_reminder")) {

                val value = sharedPreferences.getBoolean(KEY_SHOW_DAILY_REMINDERS, false)
                settingsPreferences.edit()
                    .putBoolean("daily_reminder", value)
                    .apply()
            }

            // Convert any string values to proper booleans in settings preferences
            fixStringBooleanIssues()

            // Make sure user data is synced properly
            syncUserProfile()
        } catch (e: Exception) {
            Log.e("PrefsManager", "Error during preference migration", e)
        }
    }

    private fun fixStringBooleanIssues() {
        try {
            val editor = settingsPreferences.edit()
            val allPrefs = settingsPreferences.all

            // Fix known boolean preferences stored as strings
            val booleanKeys = listOf("budget_alerts", "daily_reminder")

            for (key in booleanKeys) {
                if (allPrefs.containsKey(key) && allPrefs[key] is String) {
                    val stringValue = allPrefs[key].toString()
                    val boolValue = stringValue.equals("true", ignoreCase = true)

                    // Remove and recreate as proper boolean
                    editor.remove(key)
                    editor.putBoolean(key, boolValue)

                    Log.d("PrefsManager", "Fixed type for $key: $stringValue → $boolValue")
                }
            }

            editor.apply()
        } catch (e: Exception) {
            Log.e("PrefsManager", "Error fixing string/boolean issues", e)
        }
    }

    private fun syncUserProfile() {
        try {
            // Get user profile data from shared preferences
            val userPrefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE)
            val name = userPrefs.getString("name", null)
            val email = userPrefs.getString("email", null)

            // Log user data status
            Log.d("PrefsManager", "User profile - Name: ${name ?: "not set"}, Email: ${email ?: "not set"}")
        } catch (e: Exception) {
            Log.e("PrefsManager", "Error syncing user profile", e)
        }
    }

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

    // Updated to use both locations for compatibility
    fun shouldNotifyBudgetWarning(): Boolean {
        return try {
            // Try settings preferences first (where the switch preference saves)
            if (settingsPreferences.contains("budget_alerts")) {
                settingsPreferences.getBoolean("budget_alerts", true)
            } else {
                // Fall back to app preferences
                sharedPreferences.getBoolean(KEY_NOTIFY_BUDGET_WARNING, true)
            }
        } catch (e: ClassCastException) {
            // Handle case where preference might be stored as string
            val value = settingsPreferences.getString("budget_alerts", "true")
            val boolValue = value?.equals("true", ignoreCase = true) ?: true

            // Fix the preference type
            settingsPreferences.edit().remove("budget_alerts").apply()
            settingsPreferences.edit().putBoolean("budget_alerts", boolValue).apply()

            // Also update app preferences for consistency
            sharedPreferences.edit().putBoolean(KEY_NOTIFY_BUDGET_WARNING, boolValue).apply()

            boolValue
        }
    }


    fun setShouldNotifyBudgetWarning(shouldNotify: Boolean) {
        // Update both locations
        settingsPreferences.edit().putBoolean("budget_alerts", shouldNotify).apply()
        sharedPreferences.edit().putBoolean(KEY_NOTIFY_BUDGET_WARNING, shouldNotify).apply()
    }

    fun getCategoryBudget(category: String): Double {
        return sharedPreferences.getFloat("${KEY_CATEGORY_BUDGET_PREFIX}$category", 0f).toDouble()
    }

    fun setCategoryBudget(category: String, budget: Double) {
        sharedPreferences.edit().putFloat("${KEY_CATEGORY_BUDGET_PREFIX}$category", budget.toFloat()).apply()
    }

    // Updated to use both locations for compatibility
    fun shouldShowDailyReminders(): Boolean {
        return try {
            // Try settings preferences first
            if (settingsPreferences.contains("daily_reminder")) {
                settingsPreferences.getBoolean("daily_reminder", false)
            } else {
                sharedPreferences.getBoolean(KEY_SHOW_DAILY_REMINDERS, false)
            }
        } catch (e: ClassCastException) {
            // Handle case where preference might be stored as string
            val value = settingsPreferences.getString("daily_reminder", "false")
            val boolValue = value?.equals("true", ignoreCase = true) ?: false

            // Fix the preference type
            settingsPreferences.edit().remove("daily_reminder").apply()
            settingsPreferences.edit().putBoolean("daily_reminder", boolValue).apply()

            // Also update app preferences
            sharedPreferences.edit().putBoolean(KEY_SHOW_DAILY_REMINDERS, boolValue).apply()

            boolValue
        }
    }

    fun setShouldShowDailyReminders(shouldShow: Boolean) {
        // Update both locations
        settingsPreferences.edit().putBoolean("daily_reminder", shouldShow).apply()
        sharedPreferences.edit().putBoolean(KEY_SHOW_DAILY_REMINDERS, shouldShow).apply()
    }

    fun getReminderTime(): String {
        return sharedPreferences.getString(KEY_REMINDER_TIME, "20:00") ?: "20:00"
    }

    fun setReminderTime(time: String) {
        sharedPreferences.edit().putString(KEY_REMINDER_TIME, time).apply()
    }

    fun getReminderTimeHour(): Int {
        return sharedPreferences.getInt(KEY_REMINDER_HOUR, 20) // Default to 8:00 PM
    }

    fun getReminderTimeMinute(): Int {
        return sharedPreferences.getInt(KEY_REMINDER_MINUTE, 0)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        sharedPreferences.edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
    }

    fun getBudgetWarningThreshold(): Int {
        return try {
            val thresholdStr = settingsPreferences.getString("budget_warning_percent", "80")
            thresholdStr?.toIntOrNull() ?: 80
        } catch (e: Exception) {
            80 // Default if there's any issue
        }
    }

    fun setBudgetWarningThreshold(percent: Int) {
        val validPercent = percent.coerceIn(50, 90)
        settingsPreferences.edit().putString("budget_warning_percent", validPercent.toString()).apply()
    }

    // Make sure these methods exist in your PrefsManager class

    fun setShowDailyReminders(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("daily_reminder", enabled).apply()
    }


    companion object {
        private const val PREFS_NAME = "spendly_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_CURRENCY_SYMBOL = "currency_symbol"

        private const val KEY_MONTHLY_BUDGET = "monthly_budget"
        private const val KEY_CATEGORY_BUDGET_PREFIX = "category_budget_"
        private const val KEY_NOTIFY_BUDGET_WARNING = "notify_budget_warning" // Old key
        private const val KEY_SHOW_DAILY_REMINDERS = "show_daily_reminders" // Old key
        private const val KEY_REMINDER_TIME = "reminder_time"

        private const val KEY_CURRENCY = "currency"
        private const val KEY_BUDGET_WARNING_THRESHOLD = "budget_warning_threshold"
        private const val KEY_DAILY_REMINDERS = "daily_reminders" // Old key
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
    }
}