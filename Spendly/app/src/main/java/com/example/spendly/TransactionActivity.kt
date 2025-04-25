package com.example.spendly

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendly.AddTransactionActivity
import com.example.spendly.databinding.ActivityTransactionBinding
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

class TransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransactionBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var prefsManager: PrefsManager
    private lateinit var bottomNavHelper: BottomNavHelper

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private var selectedCategory: String? = null
    private var currentFilterType: FilterType = FilterType.ALL

    private enum class FilterType {
        ALL, INCOME, EXPENSE, DATE, CATEGORY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        prefsManager = PrefsManager(this)
        bottomNavHelper = BottomNavHelper(this, binding.root)

        setupToolbar()
        setupBottomNav()
        setupRecyclerView()
        setupFilterOptions()
        setupEmptyState()
        setupButtons()

        loadTransactions()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Transactions"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupBottomNav() {
        try {
            if (::bottomNavHelper.isInitialized) {
                bottomNavHelper.setupBottomNav(NavSection.TRANSACTIONS)
            } else {
                Log.e("TransactionActivity", "bottomNavHelper not initialized correctly")
            }
        } catch (e: Exception) {
            Log.e("TransactionActivity", "Error setting up bottom nav: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupButtons() {
        binding.btnAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
            overridePendingTransition(R.anim.slide_up, R.anim.fade_out)
        }

        binding.btnDateFilter.setOnClickListener {
            if (binding.layoutDateFilter.visibility == View.VISIBLE) {
                binding.layoutDateFilter.visibility = View.GONE
            } else {
                binding.layoutDateFilter.visibility = View.VISIBLE
                binding.layoutCategoryFilter.visibility = View.GONE
                if (currentFilterType != FilterType.DATE) {
                    currentFilterType = FilterType.DATE
                    binding.chipAll.isChecked = false
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvTransactions.layoutManager = LinearLayoutManager(this)

        val emptyList = mutableListOf<Transaction>()

        transactionAdapter = TransactionAdapter(
            emptyList,
            prefsManager.getCurrencySymbol(),
            onItemClick = { transaction ->
                editTransaction(transaction)
            },
            onDeleteClick = { transaction ->
                confirmDeleteTransaction(transaction)
            }
        )
        binding.rvTransactions.adapter = transactionAdapter
    }

    private fun updateTransactionList(transactions: List<Transaction>) {
        if (transactions.isEmpty()) {
            binding.rvTransactions.visibility = View.GONE
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.tvEmptyTitle.text = "No Transactions Found"
            binding.tvEmptyMessage.text = when (currentFilterType) {
                FilterType.ALL -> "Add your first transaction to start tracking your finances"
                FilterType.INCOME -> "No income transactions found"
                FilterType.EXPENSE -> "No expense transactions found"
                FilterType.DATE -> "No transactions found in the selected date range"
                FilterType.CATEGORY -> "No transactions found for this category"
            }
        } else {
            binding.rvTransactions.visibility = View.VISIBLE
            binding.emptyStateLayout.visibility = View.GONE

            transactionAdapter = TransactionAdapter(
                transactions,
                prefsManager.getCurrencySymbol(),
                onItemClick = { transaction ->
                    editTransaction(transaction)
                },
                onDeleteClick = { transaction ->
                    confirmDeleteTransaction(transaction)
                }
            )
            binding.rvTransactions.adapter = transactionAdapter

            val animation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
            binding.rvTransactions.layoutAnimation = animation
            binding.rvTransactions.scheduleLayoutAnimation()
        }
    }

    private fun setupFilterOptions() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = findViewById<Chip>(checkedIds[0])
                when (chip.id) {
                    R.id.chipAll -> {
                        currentFilterType = FilterType.ALL
                        binding.layoutDateFilter.visibility = View.GONE
                        binding.layoutCategoryFilter.visibility = View.GONE
                    }
                    R.id.chipIncome -> {
                        currentFilterType = FilterType.INCOME
                        binding.layoutDateFilter.visibility = View.GONE
                        binding.layoutCategoryFilter.visibility = View.GONE
                    }
                    R.id.chipExpense -> {
                        currentFilterType = FilterType.EXPENSE
                        binding.layoutDateFilter.visibility = View.GONE
                        binding.layoutCategoryFilter.visibility = View.GONE
                    }
                    R.id.chipCategory -> {
                        currentFilterType = FilterType.CATEGORY
                        binding.layoutDateFilter.visibility = View.GONE
                        binding.layoutCategoryFilter.visibility = View.VISIBLE
                        showCategorySelector()
                    }
                }
                loadTransactions()
            }
        }

        binding.btnStartDate.setOnClickListener {
            showDatePicker(true)
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker(false)
        }

        binding.btnSelectCategory.setOnClickListener {
            showCategorySelector()
        }

        binding.chipAll.isChecked = true
    }

    private fun setupEmptyState() {
        binding.btnAddFirstTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
            overridePendingTransition(R.anim.slide_up, R.anim.fade_out)
        }
    }

    private fun loadTransactions() {
        val transactions = when (currentFilterType) {
            FilterType.ALL -> transactionRepository.getAllTransactions()
            FilterType.INCOME -> transactionRepository.getTransactionsByType(true)
            FilterType.EXPENSE -> transactionRepository.getTransactionsByType(false)
            FilterType.DATE -> {
                if (startDate != null && endDate != null) {
                    transactionRepository.getTransactionsByDateRange(
                        startDate!!.timeInMillis,
                        endDate!!.timeInMillis
                    )
                } else if (startDate != null) {
                    transactionRepository.getTransactionsAfterDate(startDate!!.timeInMillis)
                } else if (endDate != null) {
                    transactionRepository.getTransactionsBeforeDate(endDate!!.timeInMillis)
                } else {
                    transactionRepository.getAllTransactions()
                }
            }
            FilterType.CATEGORY -> {
                if (selectedCategory != null) {
                    transactionRepository.getTransactionsByCategory(selectedCategory!!)
                } else {
                    transactionRepository.getAllTransactions()
                }
            }
        }

        updateTransactionList(transactions)
        updateSummary(transactions)
    }

    private fun updateSummary(transactions: List<Transaction>) {
        val currencySymbol = prefsManager.getCurrencySymbol()

        var totalIncome = 0.0
        var totalExpense = 0.0

        for (transaction in transactions) {
            if (transaction.isIncome) {
                totalIncome += transaction.amount
            } else {
                totalExpense += transaction.amount
            }
        }

        val balance = totalIncome - totalExpense

        binding.tvTotalIncome.text = CurrencyFormatter.formatAmount(totalIncome, currencySymbol)
        binding.tvTotalExpense.text = CurrencyFormatter.formatAmount(totalExpense, currencySymbol)
        binding.tvBalance.text = CurrencyFormatter.formatAmount(balance, currencySymbol)

        val headerText = when (currentFilterType) {
            FilterType.ALL -> "All Transactions"
            FilterType.INCOME -> "Income Transactions"
            FilterType.EXPENSE -> "Expense Transactions"
            FilterType.DATE -> {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                val startStr = startDate?.let { dateFormat.format(it.time) } ?: "Any"
                val endStr = endDate?.let { dateFormat.format(it.time) } ?: "Any"
                "Transactions: $startStr to $endStr"
            }
            FilterType.CATEGORY -> "Category: ${selectedCategory ?: "All"}"
        }
        binding.tvTransactionsHeader.text = headerText
    }

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(year, month, day)

            if (isStartDate) {
                startDate = selectedCalendar
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                binding.btnStartDate.text = dateFormat.format(startDate!!.time)
            } else {
                endDate = selectedCalendar
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                binding.btnEndDate.text = dateFormat.format(endDate!!.time)
            }

            loadTransactions()
        }

        val datePickerDialog = DatePickerDialog(
            this,
            dateListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showCategorySelector() {
        val categories = getExpenseCategories().map { it.name }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Category")
            .setItems(categories) { _, which ->
                selectedCategory = categories[which]
                binding.btnSelectCategory.text = selectedCategory
                loadTransactions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun editTransaction(transaction: Transaction) {
        val intent = Intent(this, AddTransactionActivity::class.java)
        intent.putExtra("TRANSACTION_ID", transaction.id)
        intent.putExtra("IS_EDIT", true)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_up, R.anim.fade_out)
    }

    private fun confirmDeleteTransaction(transaction: Transaction) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction(transaction)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction(transaction: Transaction) {
        transactionRepository.deleteTransaction(transaction.id)
        
        BackupHelper(this).backupUserData()
        
        loadTransactions()
    }

    private fun getExpenseCategories(): List<Category> {
        return listOf(
            Category("food", "Food", "Food", R.drawable.ic_category_food, R.color.category_food),
            Category("transport", "Transport", "Transport", R.drawable.ic_category_transport, R.color.category_transport),
            Category("bills", "Bills", "Bills", R.drawable.ic_category_bills, R.color.category_bills),
            Category("entertainment", "Entertainment", "Entertainment", R.drawable.ic_category_entertainment, R.color.category_entertainment),
            Category("shopping", "Shopping", "Shopping", R.drawable.ic_category_shopping, R.color.category_shopping),
            Category("health", "Health", "Health", R.drawable.ic_category_health, R.color.category_health),
            Category("education", "Education", "Education", R.drawable.ic_category_education, R.color.category_education),
            Category("other", "Other", "Other", R.drawable.ic_category_other, R.color.category_other)
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }
}