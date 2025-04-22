package com.example.spendly

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendly.databinding.ActivityBudgetBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class BudgetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBudgetBinding
    private lateinit var prefsManager: PrefsManager
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var bottomNavHelper: BottomNavHelper

    private lateinit var categoryBudgetAdapter: CategoryBudgetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PrefsManager(this)
        transactionRepository = TransactionRepository(this)
        bottomNavHelper = BottomNavHelper(this, binding.root)

        setupBottomNav()
        setupToolbar()
        setupMonthlyBudget()
        setupCategoryBudgets()
        setupNotifications()

        // Apply entrance animations
        applyAnimations()
    }

    private fun setupBottomNav() {
        try {
            // Make sure the bottomNavHelper is initialized after setContentView
            if (::bottomNavHelper.isInitialized) {
                bottomNavHelper.setupBottomNav(NavSection.BUDGET)
            } else {
                Log.e("BudgetActivity", "bottomNavHelper not initialized correctly")
            }
        } catch (e: Exception) {
            Log.e("BudgetActivity", "Error setting up bottom nav: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Budget Management"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun applyAnimations() {
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.cardMonthlyBudget.startAnimation(slideUp)

        val categoryHeader = binding.categoryBudgetsHeader
        categoryHeader.alpha = 0f
        categoryHeader.animate().alpha(1f).setDuration(500).setStartDelay(300).start()

        val staggeredAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        binding.root.postDelayed({
            binding.notificationCard.startAnimation(staggeredAnimation)
        }, 200)
    }

    private fun setupMonthlyBudget() {
        val currencySymbol = prefsManager.getCurrencySymbol()
        val monthlyBudget = prefsManager.getMonthlyBudget()

        if (monthlyBudget > 0) {
            binding.tvMonthlyBudget.text = CurrencyFormatter.formatAmount(monthlyBudget, currencySymbol)

            // Calculate progress
            val totalExpense = transactionRepository.getTotalExpenseForCurrentMonth()
            val percentSpent = if (monthlyBudget > 0) {
                ((totalExpense / monthlyBudget) * 100).toInt().coerceIn(0, 100)
            } else 0

            binding.progressBudget.progress = percentSpent
            binding.tvBudgetPercent.text = "$percentSpent%"

            val spent = CurrencyFormatter.formatAmount(totalExpense, currencySymbol)
            val remaining = CurrencyFormatter.formatAmount(monthlyBudget - totalExpense, currencySymbol)
            binding.tvSpent.text = spent
            binding.tvRemaining.text = remaining

            // Update status based on spending
            when {
                percentSpent >= 100 -> {
                    binding.tvBudgetStatus.text = "Budget exceeded!"
                    binding.tvBudgetStatus.setTextColor(getColor(R.color.error))
                    binding.progressBudget.progressTintList = ColorStateList.valueOf(getColor(R.color.error))
                }
                percentSpent >= 80 -> {
                    binding.tvBudgetStatus.text = "Approaching limit!"
                    binding.tvBudgetStatus.setTextColor(getColor(R.color.warning))
                    binding.progressBudget.progressTintList = ColorStateList.valueOf(getColor(R.color.warning))
                }
                percentSpent > 0 -> {
                    binding.tvBudgetStatus.text = "Budget on track"
                    binding.tvBudgetStatus.setTextColor(getColor(R.color.success))
                    binding.progressBudget.progressTintList = ColorStateList.valueOf(getColor(R.color.success))
                }
                else -> {
                    binding.tvBudgetStatus.text = "No expenses yet"
                    binding.tvBudgetStatus.setTextColor(getColor(R.color.text_secondary))
                    binding.progressBudget.progressTintList = ColorStateList.valueOf(getColor(R.color.text_secondary))
                }
            }
        } else {
            binding.tvMonthlyBudget.text = "No budget set"
            binding.progressBudget.progress = 0
            binding.tvBudgetPercent.text = "0%"
            binding.tvBudgetStatus.text = "Set a budget to track spending"
            binding.tvBudgetStatus.setTextColor(getColor(R.color.text_secondary))
            binding.tvSpent.text = CurrencyFormatter.formatAmount(0.0, currencySymbol)
            binding.tvRemaining.text = CurrencyFormatter.formatAmount(0.0, currencySymbol)
        }

        // Set up edit button
        binding.btnEditBudget.setOnClickListener {
            showBudgetEditDialog()
        }
    }

    private fun setupCategoryBudgets() {
        val categoryBudgets = getCategoryBudgets()

        if (categoryBudgets.isEmpty()) {
            binding.rvCategoryBudgets.visibility = View.GONE
            binding.layoutEmptyCategoryBudgets.visibility = View.VISIBLE
        } else {
            binding.rvCategoryBudgets.visibility = View.VISIBLE
            binding.layoutEmptyCategoryBudgets.visibility = View.GONE

            binding.rvCategoryBudgets.layoutManager = LinearLayoutManager(this)
            categoryBudgetAdapter = CategoryBudgetAdapter(
                this,
                categoryBudgets,
                prefsManager.getCurrencySymbol()
            ) { categoryBudget ->
                showCategoryBudgetEditDialog(categoryBudget.category)
            }
            binding.rvCategoryBudgets.adapter = categoryBudgetAdapter
        }

        // Set up add buttons
        binding.btnAddCategoryBudget.setOnClickListener {
            showCategoryBudgetAddDialog()
        }

        binding.btnAddFirstCategoryBudget.setOnClickListener {
            showCategoryBudgetAddDialog()
        }
    }

    private fun setupNotifications() {
        // Budget alerts switch
        binding.switchBudgetAlerts.isChecked = prefsManager.shouldNotifyBudgetWarning()
        binding.switchBudgetAlerts.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setShouldNotifyBudgetWarning(isChecked)

            // If enabled, start the budget check service
            if (isChecked) {
                startService(Intent(this, BudgetCheckService::class.java))
            }
        }

        // Daily reminders switch
        binding.switchDailyReminders.isChecked = prefsManager.shouldShowDailyReminders()
        binding.switchDailyReminders.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setShouldShowDailyReminders(isChecked)
            binding.btnSetReminderTime.isEnabled = isChecked
        }

        // Reminder time button
        binding.btnSetReminderTime.isEnabled = binding.switchDailyReminders.isChecked
        binding.btnSetReminderTime.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun showTimePickerDialog() {
        // Implementation for time picker dialog
        // This would typically use a TimePickerDialog to select reminder time
        // For now just show a toast message
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Coming Soon")
            .setMessage("Time picker functionality will be available in the next update.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showBudgetEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_budget, null)
        val etBudget = dialogView.findViewById<TextInputEditText>(R.id.etBudget)

        // Pre-fill with current budget if any
        val currentBudget = prefsManager.getMonthlyBudget()
        if (currentBudget > 0) {
            etBudget.setText(currentBudget.toString())
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Set Monthly Budget")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                try {
                    val newBudget = etBudget.text.toString().toDoubleOrNull() ?: 0.0

                    if (newBudget > 0) {
                        prefsManager.setMonthlyBudget(newBudget)
                        setupMonthlyBudget() // Refresh the UI

                        // Check budget status
                        startService(Intent(this, BudgetCheckService::class.java))
                    }
                } catch (e: Exception) {
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Please enter a valid budget amount")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCategoryBudgetAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category_budget, null)
        val spinner = dialogView.findViewById<androidx.appcompat.widget.AppCompatSpinner>(R.id.spinnerCategory)
        val etBudget = dialogView.findViewById<TextInputEditText>(R.id.etCategoryBudget)

        // Get all expense categories
        val expenseCategories = getExpenseCategories()
        val categoryNames = expenseCategories.map { it.name }.toTypedArray()

        // Setup spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        MaterialAlertDialogBuilder(this)
            .setTitle("Add Category Budget")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                try {
                    val category = spinner.selectedItem.toString()
                    val budget = etBudget.text.toString().toDoubleOrNull() ?: 0.0

                    if (budget > 0) {
                        prefsManager.setCategoryBudget(category, budget)
                        setupCategoryBudgets() // Refresh the UI
                    }
                } catch (e: Exception) {
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Please enter a valid budget amount")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCategoryBudgetEditDialog(category: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_category_budget, null)
        val tvCategoryName = dialogView.findViewById<TextView>(R.id.tvCategoryName)
        val etBudget = dialogView.findViewById<TextInputEditText>(R.id.etCategoryBudget)

        // Set category name
        tvCategoryName.text = category

        // Pre-fill with current budget if any
        val currentBudget = prefsManager.getCategoryBudget(category)
        if (currentBudget > 0) {
            etBudget.setText(currentBudget.toString())
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Edit Category Budget")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                try {
                    val budget = etBudget.text.toString().toDoubleOrNull() ?: 0.0
                    prefsManager.setCategoryBudget(category, budget)
                    setupCategoryBudgets() // Refresh the UI
                } catch (e: Exception) {
                    AlertDialog.Builder(this)
                        .setTitle("Error")
                        .setMessage("Please enter a valid budget amount")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                prefsManager.setCategoryBudget(category, 0.0)
                setupCategoryBudgets() // Refresh the UI
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getCategoryBudgets(): List<CategoryBudget> {
        val expenseCategories = getExpenseCategories()
        val result = mutableListOf<CategoryBudget>()

        for (category in expenseCategories) {
            val budget = prefsManager.getCategoryBudget(category.name)
            if (budget > 0) {
                val spent = transactionRepository.getExpenseForCategory(category.name)
                result.add(CategoryBudget(category.name, budget, spent))
            }
        }

        return result
    }

    private fun getExpenseCategories(): List<Category> {
        return listOf(
            Category("food", "Food","Food", R.drawable.ic_category_food, R.color.category_food),
            Category("transport", "Transport","Transport" ,R.drawable.ic_category_transport, R.color.category_transport),
            Category("bills", "Bills","Bills", R.drawable.ic_category_bills, R.color.category_bills),
            Category("entertainment", "Entertainment","Entertainment", R.drawable.ic_category_entertainment, R.color.category_entertainment),
            Category("shopping", "Shopping","Shopping", R.drawable.ic_category_shopping, R.color.category_shopping),
            Category("health", "Health","Health", R.drawable.ic_category_health, R.color.category_health),
            Category("education", "Education","Education", R.drawable.ic_category_education, R.color.category_education),
            Category("other", "Other","Other", R.drawable.ic_category_other, R.color.category_other)
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}