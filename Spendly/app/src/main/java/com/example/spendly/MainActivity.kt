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

        prefsManager = PrefsManager(this)
        transactionRepository = TransactionRepository(this)
        bottomNavHelper = BottomNavHelper(this, binding.root)
        btnSettings = findViewById(R.id.btnSettings)
        bottomNavHelper.setupBottomNav(NavSection.HOME)
        tvUserName = findViewById(R.id.tvUserName)

        btnSettings.setOnClickListener {
            openSettings()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }

        val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvCurrentMonth.text = monthFormatter.format(Date())

        setupDashboard()
        setupRecentTransactions()

        checkBudgetAndScheduleReminders()
    }

    override fun onResume() {
        super.onResume()
        setupDashboard()
        setupRecentTransactions()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            val hasPermission = ContextCompat.checkSelfPermission(
                this,
                "android.permission.POST_NOTIFICATIONS"
            ) == PackageManager.PERMISSION_GRANTED

            Log.d(TAG, "Notification permission already granted: $hasPermission")

            if (!hasPermission) {
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

        val totalIncome = transactionRepository.getTotalIncomeForCurrentMonth()
        val totalExpense = transactionRepository.getTotalExpenseForCurrentMonth()
        val balance = totalIncome - totalExpense

        updateUserName()

        binding.tvTotalIncome.text = CurrencyFormatter.formatAmount(totalIncome, currencySymbol)
        binding.tvTotalExpense.text = CurrencyFormatter.formatAmount(totalExpense, currencySymbol)
        binding.tvBalance.text = CurrencyFormatter.formatAmount(balance, currencySymbol)

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

        val monthlyBudget = prefsManager.getMonthlyBudget()
        if (monthlyBudget > 0) {
            val remainingBudget = monthlyBudget - totalExpense
            binding.tvRemainingBudget.text = CurrencyFormatter.formatAmount(remainingBudget, currencySymbol)
            binding.tvTotalBudget.text = "of ${CurrencyFormatter.formatAmount(monthlyBudget, currencySymbol)}"

            val percentSpent = if (monthlyBudget > 0) {
                (totalExpense / monthlyBudget * 100).toInt()
            } else {
                0
            }

            binding.progressBudget.progress = percentSpent
            binding.tvBudgetPercentage.text = "$percentSpent%"

            val (statusText, colorId) = when {
                percentSpent >= 100 -> Pair("Budget exceeded!", R.color.expense)
                percentSpent >= 80 -> Pair("Getting close to limit!", R.color.warning)
                percentSpent > 0 -> Pair("Budget on track", R.color.success)
                else -> Pair("No expenses yet", R.color.text_secondary)
            }

            binding.tvBudgetStatus.text = statusText
            binding.tvBudgetStatus.setTextColor(getColor(colorId))
            binding.progressBudget.progressTintList = ColorStateList.valueOf(getColor(colorId))
        } else {
            binding.tvRemainingBudget.text = CurrencyFormatter.formatAmount(0.0, currencySymbol)
            binding.tvTotalBudget.text = "No budget set"
            binding.tvBudgetStatus.text = "Set your monthly budget"
            binding.tvBudgetStatus.setTextColor(getColor(R.color.text_secondary))
            binding.progressBudget.progress = 0
            binding.tvBudgetPercentage.text = "0%"
            
            binding.cardBudget.setOnClickListener {
                val intent = Intent(this, BudgetActivity::class.java)
                startActivity(intent)
            }
        }

        val expensesByCategory = transactionRepository.getExpensesByCategory()
        if (expensesByCategory.isNotEmpty()) {
            val topCategory = expensesByCategory.maxByOrNull { it.value }
            if (topCategory != null) {
                binding.tvTopCategory.text = topCategory.key
                binding.tvTopCategoryAmount.text = CurrencyFormatter.formatAmount(topCategory.value, currencySymbol)
                
                when (topCategory.key.lowercase()) {
                    "food" -> binding.imgTopCategory.setImageResource(R.drawable.ic_category_food)
                    "transport" -> binding.imgTopCategory.setImageResource(R.drawable.ic_category_transport)
                    "bills" -> binding.imgTopCategory.setImageResource(R.drawable.ic_category_bills)
                    "entertainment" -> binding.imgTopCategory.setImageResource(R.drawable.ic_category_entertainment)
                    "shopping" -> binding.imgTopCategory.setImageResource(R.drawable.ic_category_shopping)
                    "health" -> binding.imgTopCategory.setImageResource(R.drawable.ic_category_health)
                    "education" -> binding.imgTopCategory.setImageResource(R.drawable.ic_category_education)
                    else -> binding.imgTopCategory.setImageResource(R.drawable.ic_category_other)
                }
            }
        }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000

        val todayTransactions = transactionRepository.getTransactionsByDateRange(startOfDay, endOfDay)
        binding.tvRecentActivityCount.text = todayTransactions.size.toString()
    }

    private fun updateUserName() {
        val sharedPrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        val userName = sharedPrefs.getString("name", "")

        if (!userName.isNullOrEmpty()) {
            tvUserName.text = userName
        } else {
            val userManager = UserManager(this)
            val userNameFromManager = userManager.getCurrentUserName()

            if (!userNameFromManager.isNullOrEmpty()) {
                tvUserName.text = userNameFromManager
                sharedPrefs.edit().putString("name", userNameFromManager).apply()
            } else {
                val email = userManager.getCurrentUserEmail()
                if (!email.isNullOrEmpty()) {
                    val emailUsername = email.substringBefore("@")
                    tvUserName.text = emailUsername
                    sharedPrefs.edit().putString("name", emailUsername).apply()
                } else {
                    tvUserName.text = "User"
                }
            }
        }

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
            )

            binding.rvRecentTransactions.adapter = adapter
        }

        binding.btnViewAllTransactions.setOnClickListener {
            startActivity(Intent(this, TransactionActivity::class.java))
        }
    }

    private fun checkBudgetAndScheduleReminders() {
        try {
            if (prefsManager.shouldShowDailyReminders()) {
                val reminderIntent = Intent(this, BudgetCheckService::class.java).apply {
                    action = BudgetCheckService.ACTION_SCHEDULE_DAILY_REMINDER
                }
                startService(reminderIntent)
                Log.d(TAG, "Daily reminders scheduled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting service: ${e.message}", e)
        }
    }
}