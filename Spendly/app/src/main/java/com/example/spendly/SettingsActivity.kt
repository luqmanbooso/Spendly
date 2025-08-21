package com.example.spendly

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager
    private lateinit var backupHelper: BackupHelper
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefsManager = PrefsManager(this)
        backupHelper = BackupHelper(this)
        sharedPreferences = getSharedPreferences("com.example.spendly_preferences", Context.MODE_PRIVATE)

        setupToolbar()
        setupClickListeners()
        loadCurrentSettings()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            finishWithAnimation()
        }
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.profile_settings).setOnClickListener {
            startActivityForResult(Intent(this, ProfileActivity::class.java), REQUEST_CODE_PROFILE)
        }

        findViewById<View>(R.id.currency_settings).setOnClickListener {
            showCurrencySelectionDialog()
        }

        findViewById<SwitchMaterial>(R.id.budget_alerts_switch).setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setShouldNotifyBudgetWarning(isChecked)
            sharedPreferences.edit().putBoolean("budget_alerts", isChecked).apply()
        }

        findViewById<SwitchMaterial>(R.id.daily_reminder_switch).setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setShowDailyReminders(isChecked)
            sharedPreferences.edit().putBoolean("daily_reminder", isChecked).apply()

            val serviceIntent = Intent(this, BudgetCheckService::class.java).apply {
                action = if (isChecked) {
                    BudgetCheckService.ACTION_SCHEDULE_DAILY_REMINDER
                } else {
                    BudgetCheckService.ACTION_CANCEL_DAILY_REMINDER
                }
            }
            startService(serviceIntent)

            if (isChecked) {
                val time = prefsManager.getReminderTime()
                Snackbar.make(findViewById(android.R.id.content),
                    "Daily reminders enabled at $time",
                    Snackbar.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.backup_data).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Create Backup")
                .setMessage("This will save all your transactions and settings. Continue?")
                .setPositiveButton("Backup") { _, _ ->
                try {
                        if (backupHelper.backupUserData()) {
                            val backupFiles = backupHelper.getBackupFiles()
                            if (backupFiles.isNotEmpty()) {
                                val latestBackup = backupFiles.first()
                                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "application/json"
                                    putExtra(Intent.EXTRA_TITLE, "spendly_backup_${SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())}.json")
                                }
                                startActivityForResult(
                                    Intent.createChooser(intent, "Save Backup As"),
                                    REQUEST_CODE_SAVE_BACKUP
                                )
                            }
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), "Failed to create backup", Snackbar.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Log.e("SettingsActivity", "Error creating backup", e)
                        Snackbar.make(findViewById(android.R.id.content), "Error creating backup: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<View>(R.id.restore_data).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/json"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(
                Intent.createChooser(intent, "Select Backup File"),
                REQUEST_CODE_PICK_BACKUP
            )
        }

        findViewById<View>(R.id.logout).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to sign out? You will need to sign in again to access your data.")
                .setPositiveButton("Logout") { _, _ ->
                    try {
                        sharedPreferences.edit().clear().apply()

                        getSharedPreferences("user_profile", Context.MODE_PRIVATE).edit().clear().apply()

                        getSharedPreferences("transactions", Context.MODE_PRIVATE).edit().clear().apply()

                        getSharedPreferences("budgets", Context.MODE_PRIVATE).edit().clear().apply()

                        val filesDir = filesDir
                        File(filesDir, "transactions.json").delete()
                        File(filesDir, "categories.json").delete()

                        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this, MainPage::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
        } catch (e: Exception) {
                        Log.e("SettingsActivity", "Error during logout: ${e.message}", e)
                        val intent = Intent(this, MainPage::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadCurrentSettings() {
        findViewById<SwitchMaterial>(R.id.budget_alerts_switch).isChecked =
            sharedPreferences.getBoolean("budget_alerts", true)

        findViewById<SwitchMaterial>(R.id.daily_reminder_switch).isChecked =
            sharedPreferences.getBoolean("daily_reminder", false)

        val userPrefs = getSharedPreferences("user_profile", 0)
        val userName = userPrefs.getString("name", "")
        val userEmail = userPrefs.getString("email", "")
        if (!userName.isNullOrEmpty() && !userEmail.isNullOrEmpty()) {
            findViewById<TextView>(R.id.profile_summary).text = "$userName ($userEmail)"
        }

        val currency = sharedPreferences.getString("default_currency", "$") ?: "$"
        findViewById<TextView>(R.id.currency_summary).text = "Current currency: $currency"
    }

    private fun finishWithAnimation() {
        val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
        findViewById<View>(android.R.id.content).startAnimation(slideDown)

        slideDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishWithAnimation()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        when (requestCode) {
            REQUEST_CODE_PROFILE -> {
                if (resultCode == RESULT_OK && data != null) {
                    val updatedName = data.getStringExtra("name")
                    val updatedEmail = data.getStringExtra("email")
                    
                    if (!updatedName.isNullOrEmpty() && !updatedEmail.isNullOrEmpty()) {
                        findViewById<TextView>(R.id.profile_summary).text = "$updatedName ($updatedEmail)"
                        
                        val userPrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
                        userPrefs.edit().apply {
                            putString("name", updatedName)
                            putString("email", updatedEmail)
                            apply()
                    }
                }
                }
            }
                REQUEST_CODE_PICK_BACKUP -> {
                    if (resultCode == RESULT_OK) {
                        data?.data?.let { uri ->
                            try {
                            val tempFile = File(cacheDir, "temp_backup.json")
                            contentResolver.openInputStream(uri)?.use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }

                            MaterialAlertDialogBuilder(this)
                                    .setTitle("Confirm Restore")
                                    .setMessage("This will replace your current transactions and budgets with the backup. Are you sure?")
                                    .setPositiveButton("Restore") { _, _ ->
                                        try {
                                            val backupData = tempFile.readText()
                                            if (backupHelper.restoreUserData(backupData)) {
                                            Snackbar.make(findViewById(android.R.id.content), "Data restored successfully", Snackbar.LENGTH_LONG)
                                                    .setAction("Restart") {
                                                    recreate()
                                                    }
                                                    .show()
                                            } else {
                                            Snackbar.make(findViewById(android.R.id.content), "Failed to restore data", Snackbar.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                        Log.e("SettingsActivity", "Error restoring data", e)
                                        Snackbar.make(findViewById(android.R.id.content), "Error restoring data: ${e.message}", Snackbar.LENGTH_LONG).show()
                                        } finally {
                                            tempFile.delete()
                                        }
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            } catch (e: Exception) {
                            Log.e("SettingsActivity", "Error handling backup file", e)
                            Snackbar.make(findViewById(android.R.id.content), "Error handling backup file: ${e.message}", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                REQUEST_CODE_SAVE_BACKUP -> {
                    if (resultCode == RESULT_OK) {
                        data?.data?.let { uri ->
                            try {
                                val backupFiles = backupHelper.getBackupFiles()
                                if (backupFiles.isNotEmpty()) {
                                    val latestBackup = backupFiles.first()
                                contentResolver.openOutputStream(uri)?.use { output ->
                                        File(latestBackup.absolutePath).inputStream().use { input ->
                                            input.copyTo(output)
                                    }
                                }
                                Snackbar.make(findViewById(android.R.id.content), "Backup saved successfully", Snackbar.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.e("SettingsActivity", "Error saving backup", e)
                            Snackbar.make(findViewById(android.R.id.content), "Error saving backup: ${e.message}", Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun showCurrencySelectionDialog() {
        val currencies = resources.getStringArray(R.array.currency_entries)
        val currencyValues = resources.getStringArray(R.array.currency_values)
        val currentCurrency = sharedPreferences.getString("default_currency", "USD") ?: "USD"
        val currentIndex = currencyValues.indexOf(currentCurrency)

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Currency")
            .setSingleChoiceItems(currencies, currentIndex) { dialog, which ->
                val selectedCurrency = currencyValues[which]
                val currency = Currency.getInstance(selectedCurrency)
                
                sharedPreferences.edit()
                    .putString("default_currency", selectedCurrency)
                    .putString("currency_symbol", currency.symbol)
                    .apply()
                
                prefsManager.setCurrencySymbol(currency.symbol)
                
                findViewById<TextView>(R.id.currency_summary).text = "Current currency: ${currency.symbol}"
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
        }

        companion object {
        private const val REQUEST_CODE_PROFILE = 1000
            private const val REQUEST_CODE_PICK_BACKUP = 1001
            private const val REQUEST_CODE_SAVE_BACKUP = 1002
    }
}