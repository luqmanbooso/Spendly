package com.example.spendly

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

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
            onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Setup profile preference click listener
            findPreference<Preference>("profile")?.setOnPreferenceClickListener {
                val intent = Intent(requireContext(), ProfileActivity::class.java)
                startActivity(intent)
                true
            }

            // Setup backup data preference
            findPreference<Preference>("backup_data")?.setOnPreferenceClickListener {
                val backupHelper = BackupHelper(requireContext())
                backupHelper.backupUserData()
                true
            }

            // Setup restore data preference
            findPreference<Preference>("restore_data")?.setOnPreferenceClickListener {
                val backupHelper = BackupHelper(requireContext())
                backupHelper.restoreUserData()
                true
            }

            // Export transactions preference (if you have TransactionRepository available)
            findPreference<Preference>("export_transactions")?.setOnPreferenceClickListener {
                try {
                    val repository = TransactionRepository(requireContext())
                    val backupHelper = BackupHelper(requireContext())
                    backupHelper.backupAllTransactions(repository)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                true
            }

            // Logout preference
            findPreference<Preference>("logout")?.setOnPreferenceClickListener {
                // Clear user data from SharedPreferences
                val userPrefs = requireContext().getSharedPreferences("user_profile", 0)
                userPrefs.edit().clear().apply()

                // Go to login screen
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
                true
            }
        }
    }
}