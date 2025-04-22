package com.example.spendly

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.spendly.R
import com.example.spendly.CategoryAdapter
import com.example.spendly.databinding.ActivityAddTransactionBinding
import com.example.spendly.Category
import com.example.spendly.Transaction
import com.example.spendly.TransactionType
import com.example.spendly.TransactionRepository
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var repository: TransactionRepository
    private lateinit var categoryAdapter: CategoryAdapter

    private var selectedDate = System.currentTimeMillis()
    private var currentType = TransactionType.EXPENSE
    private var editingTransaction: Transaction? = null
    private var selectedCategory: Category? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = TransactionRepository(this)

        setupToolbar()
        setupTypeSwitch()
        setupDatePicker()
        setupCategorySelection()
        setupValidation()
        setupSaveButton()

        // Check if we're editing an existing transaction
        val transactionId = intent.getStringExtra("TRANSACTION_ID")
        if (transactionId != null) {
            loadTransaction(transactionId)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Add Transaction")
    }

    private fun setupTypeSwitch() {
        binding.tabLayoutType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        currentType = TransactionType.EXPENSE
                        binding.btnSave.text = "Save Expense"
                        loadCategories(TransactionType.EXPENSE)
                    }
                    1 -> {
                        currentType = TransactionType.INCOME
                        binding.btnSave.text = "Save Income"
                        loadCategories(TransactionType.INCOME)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupDatePicker() {
        updateDateDisplay()

        // Set up date picker dialog
        binding.tilDate.setEndIconOnClickListener {
            showDatePickerDialog()
        }

        binding.etDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = selectedDate

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedDate = calendar.timeInMillis
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        binding.etDate.setText(dateFormat.format(Date(selectedDate)))
    }

    private fun setupCategorySelection() {
        binding.rvCategories.layoutManager = GridLayoutManager(this, 4)
        loadCategories(TransactionType.EXPENSE) // Default to expense categories
    }

    private fun loadCategories(type: TransactionType) {
        val categories = if (type == TransactionType.EXPENSE) {
            getExpenseCategories()
        } else {
            getIncomeCategories()
        }

        // Reset selected category when changing type
        selectedCategory = null
        binding.tvSelectedCategory.visibility = View.GONE

        categoryAdapter = CategoryAdapter(this, categories) { category ->
            // Update selected category
            categories.forEach { it.isSelected = false }
            category.isSelected = true
            selectedCategory = category
            categoryAdapter.notifyDataSetChanged()

            // Show selected category pill
            binding.tvSelectedCategory.text = category.title
            binding.tvSelectedCategory.visibility = View.VISIBLE

            updateSaveButtonState()
        }

        binding.rvCategories.adapter = categoryAdapter
    }

    private fun getExpenseCategories(): List<Category> {
        return listOf(
            Category("food_id", "Food", "food", R.drawable.ic_category_food, R.color.category_food),
            Category("transport_id", "Transport", "transport", R.drawable.ic_category_transport, R.color.category_transport),
            Category("bills_id", "Bills", "bills", R.drawable.ic_category_bills, R.color.category_bills),
            Category("entertainment_id", "Entertainment", "entertainment", R.drawable.ic_category_entertainment, R.color.category_entertainment),
            Category("shopping_id", "Shopping", "shopping", R.drawable.ic_category_shopping, R.color.category_shopping),
            Category("health_id", "Health", "health", R.drawable.ic_category_health, R.color.category_health),
            Category("education_id", "Education", "education", R.drawable.ic_category_education, R.color.category_education),
            Category("other_id", "Other", "other", R.drawable.ic_category_other, R.color.category_other)
        )
    }

    private fun getIncomeCategories(): List<Category> {
        return listOf(
            Category("salary_id", "Salary", "salary", R.drawable.ic_category_salary, R.color.category_salary),
            Category("business_id", "Business", "business", R.drawable.ic_category_business, R.color.category_business),
            Category("investment_id", "Investment", "investment", R.drawable.ic_category_investment, R.color.category_investment),
            Category("other_id", "Other", "other", R.drawable.ic_category_other_income, R.color.category_other)
        )
    }

    private fun setupValidation() {
        binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateAmount()
                updateSaveButtonState()
            }
        })

        binding.etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateTitle()
                updateSaveButtonState()
            }
        })
    }

    private fun validateAmount(): Boolean {
        val amountText = binding.etAmount.text.toString()
        if (amountText.isBlank()) {
            binding.tvAmountError.visibility = View.VISIBLE
            binding.tvAmountError.text = "Please enter an amount"
            return false
        }

        try {
            val amount = amountText.toDouble()
            if (amount <= 0) {
                binding.tvAmountError.visibility = View.VISIBLE
                binding.tvAmountError.text = "Amount must be greater than zero"
                return false
            }
        } catch (e: NumberFormatException) {
            binding.tvAmountError.visibility = View.VISIBLE
            binding.tvAmountError.text = "Invalid amount"
            return false
        }

        binding.tvAmountError.visibility = View.GONE
        return true
    }

    private fun validateTitle(): Boolean {
        val titleText = binding.etTitle.text.toString()
        if (titleText.isBlank()) {
            binding.tilTitle.error = "Title is required"
            return false
        }

        binding.tilTitle.error = null
        return true
    }

    private fun updateSaveButtonState() {
        val isValid = binding.etTitle.text.toString().isNotBlank() &&
                binding.etAmount.text.toString().isNotBlank() &&
                selectedCategory != null &&
                validateAmount()

        binding.btnSave.isEnabled = isValid
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                saveTransaction()
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate title
        if (!validateTitle()) {
            isValid = false
        }

        // Validate amount
        if (!validateAmount()) {
            isValid = false
        }

        // Validate category
        if (selectedCategory == null) {
            Snackbar.make(binding.root, "Please select a category", Snackbar.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun saveTransaction() {
        try {
            val title = binding.etTitle.text.toString().trim()
            val amount = binding.etAmount.text.toString().toDouble()
            val category = selectedCategory?.name ?: ""

            // Set isIncome based on the current type
            val isIncome = currentType == TransactionType.INCOME

            val transaction = editingTransaction?.copy(
                title = title,
                amount = amount,
                category = category,
                date = selectedDate,
                type = currentType,
                isIncome = isIncome  // Add this line
            ) ?: Transaction(
                title = title,
                amount = amount,
                category = category,
                date = selectedDate,
                type = currentType,
                isIncome = isIncome  // Add this line
            )

            repository.saveTransaction(transaction)

            showSuccessMessage()
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_bottom)

        } catch (e: Exception) {
            Snackbar.make(binding.root, "Error saving transaction: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showSuccessMessage() {
        val actionName = if (editingTransaction != null) "updated" else "added"
        val typeName = if (currentType == TransactionType.EXPENSE) "Expense" else "Income"

        Snackbar.make(binding.root, "$typeName $actionName successfully", Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.success))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    private fun loadTransaction(transactionId: String) {
        val transaction = repository.getTransaction(transactionId)
        if (transaction != null) {
            editingTransaction = transaction

            // Set title
            supportActionBar?.setTitle("Edit Transaction")

            // Fill form fields
            binding.etTitle.setText(transaction.title)
            binding.etAmount.setText(transaction.amount.toString())
            selectedDate = transaction.date
            updateDateDisplay()

            // Set transaction type
            if (transaction.type == TransactionType.INCOME) {
                binding.tabLayoutType.getTabAt(1)?.select()
            } else {
                binding.tabLayoutType.getTabAt(0)?.select()
            }

            // Select category
            val categories = if (transaction.type == TransactionType.EXPENSE) {
                getExpenseCategories()
            } else {
                getIncomeCategories()
            }

            // Select the right category
            categories.forEach { category ->
                if (category.name.equals(transaction.category, ignoreCase = true)) {
                    category.isSelected = true
                    selectedCategory = category
                    binding.tvSelectedCategory.text = category.title
                    binding.tvSelectedCategory.visibility = View.VISIBLE
                }
            }

            // Update save button text
            binding.btnSave.text = "Update Transaction"
        }
    }

    private fun showDeleteConfirmation(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                repository.deleteTransaction(transaction.id)
                Snackbar.make(binding.root, "Transaction deleted", Snackbar.LENGTH_SHORT).show()
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.slide_out_bottom)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            overridePendingTransition(R.anim.fade_in, R.anim.slide_out_bottom)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.slide_out_bottom)
    }
}