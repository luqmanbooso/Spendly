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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefsManager: PrefsManager
    private lateinit var backupHelper: BackupHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fix any corrupted preferences before setting content
        migratePreferences()

        setContentView(R.layout.activity_settings)

        prefsManager = PrefsManager(this)
        backupHelper = BackupHelper(this)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }

        toolbar.setNavigationOnClickListener {
            finishWithAnimation()
        }

        // Register preference change listener AFTER fixing preferences
        val preferenceListener = SafePreferenceChangeListener()
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    private fun migratePreferences() {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = prefs.edit()

            // Fix boolean preferences stored as strings
            val booleanKeys = listOf("budget_alerts", "daily_reminder")

            for (key in booleanKeys) {
                try {
                    if (prefs.contains(key)) {
                        val entry = prefs.all[key]
                        if (entry is String) {
                            // It's stored as a string, convert it
                            val boolValue = entry.equals("true", ignoreCase = true)
                            editor.remove(key)
                            editor.putBoolean(key, boolValue)
                            Log.d("SettingsActivity", "Fixed preference $key: $entry → $boolValue")
                        }
                    }
                } catch (e: Exception) {
                    // If there's any issue, remove the problematic preference
                    Log.e("SettingsActivity", "Error migrating preference: $key", e)
                    editor.remove(key)
                }
            }

            editor.apply()
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Migration failed", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Unregister all preference listeners to prevent leaks
            PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(SafePreferenceChangeListener())
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error unregistering listener", e)
        }
    }

    private fun finishWithAnimation() {
        val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
        findViewById<View>(R.id.settings_container).startAnimation(slideDown)

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

    /**
     * Safe preference listener that catches exceptions
     */
    private inner class SafePreferenceChangeListener : SharedPreferences.OnSharedPreferenceChangeListener {
        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
            try {
                when (key) {
                    "budget_alerts" -> {
                        try {
                            val isEnabled = sharedPreferences.getBoolean(key, true)
                            prefsManager.setShouldNotifyBudgetWarning(isEnabled)
                        } catch (e: ClassCastException) {
                            // Fix the type mismatch
                            val strValue = sharedPreferences.getString(key, "true")
                            val boolValue = strValue?.equals("true", ignoreCase = true) ?: true
                            prefsManager.setShouldNotifyBudgetWarning(boolValue)
                            recreateSettingsFragment()
                        }
                    }
                    "budget_warning_percent" -> {
                        try {
                            val strValue = sharedPreferences.getString(key, "80")
                            val value = strValue?.toIntOrNull() ?: 80
                            if (value in 50..90) {
                                prefsManager.setBudgetWarningThreshold(value)
                                Log.d("SettingsActivity", "Updated budget warning threshold: $value%")
                            } else {
                                // Reset to default if invalid
                                sharedPreferences.edit().putString(key, "80").apply()
                                Toast.makeText(this@SettingsActivity, 
                                    "Warning threshold must be between 50% and 90%", 
                                    Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.e("SettingsActivity", "Error updating budget threshold: ${e.message}")
                        }
                    }
                    "default_currency" -> {
                        val currency = sharedPreferences.getString(key, "$") ?: "$"
                        prefsManager.setCurrencySymbol(currency)
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Error in preference change listener: ${e.message}", e)
            }
        }
    }

    private fun recreateSettingsFragment() {
        try {
            if (!isFinishing && !isDestroyed) {
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.settings_container, SettingsFragment())
                    .commit()
            }
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error recreating settings fragment", e)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var prefsManager: PrefsManager
        private lateinit var backupHelper: BackupHelper
        private lateinit var transactionRepository: TransactionRepository



        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            try {
                // Safely load preferences
                setPreferencesFromResource(R.xml.preferences, rootKey)

                prefsManager = PrefsManager(requireContext())
                backupHelper = BackupHelper(requireContext())
                transactionRepository = TransactionRepository(requireContext())

                // Fix any string-to-boolean preferences before setting up values
                fixBooleanPreferences()

                // Safely initialize preferences with current values from PrefsManager
                setupPreferenceValues()
                setupPreferenceClickListeners()

            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error in onCreatePreferences", e)
            }
        }


        /**
         * Helper method to share a file
         */
        private fun shareFile(file: File, mimeType: String, title: String) {
            try {
                val fileUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    file
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(shareIntent, title))
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error sharing file: ${e.message}", e)
                Snackbar.make(requireView(), "Error sharing file: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }

        private fun fixBooleanPreferences() {
            try {
                val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
                val editor = prefs.edit()
                val booleanKeys = listOf("budget_alerts", "daily_reminder")

                for (key in booleanKeys) {
                    if (prefs.contains(key)) {
                        val value = prefs.all[key]
                        if (value is String) {
                            val boolValue = value.equals("true", ignoreCase = true)
                            editor.putBoolean(key, boolValue)
                            Log.d("SettingsFragment", "Fixed boolean preference $key: $value → $boolValue")
                        }
                    }
                }
                editor.apply()
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error fixing boolean preferences", e)
            }
        }

        private fun setupPreferenceValues() {
            try {
                // Budget alerts
                val budgetAlerts = findPreference<SwitchPreferenceCompat>("budget_alerts")
                budgetAlerts?.isChecked = prefsManager.shouldNotifyBudgetWarning()

                // Budget warning threshold
                val threshold = findPreference<EditTextPreference>("budget_warning_percent")
                threshold?.text = prefsManager.getBudgetWarningThreshold().toString()

                // Currency preference
                val currencyPref = findPreference<ListPreference>("default_currency")
                if (currencyPref != null) {
                    val currentSymbol = prefsManager.getCurrencySymbol()
                    currencyPref.value = currentSymbol
                }

                // Profile information
                val profilePref = findPreference<Preference>("profile")
                val userPrefs = requireContext().getSharedPreferences("user_profile", 0)
                val userName = userPrefs.getString("name", "")
                val userEmail = userPrefs.getString("email", "")

                if (!userName.isNullOrEmpty() && !userEmail.isNullOrEmpty()) {
                    profilePref?.summary = "$userName ($userEmail)"
                }
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error setting up preference values", e)
            }
        }

        private fun setupPreferenceClickListeners() {
            try {
                // Setup profile preference click listener
                findPreference<Preference>("profile")?.setOnPreferenceClickListener {
                    val intent = Intent(requireContext(), ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }

                // Add this immediately after your existing preference handlers in setupPreferenceClickListeners()
// Handle daily reminder toggle
                findPreference<SwitchPreferenceCompat>("daily_reminder")?.setOnPreferenceChangeListener { _, newValue ->
                    try {
                        val isEnabled = newValue as Boolean
                        prefsManager.setShowDailyReminders(isEnabled)

                        // Schedule or cancel the reminder based on the new setting
                        val serviceIntent = Intent(requireContext(), BudgetCheckService::class.java).apply {
                            action = if (isEnabled) {
                                BudgetCheckService.ACTION_SCHEDULE_DAILY_REMINDER
                            } else {
                                BudgetCheckService.ACTION_CANCEL_DAILY_REMINDER
                            }
                        }
                        requireContext().startService(serviceIntent)

                        // Make reminder time preference visible only when reminders are enabled
                        findPreference<Preference>("reminder_time")?.isVisible = isEnabled

                        // Show confirmation
                        if (isEnabled) {
                            val time = prefsManager.getReminderTime()
                            Snackbar.make(requireView(),
                                "Daily reminders enabled at $time",
                                Snackbar.LENGTH_SHORT).show()
                        }

                        true
                    } catch (e: Exception) {
                        Log.e("SettingsFragment", "Error toggling daily reminder: ${e.message}")
                        false
                    }
                }

// Initialize reminder time visibility based on current preference
                findPreference<Preference>("reminder_time")?.isVisible =
                    prefsManager.shouldShowDailyReminders()

// Update reminder time click handler to reschedule reminders
                findPreference<Preference>("reminder_time")?.setOnPreferenceClickListener {
                    val calendar = Calendar.getInstance()
                    TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->
                            val time = String.format("%02d:%02d", hourOfDay, minute)

                            // Update preference and summary
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                                .edit()
                                .putString("reminder_time", time)
                                .apply()

                            // Update PrefsManager
                            prefsManager.setReminderTime(time)

                            // Update summary
                            it.summary = "Daily reminder at $time"

                            // Reschedule the reminder with the new time
                            if (prefsManager.shouldShowDailyReminders()) {
                                val intent = Intent(requireContext(), BudgetCheckService::class.java).apply {
                                    action = BudgetCheckService.ACTION_SCHEDULE_DAILY_REMINDER
                                }
                                requireContext().startService(intent)

                                Snackbar.make(requireView(),
                                    "Daily reminder scheduled for $time",
                                    Snackbar.LENGTH_SHORT).show()
                            }
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                    true
                }

                // Setup backup data preference with confirmation dialog
                findPreference<Preference>("backup_data")?.setOnPreferenceClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Create Backup")
                        .setMessage("This will save all your transactions and settings. Continue?")
                        .setPositiveButton("Backup") { _, _ ->
                            try {
                                // Create backup first
                                if (backupHelper.backupUserData()) {
                                    // Get the most recent backup file
                                    val backupFiles = backupHelper.getBackupFiles()
                                    if (backupFiles.isNotEmpty()) {
                                        val latestBackup = backupFiles.first()
                                        
                                        // Create an intent to save the file
                                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_TITLE, "spendly_backup_${SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault()).format(Date())}.json")
                                        }

                                        // Start the file picker
                                        startActivityForResult(
                                            Intent.createChooser(intent, "Save Backup As"),
                                            REQUEST_CODE_SAVE_BACKUP
                                        )
                                    }
                                } else {
                                    Snackbar.make(requireView(), "Failed to create backup", Snackbar.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Log.e("SettingsFragment", "Error creating backup", e)
                                Snackbar.make(requireView(), "Error creating backup: ${e.message}", Snackbar.LENGTH_LONG).show()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }

                // Add JSON export functionality
                findPreference<Preference>("export_json")?.setOnPreferenceClickListener {
                    try {
                        val jsonData = backupHelper.exportDataAsJson()

                        // Create a file with timestamp
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
                        val fileName = "spendly_export_${dateFormat.format(Date())}.json"
                        val file = File(requireContext().cacheDir, fileName)
                        file.writeText(jsonData)

                        // Share the file
                        shareFile(file, "application/json", "Export as JSON")
                    } catch (e: Exception) {
                        Log.e("SettingsFragment", "Error exporting as JSON: ${e.message}", e)
                        Snackbar.make(requireView(), "Failed to export: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
                    true
                }


// Add Text export functionality
                findPreference<Preference>("export_text")?.setOnPreferenceClickListener {
                    try {
                        val textData = backupHelper.exportDataAsText()

                        // Create a file with timestamp
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
                        val fileName = "spendly_export_${dateFormat.format(Date())}.txt"
                        val file = File(requireContext().cacheDir, fileName)
                        file.writeText(textData)

                        // Share the file
                        shareFile(file, "text/plain", "Export as Text")
                    } catch (e: Exception) {
                        Log.e("SettingsFragment", "Error exporting as text: ${e.message}", e)
                        Snackbar.make(requireView(), "Failed to export: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }
                    true
                }
                // Setup restore data preference with confirmation
                findPreference<Preference>("restore_data")?.setOnPreferenceClickListener {
                    // Create an intent to pick a JSON file
                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "application/json"
                        addCategory(Intent.CATEGORY_OPENABLE)
                    }

                    // Start the file picker
                    startActivityForResult(
                        Intent.createChooser(intent, "Select Backup File"),
                        REQUEST_CODE_PICK_BACKUP
                    )
                    true
                }

                // Export transactions preference
                findPreference<Preference>("export_transactions")?.setOnPreferenceClickListener {
                    // Get all transactions
                    val transactions = transactionRepository.getAllTransactions()

                    if (transactions.isEmpty()) {
                        Snackbar.make(requireView(), "No transactions to export", Snackbar.LENGTH_SHORT).show()
                        return@setOnPreferenceClickListener true
                    }

                    try {
                        val file = backupHelper.exportTransactionsAsCSV(transactions)

                        // Share the CSV file
                        val fileUri = androidx.core.content.FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.provider",
                            file
                        )

                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            type = "text/csv"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        startActivity(Intent.createChooser(shareIntent, "Share Transactions"))

                    } catch (e: Exception) {
                        Log.e("Settings", "Export error", e)
                        Snackbar.make(requireView(), "Export failed: ${e.message}", Snackbar.LENGTH_SHORT).show()
                    }

                    true
                }

                // Handle logout preference click
                findPreference<Preference>("logout")?.setOnPreferenceClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Confirm Logout")
                        .setMessage("Are you sure you want to sign out? You will need to sign in again to access your data.")
                        .setPositiveButton("Logout") { _, _ ->
                            try {
                                // Clear all shared preferences
                                val prefs = requireContext().getSharedPreferences("com.example.spendly_preferences", Context.MODE_PRIVATE)
                                prefs.edit().clear().apply()

                                // Clear user profile
                                val userPrefs = requireContext().getSharedPreferences("user_profile", Context.MODE_PRIVATE)
                                userPrefs.edit().clear().apply()

                                // Clear transactions
                                val transactionPrefs = requireContext().getSharedPreferences("transactions", Context.MODE_PRIVATE)
                                transactionPrefs.edit().clear().apply()

                                // Clear budgets
                                val budgetPrefs = requireContext().getSharedPreferences("budgets", Context.MODE_PRIVATE)
                                budgetPrefs.edit().clear().apply()

                                // Delete internal storage files
                                val filesDir = requireContext().filesDir
                                File(filesDir, "transactions.json").delete()
                                File(filesDir, "categories.json").delete()

                                Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

                                // Navigate to login screen
                                val intent = Intent(requireContext(), LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                requireActivity().finish()
                            } catch (e: Exception) {
                                Log.e("SettingsFragment", "Error during logout: ${e.message}", e)
                                // If there's an error, still try to go to the login screen
                                val intent = Intent(requireContext(), LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                requireActivity().finish()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }

                // Handle reminder time preference click
                findPreference<Preference>("reminder_time")?.setOnPreferenceClickListener {
                    val calendar = Calendar.getInstance()
                    TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->
                            val time = String.format("%02d:%02d", hourOfDay, minute)
                            PreferenceManager.getDefaultSharedPreferences(requireContext())
                                .edit()
                                .putString("reminder_time", time)
                                .apply()
                            it.summary = "Daily reminder at $time"
                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                    true
                }

                // Handle budget warning threshold validation
                findPreference<Preference>("budget_warning_percent")?.setOnPreferenceChangeListener { _, newValue ->
                    try {
                        val value = (newValue as String).toInt()
                        if (value in 50..90) {
                            true
                        } else {
                            Toast.makeText(requireContext(), "Please enter a value between 50 and 90", Toast.LENGTH_SHORT).show()
                            false
                        }
                    } catch (e: NumberFormatException) {
                        Toast.makeText(requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show()
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsFragment", "Error setting up click listeners", e)
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            
            when (requestCode) {
                REQUEST_CODE_PICK_BACKUP -> {
                    if (resultCode == RESULT_OK) {
                        data?.data?.let { uri ->
                            try {
                                // Create a temporary file to copy the selected backup to
                                val tempFile = File(requireContext().cacheDir, "temp_backup.json")
                                
                                // Copy the selected file to our temporary location
                                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }

                                // Show confirmation dialog
                                MaterialAlertDialogBuilder(requireContext())
                                    .setTitle("Confirm Restore")
                                    .setMessage("This will replace your current transactions and budgets with the backup. Are you sure?")
                                    .setPositiveButton("Restore") { _, _ ->
                                        try {
                                            // Read the file contents
                                            val backupData = tempFile.readText()
                                            if (backupHelper.restoreUserData(backupData)) {
                                                Snackbar.make(requireView(), "Data restored successfully", Snackbar.LENGTH_LONG)
                                                    .setAction("Restart") {
                                                        // Force reload settings
                                                        requireActivity().recreate()
                                                    }
                                                    .show()
                                            } else {
                                                Snackbar.make(requireView(), "Failed to restore data", Snackbar.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Log.e("SettingsFragment", "Error restoring data", e)
                                            Snackbar.make(requireView(), "Error restoring data: ${e.message}", Snackbar.LENGTH_LONG).show()
                                        } finally {
                                            // Clean up temporary file
                                            tempFile.delete()
                                        }
                                    }
                                    .setNegativeButton("Cancel", null)
                                    .show()
                            } catch (e: Exception) {
                                Log.e("SettingsFragment", "Error handling backup file", e)
                                Snackbar.make(requireView(), "Error handling backup file: ${e.message}", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                REQUEST_CODE_SAVE_BACKUP -> {
                    if (resultCode == RESULT_OK) {
                        data?.data?.let { uri ->
                            try {
                                // Get the most recent backup file
                                val backupFiles = backupHelper.getBackupFiles()
                                if (backupFiles.isNotEmpty()) {
                                    val latestBackup = backupFiles.first()
                                    
                                    // Copy the backup file to the selected location
                                    requireContext().contentResolver.openOutputStream(uri)?.use { output ->
                                        File(latestBackup.absolutePath).inputStream().use { input ->
                                            input.copyTo(output)
                                        }
                                    }
                                    
                                    Snackbar.make(requireView(), "Backup saved successfully", Snackbar.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Log.e("SettingsFragment", "Error saving backup", e)
                                Snackbar.make(requireView(), "Error saving backup: ${e.message}", Snackbar.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }

        companion object {
            private const val REQUEST_CODE_PICK_BACKUP = 1001
            private const val REQUEST_CODE_SAVE_BACKUP = 1002
        }
    }
}