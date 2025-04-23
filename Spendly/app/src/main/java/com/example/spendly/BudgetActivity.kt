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
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
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

        // Apply entrance animations first
        applyAnimations()

        // Then load content with subtle animations
        binding.root.postDelayed({
            setupMonthlyBudget()
            setupCategoryBudgets()
            setupNotifications()
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
        // Animate the main budget card with a slide up and subtle bounce
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        slideUp.interpolator = OvershootInterpolator(0.8f)
        binding.cardMonthlyBudget.startAnimation(slideUp)

        // Animate the headers with a fade in
        binding.categoryBudgetsHeader.alpha = 0f
        binding.categoryBudgetsHeader.animate()
            .alpha(1f)
            .setDuration(600)
            .setStartDelay(400)
            .start()

        // Staggered animation for cards
        binding.notificationCard.alpha = 0f
        binding.notificationCard.translationY = 100f
        binding.notificationCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(600)
            .start()
    }

    private fun setupMonthlyBudget() {
        val currencySymbol = prefsManager.getCurrencySymbol()
        val monthlyBudget = prefsManager.getMonthlyBudget()

        // IMPORTANT: First, explicitly set visibility of all elements
        // This ensures proper state management regardless of previous state
        if (monthlyBudget > 0) {
            // BUDGET IS SET
            // Show the budget info layout and hide the empty state
            binding.layoutBudgetSet.visibility = View.VISIBLE
            binding.layoutNoBudget.visibility = View.GONE

            // Explicitly show Edit and Delete buttons
            binding.btnEditBudget.visibility = View.VISIBLE
            binding.btnDeleteBudget.visibility = View.VISIBLE

            // Set "Set Budget Now" button properties for when it becomes visible
            binding.btnCreateBudget.text = "Set Budget Now"
            binding.btnCreateBudget.setIconResource(R.drawable.ic_add)

            // Display budget information
            binding.tvMonthlyBudget.text = CurrencyFormatter.formatAmount(monthlyBudget, currencySymbol)

            // Calculate progress
            val totalExpense = transactionRepository.getTotalExpenseForCurrentMonth()
            val percentSpent = if (monthlyBudget > 0) {
                ((totalExpense / monthlyBudget) * 100).toInt().coerceIn(0, 100)
            } else 0

            // Animate the progress bar
            binding.progressBudget.progress = 0
            binding.progressBudget.animate().alpha(1f).setDuration(500).start()
            animateProgressBar(percentSpent)

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
            // NO BUDGET SET
            // Show the empty state layout and hide the budget info
            binding.layoutBudgetSet.visibility = View.GONE
            binding.layoutNoBudget.visibility = View.VISIBLE

            // Explicitly hide Edit and Delete buttons
            binding.btnEditBudget.visibility = View.GONE
            binding.btnDeleteBudget.visibility = View.GONE

            // Enhance "Create Budget" button to be more prominent
            binding.btnCreateBudget.text = "Set Budget Now"
            binding.btnCreateBudget.setIconResource(R.drawable.ic_add)
            binding.btnCreateBudget.setBackgroundColor(getColor(R.color.primary))

            // Reset any progress values
            binding.progressBudget.progress = 0
        }

        // Set up button click handlers
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
                // Reset budget to ZERO
                prefsManager.setMonthlyBudget(0.0)

                // Show a fade out animation for the budget layout
                binding.layoutBudgetSet.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        // IMPORTANT: Explicitly hide buttons to ensure clean state
                        binding.btnEditBudget.visibility = View.GONE
                        binding.btnDeleteBudget.visibility = View.GONE

                        // Refresh the UI
                        setupMonthlyBudget()

                        // Fade in the no-budget layout
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

            // Fade in empty state
            binding.layoutEmptyCategoryBudgets.alpha = 0f
            binding.layoutEmptyCategoryBudgets.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

        } else {
            binding.rvCategoryBudgets.visibility = View.VISIBLE
            binding.layoutEmptyCategoryBudgets.visibility = View.GONE

            binding.rvCategoryBudgets.layoutManager = LinearLayoutManager(this)

            // Add nice item animations
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
                val intent = Intent(this, BudgetCheckService::class.java)
                intent.action = BudgetCheckService.ACTION_CHECK_BUDGET
                startService(intent)

                Snackbar.make(binding.root,
                    "Budget alerts enabled",
                    Snackbar.LENGTH_SHORT)
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            } else {
                Snackbar.make(binding.root,
                    "Budget alerts disabled",
                    Snackbar.LENGTH_SHORT)
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            }
        }

        // Daily reminders switch
        binding.switchDailyReminders.isChecked = prefsManager.shouldShowDailyReminders()
        binding.switchDailyReminders.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setShouldShowDailyReminders(isChecked)
            binding.btnSetReminderTime.isEnabled = isChecked

            // Animate the time button visibility
            if (isChecked) {
                binding.btnSetReminderTime.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            } else {
                binding.btnSetReminderTime.animate()
                    .alpha(0.5f)
                    .setDuration(300)
                    .start()
            }

            try {
                // Schedule or cancel daily reminders based on the switch
                if (isChecked) {
                    val intent = Intent(this, BudgetCheckService::class.java)
                    intent.action = BudgetCheckService.ACTION_SCHEDULE_DAILY_REMINDER
                    startService(intent)

                    Snackbar.make(binding.root,
                        "Daily reminders enabled",
                        Snackbar.LENGTH_SHORT)
                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                        .show()

                } else {
                    // Cancel scheduled reminder
                    val intent = Intent(this, BudgetCheckService::class.java)
                    intent.action = BudgetCheckService.ACTION_DAILY_REMINDER
                    val pendingIntent = PendingIntent.getService(
                        this,
                        1001,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.cancel(pendingIntent)

                    Snackbar.make(binding.root,
                        "Daily reminders disabled",
                        Snackbar.LENGTH_SHORT)
                        .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                        .show()
                }
            } catch (e: Exception) {
                Log.e("BudgetActivity", "Error managing reminder: ${e.message}")
            }
        }

        // Reminder time button
        binding.btnSetReminderTime.isEnabled = binding.switchDailyReminders.isChecked
        binding.btnSetReminderTime.alpha = if (binding.switchDailyReminders.isChecked) 1f else 0.5f
        binding.btnSetReminderTime.setOnClickListener {
            showTimePickerDialog()
        }
    }

    private fun showTimePickerDialog() {
        // Get current time from preferences or default to 8:00 PM
        val defaultHour = prefsManager.getReminderTimeHour()
        val defaultMinute = prefsManager.getReminderTimeMinute()

        val timePicker = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                // Save selected time
                prefsManager.setReminderTime(hourOfDay, minute)

                // Update reminder schedule
                val intent = Intent(this, BudgetCheckService::class.java)
                intent.action = BudgetCheckService.ACTION_SCHEDULE_DAILY_REMINDER
                startService(intent)

                // Format time for display
                val timeString = formatTime(hourOfDay, minute)
                Snackbar.make(binding.root,
                    "Daily reminder set for $timeString",
                    Snackbar.LENGTH_SHORT)
                    .setAnimationMode(Snackbar.ANIMATION_MODE_SLIDE)
                    .show()
            },
            defaultHour,
            defaultMinute,
            false
        )

        timePicker.show()
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val hourDisplay = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val minuteDisplay = if (minute < 10) "0$minute" else minute.toString()

        return "$hourDisplay:$minuteDisplay $amPm"
    }

    private fun showBudgetAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_budget, null)
        val etBudget = dialogView.findViewById<TextInputEditText>(R.id.etBudget)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)

        tvDialogTitle.text = "Create Monthly Budget"

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Add Budget") { _, _ -> // Changed button text to "Add Budget"
                try {
                    val newBudget = etBudget.text.toString().toDoubleOrNull() ?: 0.0

                    if (newBudget > 0) {
                        // Save the budget
                        prefsManager.setMonthlyBudget(newBudget)

                        // First fade out no-budget layout
                        binding.layoutNoBudget.animate()
                            .alpha(0f)
                            .setDuration(300)
                            .withEndAction {
                                // Refresh UI and fade in budget layout
                                setupMonthlyBudget()
                                binding.layoutBudgetSet.alpha = 0f
                                binding.layoutBudgetSet.animate()
                                    .alpha(1f)
                                    .setDuration(300)
                                    .start()

                                // Check budget status
                                startService(Intent(this, BudgetCheckService::class.java))

                                // Show success message
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

        // Pre-fill with current budget
        val currentBudget = prefsManager.getMonthlyBudget()
        etBudget.setText(currentBudget.toString())

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton("Save Changes") { _, _ -> // Changed button text to "Save Changes"
                try {
                    val newBudget = etBudget.text.toString().toDoubleOrNull() ?: 0.0

                    if (newBudget > 0) {
                        prefsManager.setMonthlyBudget(newBudget)

                        // Refresh with animation
                        binding.layoutBudgetSet.alpha = 0.5f
                        binding.layoutBudgetSet.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .withEndAction {
                                setupMonthlyBudget() // Refresh the UI
                            }
                            .start()

                        // Show success message
                        showSuccessSnackbar("Budget updated!")

                        // Check budget status
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

        // Get all expense categories
        val expenseCategories = getExpenseCategories()
        val categoryNames = expenseCategories.map { it.name }.toTypedArray()

        // Setup spinner with custom colored items
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

                        // Check if this is the first category budget
                        val isFirstCategoryBudget = binding.layoutEmptyCategoryBudgets.visibility == View.VISIBLE

                        if (isFirstCategoryBudget) {
                            // Fade out empty state
                            binding.layoutEmptyCategoryBudgets.animate()
                                .alpha(0f)
                                .setDuration(300)
                                .withEndAction {
                                    setupCategoryBudgets() // Refresh the UI

                                    // Fade in RecyclerView
                                    binding.rvCategoryBudgets.alpha = 0f
                                    binding.rvCategoryBudgets.animate()
                                        .alpha(1f)
                                        .setDuration(300)
                                        .start()
                                }
                                .start()
                        } else {
                            setupCategoryBudgets() // Just refresh
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
                    if (budget > 0) {
                        prefsManager.setCategoryBudget(category, budget)
                        setupCategoryBudgets() // Refresh the UI
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

                // Check if this was the last category budget
                val remainingBudgets = getCategoryBudgets().size - 1

                if (remainingBudgets == 0) {
                    // This was the last category budget
                    binding.rvCategoryBudgets.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            setupCategoryBudgets() // Will show empty state
                        }
                        .start()
                } else {
                    // Still have other budgets
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
            // Add exit animation
            finishWithAnimation()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun finishWithAnimation() {
        // Apply exit animations to cards
        binding.cardMonthlyBudget.animate()
            .alpha(0f)
            .translationY(-50f)
            .setDuration(300)
            .start()

        binding.categoryBudgetsHeader.animate()
            .alpha(0f)
            .setDuration(200)
            .setStartDelay(100)
            .start()

        binding.notificationCard.animate()
            .alpha(0f)
            .translationY(50f)
            .setDuration(300)
            .setStartDelay(200)
            .withEndAction {
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
            }
            .start()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishWithAnimation()
    }
}