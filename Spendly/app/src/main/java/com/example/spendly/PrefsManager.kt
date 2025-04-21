package com.example.spendly

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "spendly_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_USER_LOGGED_IN = "user_logged_in"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchComplete() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun isUserLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_USER_LOGGED_IN, false)
    }

    fun saveUserLoggedIn(isLoggedIn: Boolean) {
        prefs.edit().putBoolean(KEY_USER_LOGGED_IN, isLoggedIn).apply()
    }
}