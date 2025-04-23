package com.example.spendly

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendly.TransactionAdapter
import com.example.spendly.PrefsManager
import com.example.spendly.TransactionRepository
import com.example.spendly.databinding.ActivityMainBinding
import com.example.spendly.Transaction
import com.example.spendly.CurrencyFormatter
import com.example.spendly.AddTransactionActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_CODE = 123
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PrefsManager
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var bottomNavHelper: BottomNavHelper
    private lateinit var btnSettings: ImageView
    private lateinit var tvUserName : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // Initialize repositories and managers
        prefsManager = PrefsManager(this)
        transactionRepository = TransactionRepository(this)
        bottomNavHelper = BottomNavHelper(this, binding.root)
        btnSettings = findViewById(R.id.btnSettings)
        bottomNavHelper.setupBottomNav(NavSection.HOME)
        tvUserName = findViewById(R.id.tvUserName) // Added user name TextView

//        findViewById<Button>(R.id.btnTestNotification).setOnClickListener {
//            testNotificationDirectly()
//        }

        btnSettings.setOnClickListener {
            openSettings()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        // Set the current month in the header
        val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvCurrentMonth.text = monthFormatter.format(Date())

        // Set up UI
        setupDashboard()
        setupRecentTransactions()

        checkBudgetAndScheduleReminders()

    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        setupDashboard()
        setupRecentTransactions()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) { // Android 13 is API 33
            // FIXED: Use correct permission constant
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                "android.permission.POST_NOTIFICATIONS" // Use string directly to avoid compile errors
            ) == PackageManager.PERMISSION_GRANTED

            Log.d(TAG, "Notification permission already granted: $hasPermission")

            if (!hasPermission) {
                // Request the permission - use the string constant directly
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf("android.permission.POST_NOTIFICATIONS"),
                    NOTIFICATION_PERMISSION_CODE
                )
                Log.d(TAG, "Requesting notification permission...")
            }
        }
    }

    private fun openSettings() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun setupDashboard() {
        val currencySymbol = prefsManager.getCurrencySymbol()

        // Update income, expense and balance
        val totalIncome = transactionRepository.getTotalIncomeForCurrentMonth()
        val totalExpense = transactionRepository.getTotalExpenseForCurrentMonth()
        val balance = totalIncome - totalExpense

        updateUserName()

        binding.tvTotalIncome.text = CurrencyFormatter.formatAmount(totalIncome, currencySymbol)
        binding.tvTotalExpense.text = CurrencyFormatter.formatAmount(totalExpense, currencySymbol)
        binding.tvBalance.text = CurrencyFormatter.formatAmount(balance, currencySymbol)

        // Update balance status message
        if (balance < 0) {
            binding.tvBalanceStatus.text = "You're overspending!"
        } else if (balance == 0.0) {
            binding.tvBalanceStatus.text = "Break-even point!"
        } else {
            binding.tvBalanceStatus.text = "You're on track!"
        }

        val viewbudget : TextView = findViewById(R.id.btnEditBudget)
        viewbudget.setOnClickListener{
            val intent = Intent(this,BudgetActivity::class.java)
            startActivity(intent)
        }

        // Update budget info
        val monthlyBudget = prefsManager.getMonthlyBudget()
        val remainingBudget = monthlyBudget - totalExpense
        binding.tvRemainingBudget.text = CurrencyFormatter.formatAmount(remainingBudget, currencySymbol)

        // Calculate percentage of budget spent
        val percentSpent = if (monthlyBudget > 0) {
            (totalExpense / monthlyBudget * 100).toInt()
        } else {
            0
        }

        binding.progressBudget.progress = percentSpent
        binding.tvBudgetPercentage.text = "$percentSpent%"

        // Update budget status text and color based on percentage
        val (statusText, colorId) = when {
            percentSpent >= 100 -> Pair("Budget exceeded!", R.color.expense)
            percentSpent >= 80 -> Pair("Getting close to limit!", R.color.warning)
            percentSpent > 0 -> Pair("Budget on track", R.color.success)
            else -> Pair("No expenses yet", R.color.text_secondary)
        }

        binding.tvBudgetStatus.text = statusText
        binding.tvBudgetStatus.setTextColor(getColor(colorId))
        binding.progressBudget.progressTintList = ColorStateList.valueOf(getColor(colorId))
    }


    private fun updateUserName() {
        // First try user_profile SharedPreferences (which we set during login)
        val sharedPrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("name", "")

        if (!userName.isNullOrEmpty()) {
            tvUserName.text = userName
        } else {
            // If not found, try directly via UserManager as backup
            val userManager = UserManager(this)
            val userNameFromManager = userManager.getCurrentUserName()

            if (!userNameFromManager.isNullOrEmpty()) {
                // Found name in UserManager, save it to user_profile for next time
                tvUserName.text = userNameFromManager
                sharedPrefs.edit().putString("name", userNameFromManager).apply()
            } else {
                // If still no name found, use email if available
                val email = userManager.getCurrentUserEmail()
                if (!email.isNullOrEmpty()) {
                    val emailUsername = email.substringBefore("@")
                    tvUserName.text = emailUsername
                    sharedPrefs.edit().putString("name", emailUsername).apply()
                } else {
                    // Last resort fallback
                    tvUserName.text = "User"
                }
            }
        }

        // Update the current date
        val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        binding.tvCurrentDate.text = currentDate
    }
    private fun setupRecentTransactions() {
        val recentTransactions = transactionRepository.getRecentTransactions(5)

        if (recentTransactions.isEmpty()) {
            binding.rvRecentTransactions.visibility = View.GONE
            binding.tvNoTransactions.visibility = View.VISIBLE
        } else {
            binding.rvRecentTransactions.visibility = View.VISIBLE
            binding.tvNoTransactions.visibility = View.GONE

            binding.rvRecentTransactions.layoutManager = LinearLayoutManager(this)

            // Fixed constructor call - now with optional delete parameter
            val adapter = TransactionAdapter(
                recentTransactions,
                prefsManager.getCurrencySymbol(),
                onItemClick = { transaction ->
                    val intent = Intent(this, AddTransactionActivity::class.java).apply {
                        putExtra("TRANSACTION_ID", transaction.id)
                        putExtra("IS_EDIT", true)
                    }
                    startActivity(intent)
                }
                // No need to pass onDeleteClick since we made it optional
            )

            binding.rvRecentTransactions.adapter = adapter
        }

        binding.btnViewAllTransactions.setOnClickListener {
            // Navigate to transactions screen
            startActivity(Intent(this, TransactionActivity::class.java))
        }
    }

    private fun testNotificationDirectly() {
        try {
            val notificationHelper = NotificationHelper(this)
            notificationHelper.showBudgetWarningNotification(85)
            Toast.makeText(this, "Test notification sent directly", Toast.LENGTH_SHORT).show()
            Log.d("MainActivity", "Test notification sent for user: ${NotificationHelper.USER_LOGIN}")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error sending test notification: ${e.message}", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkBudgetAndScheduleReminders() {
        try {
            // For regular checks in MainActivity, use regular service (not foreground)
            if (prefsManager.shouldNotifyBudgetWarning()) {
                val budgetIntent = Intent(this, BudgetCheckService::class.java).apply {
                    action = BudgetCheckService.ACTION_CHECK_BUDGET
                    // Don't set EXTRA_START_AS_FOREGROUND here
                }
                startService(budgetIntent)  // Regular service, not foreground
                Log.d("MainActivity", "Budget check service started")
            }

            // Schedule reminders if enabled
            if (prefsManager.shouldShowDailyReminders()) {
                val reminderIntent = Intent(this, BudgetCheckService::class.java).apply {
                    action = BudgetCheckService.ACTION_SCHEDULE_DAILY_REMINDER
                    // Don't set EXTRA_START_AS_FOREGROUND here
                }
                startService(reminderIntent)  // Regular service, not foreground
                Log.d("MainActivity", "Daily reminders scheduled")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting service: ${e.message}", e)
        }
    }






}