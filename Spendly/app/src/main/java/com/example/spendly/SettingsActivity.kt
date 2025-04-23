package com.example.spendly

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File

class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var prefsManager: PrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefsManager = PrefsManager(this)

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

        // Register preference change listener
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister preference change listener
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            "budget_alerts" -> {
                val isEnabled = sharedPreferences.getBoolean(key, true)
                prefsManager.setShouldNotifyBudgetWarning(isEnabled)

                // Reflect change in service
                if (isEnabled) {
                    val intent = Intent(this, BudgetCheckService::class.java)
                    intent.action = BudgetCheckService.ACTION_CHECK_BUDGET
                    startService(intent)
                }
            }
            "daily_reminder" -> {
                val isEnabled = sharedPreferences.getBoolean(key, false)
                prefsManager.setShouldShowDailyReminders(isEnabled)

                // Update reminder service
                val intent = Intent(this, BudgetCheckService::class.java)
                intent.action = if (isEnabled) {
                    BudgetCheckService.ACTION_SCHEDULE_DAILY_REMINDER
                } else {
                    BudgetCheckService.ACTION_CANCEL_DAILY_REMINDER
                }
                startService(intent)
            }
            "budget_warning_percent" -> {
                val percent = try {
                    val value = sharedPreferences.getString(key, "80")?.toIntOrNull() ?: 80
                    value.coerceIn(50, 90)
                } catch (e: Exception) {
                    80
                }
                prefsManager.setBudgetWarningThreshold(percent)
            }
            "default_currency" -> {
                val currency = sharedPreferences.getString(key, "USD") ?: "USD"
                prefsManager.setCurrency(currency)
            }
        }
    }

    override fun onBackPressed() {
        finishWithAnimation()
    }

    private fun finishWithAnimation() {
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
    }

    class SettingsFragment : PreferenceFragmentCompat() {

        private lateinit var prefsManager: PrefsManager
        private lateinit var backupHelper: BackupHelper
        private lateinit var transactionRepository: TransactionRepository

        // Activity result launcher for file operations
        private val createDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    exportTransactionsToURI(uri)
                }
            }
        }

        private val openDocumentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    importBackupFromURI(uri)
                }
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            prefsManager = PrefsManager(requireContext())
            backupHelper = BackupHelper(requireContext())
            transactionRepository = TransactionRepository(requireContext())

            // Initialize preferences with current values from PrefsManager
            val budgetAlerts = findPreference<SwitchPreferenceCompat>("budget_alerts")
            budgetAlerts?.isChecked = prefsManager.shouldNotifyBudgetWarning()

            val dailyReminders = findPreference<SwitchPreferenceCompat>("daily_reminder")
            dailyReminders?.isChecked = prefsManager.shouldShowDailyReminders()

            // Setup profile preference click listener
            findPreference<Preference>("profile")?.setOnPreferenceClickListener {
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                startActivity(intent)
                true
            }

            // Setup backup data preference
            findPreference<Preference>("backup_data")?.setOnPreferenceClickListener {
                showBackupDialog()
                true
            }

            // Setup restore data preference
            findPreference<Preference>("restore_data")?.setOnPreferenceClickListener {
                showRestoreDialog()
                true
            }

            // Export transactions preference
            findPreference<Preference>("export_transactions")?.setOnPreferenceClickListener {
                showExportDialog()
                true
            }

            // Logout preference
            findPreference<Preference>("logout")?.setOnPreferenceClickListener {
                showLogoutConfirmationDialog()
                true
            }
        }

        private fun showBackupDialog() {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Backup Data")
                .setMessage("This will create a backup of all your transactions, budgets, and settings. Continue?")
                .setPositiveButton("Backup Now") { _, _ ->
                    backupHelper.backupUserData()
                    showBackupSuccessSnackbar()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        private fun showRestoreDialog() {
            val backupFiles = backupHelper.getBackupFiles()

            if (backupFiles.isEmpty()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("No Backups Found")
                    .setMessage("You don't have any backups to restore. Create a backup first.")
                    .setPositiveButton("OK", null)
                    .show()
                return
            }

            // Show file picker
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse("content://com.android.externalstorage.documents/document/primary:Spendly"))
            }

            try {
                openDocumentLauncher.launch(intent)
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Error launching file picker", e)
                backupHelper.restoreUserData()
                Snackbar.make(requireView(), "Data restored from latest backup", Snackbar.LENGTH_LONG).show()
            }
        }

        private fun showExportDialog() {
            val transactions = transactionRepository.getAllTransactions()

            if (transactions.isEmpty()) {
                Snackbar.make(requireView(), "No transactions to export", Snackbar.LENGTH_SHORT).show()
                return
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Export Transactions")
                .setMessage("Choose a format to export your transactions:")
                .setPositiveButton("CSV") { _, _ ->
                    // Create file with SAF
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "text/csv"
                        putExtra(Intent.EXTRA_TITLE, "spendly_transactions_export.csv")
                    }
                    createDocumentLauncher.launch(intent)
                }
                .setNeutralButton("Cancel", null)
                .show()
        }

        private fun exportTransactionsToURI(uri: Uri) {
            try {
                val transactions = transactionRepository.getAllTransactions()
                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    // Write CSV header
                    val header = "Date,Type,Category,Description,Amount\n"
                    outputStream.write(header.toByteArray())

                    // Write data
                    transactions.forEach { transaction ->
                        val line = "${transaction.date},${transaction.type},${transaction.category},\"${transaction.title}\",${transaction.amount}\n"
                        outputStream.write(line.toByteArray())
                    }
                }

                Snackbar.make(requireView(), "Transactions exported successfully", Snackbar.LENGTH_LONG)
                    .setAction("View") {
                        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = uri
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        startActivity(viewIntent)
                    }
                    .show()

            } catch (e: Exception) {
                Log.e("SettingsActivity", "Export error", e)
                Snackbar.make(requireView(), "Export failed: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }

        private fun importBackupFromURI(uri: Uri) {
            try {
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { it.readText() }
                    if (backupHelper.importFromJson(jsonString)) {
                        Snackbar.make(requireView(), "Backup restored successfully", Snackbar.LENGTH_LONG).show()
                    } else {
                        Snackbar.make(requireView(), "Failed to restore backup - invalid format", Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsActivity", "Import error", e)
                Snackbar.make(requireView(), "Import failed: ${e.localizedMessage}", Snackbar.LENGTH_LONG).show()
            }
        }

        private fun showBackupSuccessSnackbar() {
            Snackbar.make(requireView(), "Backup created successfully", Snackbar.LENGTH_LONG)
                .setAction("View Backups") {
                    // Show a dialog with backup files and their dates
                    val backupFiles = backupHelper.getBackupFiles()
                    if (backupFiles.isEmpty()) {
                        Snackbar.make(requireView(), "No backup files found", Snackbar.LENGTH_SHORT).show()
                        return@setAction
                    }

                    // Format file names and dates for display
                    val backupInfo = backupFiles.map { file ->
                        val date = file.lastModified()
                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                        "${file.name} (${dateFormat.format(java.util.Date(date))})"
                    }.toTypedArray()

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Available Backups")
                        .setItems(backupInfo, null)
                        .setPositiveButton("Close", null)
                        .show()
                }
                .show()
        }

        private fun showLogoutConfirmationDialog() {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Logout")
                .setMessage("Are you sure you want to sign out? You will need to sign in again to access your data.")
                .setPositiveButton("Logout") { _, _ ->
                    // Clear user data from SharedPreferences
                    val userPrefs = requireContext().getSharedPreferences("user_profile", 0)
                    userPrefs.edit().clear().apply()

                    // Go to login screen
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}