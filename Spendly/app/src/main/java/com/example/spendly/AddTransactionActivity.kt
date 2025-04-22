package com.example.spendly.activities

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.spendly.R
import com.example.spendly.TransactionType
import com.example.spendly.adapter.CategoryAdapter
import com.example.spendly.data.PrefsManager
import com.example.spendly.data.TransactionRepository
import com.example.spendly.databinding.ActivityAddTransactionBinding
import com.example.spendly.model.Category
import com.example.spendly.model.Transaction
import com.example.spendly.model.TransactionType
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var prefsManager: PrefsManager
    private lateinit var transactionRepository: TransactionRepository
    private var selectedDate = Calendar.getInstance()
    private var editingTransaction: Transaction? = null
    private var transactionType = TransactionType.EXPENSE
    private var selectedCategory: Category? = null

    private val expenseCategories = listOf(
        Category("food", "Food", R.drawable.ic_category_food, R.color.category_food),
        Category("transport", "Transport", R.drawable.ic_category_transport, R.color.category_transport),
        Category("bills", "Bills", R.drawable.ic_category_bills, R.color.category_bills),
        Category("entertainment", "Entertainment", R.drawable.ic_category_entertainment, R.color.category_entertainment),
        Category("shopping", "Shopping", R.drawable.ic_category_shopping, R.color.category_shopping),
        Category("health", "Health", R.drawable.ic_category_health, R.color.category_health),
        Category("education", "Education", R.drawable.ic_category_education, R.color.category_education),
        Category("other", "Other", R.drawable.ic_category_other, R.color.category_other)
    )

    private val incomeCategories = listOf(
        Category("salary", "Salary", R.drawable.ic_category_salary, R.color.category_salary),
        Category("business", "Business", R.drawable.ic_category_business, R.color.category_business),
        Category("investment", "Investment", R.drawable.ic_category_investment, R.color.category_investment),
        Category("gift", "Gift", R.drawable.ic_category_gift, R.color.category_gift),
        Category("other", "Other", R.drawable.ic_category_other_income, R.color.category_other)
    )

    private var currentCategories = expenseCategories

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PrefsManager(this)
        transactionRepository = TransactionRepository(this)

        // Setup toolbar with back button
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Animate the entry
        animateEntry()

        // Check if we're editing an existing transaction
        val transactionId = intent.getStringExtra("TRANSACTION_ID")
        if (transactionId != null) {
            editingTransaction = transactionRepository.getTransaction(transactionId)
            supportActionBar?.title = "Edit Transaction"
            setupForEditing()
        }

        setupViews()
        setupListeners()
    }

    private fun animateEntry() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.appBarLayout.startAnimation(fadeIn)

        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.bottomAppBar.startAnimation(slideUp)
    }

    private fun setupViews() {
        // Set currency symbol
        binding.tvCurrencySymbol.text = prefsManager.getCurrencySymbol()

        // Setup tab layout
        binding.tabLayoutType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                transactionType = if (tab.position == 0) TransactionType.EXPENSE else TransactionType.INCOME
                setupCategoryGrid(animate = true)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Set default date to today
        updateDateDisplay()

        // Setup category grid for current transaction type
        setupCategoryGrid(animate = false)

        // Enable/disable save button based on form validation
        validateForm()
    }

    private fun setupForEditing() {
        editingTransaction?.let { transaction ->
            // Fill form fields with animation
            binding.etTitle.setText(transaction.title)
            animateEditText(binding.etTitle)

            binding.etAmount.setText(transaction.amount.toString())
            animateEditText(binding.etAmount)

            binding.etNote?.setText(transaction.note)
            binding.etNote?.let { animateEditText(it) }

            // Set transaction type
            transactionType = transaction.type
            if (transactionType == TransactionType.INCOME) {
                binding.tabLayoutType.getTabAt(1)?.select()
            } else {
                binding.tabLayoutType.getTabAt(0)?.select()
            }

            // Set date
            selectedDate.timeInMillis = transaction.date
            updateDateDisplay()

            // Set category
            currentCategories = if (transactionType == TransactionType.INCOME) incomeCategories else expenseCategories
            selectedCategory = currentCategories.find { it.name.equals(transaction.category, ignoreCase = true) }
            selectedCategory?.isSelected = true
            updateSelectedCategoryDisplay()
            setupCategoryGrid(animate = false)
        }
    }

    private fun animateEditText(view: View) {
        val animator = view.animate()
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
        animator.start()
    }

    private fun setupCategoryGrid(animate: Boolean = false) {
        currentCategories = if (transactionType == TransactionType.INCOME) incomeCategories else expenseCategories

        // Reset selection if switching between income/expense
        if (selectedCategory != null && !currentCategories.contains(selectedCategory)) {
            selectedCategory = null
            binding.tvSelectedCategory.visibility = View.GONE
        }

        val adapter = CategoryAdapter(this, currentCategories) { category ->
            selectedCategory = category
            updateSelectedCategoryDisplay()
            validateForm()
        }

        binding.rvCategories.layoutManager = GridLayoutManager(this, 4)
        binding.rvCategories.adapter = adapter

        if (animate) {
            val animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)
            binding.rvCategories.startAnimation(animation)
        }
    }

    private fun updateSelectedCategoryDisplay() {
        selectedCategory?.let { category ->
            binding.tvSelectedCategory.visibility = View.VISIBLE
            binding.tvSelectedCategory.text = category.name
            binding.tvSelectedCategory.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, category.colorRes)
            )

            // Animate the pill appearance
            binding.tvSelectedCategory.alpha = 0f
            binding.tvSelectedCategory.scaleX = 0.8f
            binding.tvSelectedCategory.scaleY = 0.8f
            binding.tvSelectedCategory.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private fun setupListeners() {
        // Date selection
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.tilDate.setEndIconOnClickListener { showDatePicker() }

        // Input validation
        binding.etTitle.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { validateForm() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { validateForm() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Add a subtle animation when amount changes
                if (!s.isNullOrEmpty() && s.length > 1) {
                    binding.etAmount.animate().scaleX(1.05f).scaleY(1.05f).setDuration(100)
                        .withEndAction {
                            binding.etAmount.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                        }.start()
                }
            }
        })

        // Save button
        binding.btnSave.setOnClickListener {
            if (validateForm()) {
                animateSaveButton()
                saveTransaction()
            }
        }
    }

    private fun animateSaveButton() {
        binding.btnSave.isEnabled = false
        binding.btnSave.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                binding.btnSave.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }.start()
    }

    private fun showDatePicker() {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH)
        val day = selectedDate.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedYear)
                    set(Calendar.MONTH, selectedMonth)
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                }
                updateDateDisplay()
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
        binding.etDate.setText(dateFormat.format(selectedDate.time))
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validate title
        if (binding.etTitle.text.toString().trim().isEmpty()) {
            binding.tilTitle.error = "Title cannot be empty"
            isValid = false
        } else {
            binding.tilTitle.error = null
        }

        // Validate amount
        val amountText = binding.etAmount.text.toString()
        if (amountText.isEmpty()) {
            binding.tvAmountError.visibility = View.VISIBLE
            binding.tvAmountError.text = "Amount cannot be empty"
            isValid = false
        } else {
            try {
                val amount = amountText.toDouble()
                if (amount <= 0) {
                    binding.tvAmountError.visibility = View.VISIBLE
                    binding.tvAmountError.text = "Amount must be greater than 0"
                    isValid = false
                } else {
                    binding.tvAmountError.visibility = View.GONE
                }
            } catch (e: NumberFormatException) {
                binding.tvAmountError.visibility = View.VISIBLE
                binding.tvAmountError.text = "Invalid amount format"
                isValid = false
            }
        }

        // Validate category
        if (selectedCategory == null) {
            isValid = false
        }

        // Enable/disable save button
        if (binding.btnSave.isEnabled != isValid) {
            binding.btnSave.isEnabled = isValid
            if (isValid) {
                binding.btnSave.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(150)
                    .withEndAction {
                        binding.btnSave.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                            .start()
                    }
                    .start()
            }
        }

        return isValid
    }

    private fun saveTransaction() {
        val title = binding.etTitle.text.toString().trim()
        val amount = binding.etAmount.text.toString().toDouble()
        val category = selectedCategory?.name ?: ""
        val note = binding.etNote?.text?.toString()?.trim() ?: ""

        val transaction = Transaction(
            id = editingTransaction?.id ?: UUID.randomUUID().toString(),
            title = title,
            amount = amount,
            category = category,
            date = selectedDate.timeInMillis,
            type = transactionType,
            note = note
        )

        // Save to repository
        transactionRepository.saveTransaction(transaction)

        // Show success animation and message
        showSuccessAndFinish()
    }

    private fun showSuccessAndFinish() {
        // Hide bottom bar
        binding.bottomAppBar.visibility = View.GONE

        // Show success toast
        Toast.makeText(
            this,
            if (editingTransaction == null) "Transaction added successfully" else "Transaction updated successfully",
            Toast.LENGTH_SHORT
        ).show()

        // Add a small delay before finishing the activity
        binding.root.postDelayed({
            finish()
            // Add exit animation
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_bottom)
        }, 500)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Add exit animation
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_right)
    }
}