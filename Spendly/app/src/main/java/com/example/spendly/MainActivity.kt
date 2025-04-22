package com.example.spendly

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

        btnSettings.setOnClickListener {
            openSettings()
        }

        // Set the current month in the header
        val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvCurrentMonth.text = monthFormatter.format(Date())

        // Set up UI
        setupDashboard()
        setupRecentTransactions()

    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to this activity
        setupDashboard()
        setupRecentTransactions()
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
            val adapter = TransactionAdapter(
                recentTransactions,
                prefsManager.getCurrencySymbol()
            ) { transaction ->
                val intent = Intent(this, AddTransactionActivity::class.java).apply {
                    putExtra("TRANSACTION_ID", transaction.id)
                }
                startActivity(intent)
            }

            binding.rvRecentTransactions.adapter = adapter
        }

        binding.btnViewAllTransactions.setOnClickListener {
            // Navigate to transactions screen
            startActivity(Intent(this, TransactionActivity::class.java))
        }
    }


}