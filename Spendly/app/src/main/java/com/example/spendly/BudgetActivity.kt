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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendly.databinding.ActivityBudgetBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import android.graphics.Color
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DefaultItemAnimator
import java.util.*

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

        applyAnimations()

        binding.root.postDelayed({
            setupMonthlyBudget()
            setupCategoryBudgets()

            if (intent.getBooleanExtra("ADJUST_BUDGET", false)) {
                val exceededAmount = intent.getDoubleExtra("EXCEEDED_AMOUNT", 0.0)
                showBudgetAdjustDialog(exceededAmount)
            }
        }, 300)
    }

    private fun setupBottomNav() {
        try {
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
        slideUp.interpolator = OvershootInterpolator(0.8f)
        binding.cardMonthlyBudget.startAnimation(slideUp)

        binding.categoryBudgetsHeader.alpha = 0f
        binding.categoryBudgetsHeader.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(400)
            .start()
    }

private fun showBudgetAdjustDialog(exceededAmount: Double) {
    val dialogView = layoutInflater.inflate(R.layout.dialog_edit_budget, null)
    val etBudget = dialogView.findViewById<TextInputEditText>(R.id.etBudget)
    val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

    tvDialogTitle.text = "Adjust Monthly Budget"

    val currentBudget = prefsManager.getMonthlyBudget()
    val totalExpense = transactionRepository.getTotalExpenseForCurrentMonth()

    // Calculate a suggested new budget (current expense + 10% buffer)
    val suggestedBudget = Math.ceil(totalExpense * 1.1)

    etBudget.setText(suggestedBudget.toString())
    etBudget.hint = "Enter amount (min: $totalExpense)"

    MaterialAlertDialogBuilder(this)
        .setView(dialogView)
        .setTitle("Budget Exceeded")
        .setMessage("Your expenses exceeded your budget by ${CurrencyFormatter.formatAmount(exceededAmount, prefsManager.getCurrencySymbol())}. Please adjust your budget accordingly.")
        .setPositiveButton("Adjust Budget") { _, _ ->
            try {
                val newBudget = etBudget.text.toString().toDoubleOrNull() ?: 0.0

                if (newBudget >= totalExpense) {
                    prefsManager.setMonthlyBudget(newBudget)

                    binding.layoutBudgetSet.alpha = 0.5f
                    binding.layoutBudgetSet.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .withEndAction {
                            setupMonthlyBudget()
                        }
                        .start()

                    showSuccessSnackbar("Budget adjusted successfully!")

                    // Check budget status - resets notifications
                    startService(Intent(this, BudgetCheckService::class.java).apply {
                        action = BudgetCheckService.ACTION_CHECK_BUDGET
                    })
                } else {
                    Snackbar.make(binding.root,
                        "Budget must be at least equal to your current expenses",
                        Snackbar.LENGTH_LONG).show()

                    // Show dialog again with error
                    showBudgetAdjustDialog(exceededAmount)
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Invalid budget amount", Snackbar.LENGTH_SHORT).show()
            }
        }
        .setNegativeButton("Cancel", null)
        .show()
}

    private fun setupMonthlyBudget() {
        val currencySymbol = prefsManager.getCurrencySymbol()
        val monthlyBudget = prefsManager.getMonthlyBudget()

        if (monthlyBudget > 0) {
            binding.layoutBudgetSet.visibility = View.VISIBLE
            binding.layoutNoBudget.visibility = View.GONE

            binding.btnEditBudget.visibility = View.VISIBLE
            binding.btnDeleteBudget.visibility = View.VISIBLE

            binding.btnCreateBudget.text = "Set Budget Now"
            binding.btnCreateBudget.setIconResource(R.drawable.ic_add)

            binding.tvMonthlyBudget.text = CurrencyFormatter.formatAmount(monthlyBudget, currencySymbol)

            val totalExpense = transactionRepository.getTotalExpenseForCurrentMonth()
            val percentSpent = if (monthlyBudget > 0) {
                ((totalExpense / monthlyBudget) * 100).toInt().coerceIn(0, 100)
            } else 0

            binding.progressBudget.progress = 0
            binding.progressBudget.animate().alpha(1f).setDuration(500).start()
            animateProgressBar(percentSpent)

            binding.tvBudgetPercent.text = "$percentSpent%"

            val spent = CurrencyFormatter.formatAmount(totalExpense, currencySymbol)
            val remaining = CurrencyFormatter.formatAmount(monthlyBudget - totalExpense, currencySymbol)
            binding.tvSpent.text = spent
            binding.tvRemaining.text = remaining

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
            binding.layoutBudgetSet.visibility = View.GONE
            binding.layoutNoBudget.visibility = View.VISIBLE

            binding.btnEditBudget.visibility = View.GONE
            binding.btnDeleteBudget.visibility = View.GONE

            binding.btnCreateBudget.text = "Set Budget Now"
            binding.btnCreateBudget.setIconResource(R.drawable.ic_add)
            binding.btnCreateBudget.setBackgroundColor(getColor(R.color.primary))

            binding.progressBudget.progress = 0
        }

        binding.btnEditBudget.setOnClickListener {
            showBudgetEditDialog()
        }

        binding.btnDeleteBudget.setOnClickListener {
            confirmDeleteBudget()
        }

        binding.btnCreateBudget.setOnClickListener {
            showBudgetAddDialog()
        }
    }

    private fun animateProgressBar(targetProgress: Int) {
        val animator = android.animation.ValueAnimator.ofInt(0, targetProgress)
        animator.duration = 1000
        animator.addUpdateListener { animation ->
            val progress = animation.animatedValue as Int
            binding.progressBudget.progress = progress
        }
        animator.start()
    }

    private fun confirmDeleteBudget() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Monthly Budget")
            .setMessage("Are you sure you want to delete your monthly budget? This will reset your budget tracking.")
            .setPositiveButton("Delete") { _, _ ->
                prefsManager.setMonthlyBudget(0.0)

                binding.layoutBudgetSet.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        binding.btnEditBudget.visibility = View.GONE
                        binding.btnDeleteBudget.visibility = View.GONE

                        setupMonthlyBudget()

                        binding.layoutNoBudget.alpha = 0f
                        binding.layoutNoBudget.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()

                        Snackbar.make(binding.root, "Monthly budget deleted", Snackbar.LENGTH_LONG)
                            .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                            .show()
                    }
                    .start()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupCategoryBudgets() {
        val categoryBudgets = getCategoryBudgets()

        if (categoryBudgets.isEmpty()) {
            binding.rvCategoryBudgets.visibility = View.GONE
            binding.layoutEmptyCategoryBudgets.visibility = View.VISIBLE

            binding.layoutEmptyCategoryBudgets.alpha = 0f
            binding.layoutEmptyCategoryBudgets.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

        } else {
            binding.rvCategoryBudgets.visibility = View.VISIBLE
            binding.layoutEmptyCategoryBudgets.visibility = View.GONE

            binding.rvCategoryBudgets.layoutManager = LinearLayoutManager(this)

            val itemAnimator = binding.rvCategoryBudgets.itemAnimator as DefaultItemAnimator
            itemAnimator.addDuration = 300
            itemAnimator.removeDuration = 300

            categoryBudgetAdapter = CategoryBudgetAdapter(
                this,
                categoryBudgets,
                prefsManager.getCurrencySymbol()
            ) { categoryBudget ->
                showCategoryBudgetEditDialog(categoryBudget.category)
            }
            binding.rvCategoryBudgets.adapter = categoryBudgetAdapter
        }

        binding.btnAddCategoryBudget.setOnClickListener {
            showCategoryBudgetAddDialog()
        }

        binding.btnAddFirstCategoryBudget.setOnClickListener {
            showCategoryBudgetAddDialog()
        }
    }

    private fun showBudgetAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_budget, null)
        val etBudget = dialogView.findViewById<TextInputEditText>(R.id.etBudget)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

        tvDialogTitle.text = "Create Monthly Budget"

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Add Budget") { _, _ ->
                try {
                    val newBudget = etBudget.text.toString().toDoubleOrNull() ?: 0.0

                    if (newBudget > 0) {
                        prefsManager.setMonthlyBudget(newBudget)

                        binding.layoutNoBudget.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction {
                                setupMonthlyBudget()
                                binding.layoutBudgetSet.alpha = 0f
                                binding.layoutBudgetSet.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start()

                                startService(Intent(this, BudgetCheckService::class.java))

                                showSuccessSnackbar("Budget created!")
                            }
                            .start()
                    } else {
                        Snackbar.make(binding.root, "Please enter a valid amount", Snackbar.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Invalid budget amount", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showBudgetEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_budget, null)
        val etBudget = dialogView.findViewById<TextInputEditText>(R.id.etBudget)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

        tvDialogTitle.text = "Edit Monthly Budget"

        val currentBudget = prefsManager.getMonthlyBudget()
        etBudget.setText(currentBudget.toString())

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Save Changes") { _, _ ->
                try {
                    val newBudget = etBudget.text.toString().toDoubleOrNull() ?: 0.0

                    if (newBudget > 0) {
                        prefsManager.setMonthlyBudget(newBudget)

                        binding.layoutBudgetSet.alpha = 0.5f
                        binding.layoutBudgetSet.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .withEndAction {
                                setupMonthlyBudget()
                            }
                            .start()

                        showSuccessSnackbar("Budget updated!")

                        startService(Intent(this, BudgetCheckService::class.java))
                    } else {
                        Snackbar.make(binding.root, "Please enter a valid amount", Snackbar.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Invalid budget amount", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCategoryBudgetAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_category_budget, null)
        val spinner = dialogView.findViewById<androidx.appcompat.widget.AppCompatSpinner>(R.id.spinnerCategory)
        val etBudget = dialogView.findViewById<TextInputEditText>(R.id.etCategoryBudget)

        val expenseCategories = getExpenseCategories()
        val categoryNames = expenseCategories.map { it.name }.toTypedArray()

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

                        val isFirstCategoryBudget = binding.layoutEmptyCategoryBudgets.visibility == View.VISIBLE

                        if (isFirstCategoryBudget) {
                            binding.layoutEmptyCategoryBudgets.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction {
                                    setupCategoryBudgets()

                                    binding.rvCategoryBudgets.alpha = 0f
                                    binding.rvCategoryBudgets.animate()
                                        .alpha(1f)
                                        .setDuration(300)
                                        .start()
                                }
                                .start()
                        } else {
                            setupCategoryBudgets()
                        }

                        showSuccessSnackbar("Category budget added!")
                    } else {
                        Snackbar.make(binding.root, "Please enter a valid amount", Snackbar.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Error adding category budget", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showCategoryBudgetEditDialog(category: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_category_budget, null)
        val tvCategoryName = dialogView.findViewById<TextView>(R.id.tvCategoryName)
        val etBudget = dialogView.findViewById<TextInputEditText>(R.id.etCategoryBudget)

        tvCategoryName.text = category

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
                    if (budget > 0) {
                        prefsManager.setCategoryBudget(category, budget)
                        setupCategoryBudgets()
                        showSuccessSnackbar("Category budget updated!")
                    } else {
                        Snackbar.make(binding.root, "Please enter a valid amount", Snackbar.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Error updating category budget", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton("Delete") { _, _ ->
                confirmDeleteCategoryBudget(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDeleteCategoryBudget(category: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Category Budget")
            .setMessage("Are you sure you want to delete the budget for $category?")
            .setPositiveButton("Delete") { _, _ ->
                prefsManager.setCategoryBudget(category, 0.0)

                val remainingBudgets = getCategoryBudgets().size - 1

                if (remainingBudgets == 0) {
                    binding.rvCategoryBudgets.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            setupCategoryBudgets()
                        }
                        .start()
                } else {
                    setupCategoryBudgets()
                }

                Snackbar.make(binding.root, "Category budget deleted", Snackbar.LENGTH_SHORT)
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSuccessSnackbar(message: String) {
        val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.success))
        snackbar.setTextColor(Color.WHITE)
        snackbar.setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
        snackbar.show()
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
            Category("transport", "Transport","Transport", R.drawable.ic_category_transport, R.color.category_transport),
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
            navigateToMainActivity()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
    }

    override fun onBackPressed() {
        navigateToMainActivity()
    }
}