package com.example.spendly

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendly.TransactionAdapter
import com.example.spendly.PrefsManager
import com.example.spendly.TransactionRepository
import com.example.spendly.databinding.ActivityMainBinding
import com.example.spendly.Transaction
import com.example.spendly.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PrefsManager
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var bottomNavHelper: BottomNavHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // Initialize repositories and managers
        prefsManager = PrefsManager(this)
        transactionRepository = TransactionRepository(this)
        bottomNavHelper = BottomNavHelper(this, binding.root)


        bottomNavHelper.setupBottomNav(NavSection.HOME)


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

    private fun setupDashboard() {
        val currencySymbol = prefsManager.getCurrencySymbol()

        // Update income, expense and balance
        val totalIncome = transactionRepository.getTotalIncomeForCurrentMonth()
        val totalExpense = transactionRepository.getTotalExpenseForCurrentMonth()
        val balance = totalIncome - totalExpense

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
                // Handle transaction click
//                val intent = Intent(this, AddTransactionActivity::class.java).apply {
//                    putExtra("TRANSACTION_ID", transaction.id)
//                }
                startActivity(intent)
            }

            binding.rvRecentTransactions.adapter = adapter
        }

//        binding.btnViewAllTransactions.setOnClickListener {
//            // Navigate to transactions screen
//            startActivity(Intent(this, TransactionsActivity::class.java))
//        }
    }


}