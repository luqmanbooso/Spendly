package com.example.spendly

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class TransactionRepository(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences(TRANSACTION_PREFS, Context.MODE_PRIVATE)

    fun saveTransaction(transaction: Transaction) {
        val transactions = getAllTransactions().toMutableList()

        // Check if transaction already exists (for edit)
        val index = transactions.indexOfFirst { it.id == transaction.id }
        if (index >= 0) {
            // Update existing
            transactions[index] = transaction
        } else {
            // Add new
            transactions.add(transaction)
        }

        // Sort by date (newest first)
        transactions.sortByDescending { it.date }

        // Save to SharedPreferences
        saveTransactionsToPrefs(transactions)
    }

    fun deleteTransaction(id: String) {
        val transactions = getAllTransactions().toMutableList()
        transactions.removeIf { it.id == id }

        // Save updated list
        saveTransactionsToPrefs(transactions)
    }

    fun getTransaction(id: String): Transaction? {
        return getAllTransactions().find { it.id == id }
    }

    fun getAllTransactions(): List<Transaction> {
        val jsonString = sharedPreferences.getString(KEY_TRANSACTIONS, null) ?: return emptyList()

        return try {
            val jsonArray = JSONArray(jsonString)
            val transactions = mutableListOf<Transaction>()

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val transactionType = if (jsonObject.getString("type") == "INCOME")
                    TransactionType.INCOME else TransactionType.EXPENSE

                transactions.add(
                    Transaction(
                        id = jsonObject.getString("id"),
                        title = jsonObject.getString("title"),
                        amount = jsonObject.getDouble("amount"),
                        category = jsonObject.getString("category"),
                        date = jsonObject.getLong("date"),
                        isIncome = transactionType == TransactionType.INCOME,
                        type = transactionType
                    )
                )
            }

            transactions
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveTransactionsToPrefs(transactions: List<Transaction>) {
        val jsonArray = JSONArray()

        for (transaction in transactions) {
            val jsonObject = JSONObject()
            jsonObject.put("id", transaction.id)
            jsonObject.put("title", transaction.title)
            jsonObject.put("amount", transaction.amount)
            jsonObject.put("category", transaction.category)
            jsonObject.put("date", transaction.date)
            jsonObject.put("type", transaction.type.toString())
            jsonObject.put("isIncome", transaction.isIncome)

            jsonArray.put(jsonObject)
        }

        sharedPreferences.edit().putString(KEY_TRANSACTIONS, jsonArray.toString()).apply()
    }

    // NEW METHODS

    // Get transactions by category
    fun getTransactionsByCategory(category: String): List<Transaction> {
        return getAllTransactions()
            .filter { it.category.equals(category, ignoreCase = true) }
            .sortedByDescending { it.date }
    }

    // Get transactions before a specific date
    fun getTransactionsBeforeDate(date: Long): List<Transaction> {
        return getAllTransactions()
            .filter { it.date <= date }
            .sortedByDescending { it.date }
    }

    // Get transactions after a specific date
    fun getTransactionsAfterDate(date: Long): List<Transaction> {
        return getAllTransactions()
            .filter { it.date >= date }
            .sortedByDescending { it.date }
    }

    // Get transactions by type (income/expense)
    fun getTransactionsByType(isIncome: Boolean): List<Transaction> {
        return getAllTransactions()
            .filter { it.isIncome == isIncome }
            .sortedByDescending { it.date }
    }

    // Get transactions for a date range
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): List<Transaction> {
        return getAllTransactions()
            .filter { it.date in startDate..endDate }
            .sortedByDescending { it.date }
    }

    fun getRecentTransactions(count: Int): List<Transaction> {
        return getAllTransactions().take(count)
    }

    fun getTotalIncomeForCurrentMonth(): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return getAllTransactions()
            .filter { it.isIncome }
            .filter {
                val transactionDate = Calendar.getInstance().apply { timeInMillis = it.date }
                transactionDate.get(Calendar.MONTH) == currentMonth &&
                        transactionDate.get(Calendar.YEAR) == currentYear
            }
            .sumOf { it.amount }
    }

    fun getTotalExpenseForCurrentMonth(): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return getAllTransactions()
            .filter { !it.isIncome }
            .filter {
                val transactionDate = Calendar.getInstance().apply { timeInMillis = it.date }
                transactionDate.get(Calendar.MONTH) == currentMonth &&
                        transactionDate.get(Calendar.YEAR) == currentYear
            }
            .sumOf { it.amount }
    }

    // Add method to get expense by category for analysis
    fun getExpensesByCategory(): Map<String, Double> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val expensesByCategory = mutableMapOf<String, Double>()

        getAllTransactions()
            .filter { !it.isIncome }
            .filter {
                val transactionDate = Calendar.getInstance().apply { timeInMillis = it.date }
                transactionDate.get(Calendar.MONTH) == currentMonth &&
                        transactionDate.get(Calendar.YEAR) == currentYear
            }
            .forEach {
                val currentAmount = expensesByCategory[it.category] ?: 0.0
                expensesByCategory[it.category] = currentAmount + it.amount
            }

        return expensesByCategory
    }

    // Add new method that accepts date range parameters
    fun getExpensesByCategory(startDate: Long, endDate: Long): Map<String, Double> {
        val expensesByCategory = mutableMapOf<String, Double>()

        getAllTransactions()
            .filter { !it.isIncome }
            .filter { it.date in startDate..endDate }
            .forEach {
                val currentAmount = expensesByCategory[it.category] ?: 0.0
                expensesByCategory[it.category] = currentAmount + it.amount
            }

        return expensesByCategory
    }

    // For getting transactions within a date range
    fun getTransactionsForPeriod(startDate: Long, endDate: Long): List<Transaction> {
        return getAllTransactions()
            .filter { it.date in startDate..endDate }
    }

    // For chart data - weekly
    fun getWeeklyData(numberOfWeeks: Int): List<WeeklyData> {
        val calendar = Calendar.getInstance()
        val result = mutableListOf<WeeklyData>()

        // Start from current week and go back
        for (i in 0 until numberOfWeeks) {
            if (i > 0) {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
            }

            val endOfWeek = calendar.timeInMillis
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            val startOfWeek = calendar.timeInMillis

            // Find week label (e.g., "Week 1")
            val weekNumber = calendar.get(Calendar.WEEK_OF_MONTH)
            val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
            val weekLabel = "W$weekNumber-$monthName"

            // Calculate income and expense for this week
            var income = 0.0
            var expense = 0.0

            getTransactionsForPeriod(startOfWeek, endOfWeek).forEach { transaction ->
                if (transaction.isIncome) {
                    income += transaction.amount
                } else {
                    expense += transaction.amount
                }
            }

            result.add(WeeklyData(weekLabel, income, expense))
        }

        return result.asReversed()  // Return in chronological order
    }

    // For chart data - monthly
    fun getMonthlyData(numberOfMonths: Int): List<MonthlyData> {
        val calendar = Calendar.getInstance()
        val result = mutableListOf<MonthlyData>()

        for (i in 0 until numberOfMonths) {
            if (i > 0) {
                calendar.add(Calendar.MONTH, -1)
            }

            val month = calendar.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
                ?: "Unknown"
            val year = calendar.get(Calendar.YEAR)
            val monthLabel = "$month ${year.toString().substring(2)}"

            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfMonth = calendar.timeInMillis

            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val endOfMonth = calendar.timeInMillis

            // Calculate income and expense for this month
            var income = 0.0
            var expense = 0.0

            getTransactionsForPeriod(startOfMonth, endOfMonth).forEach { transaction ->
                if (transaction.isIncome) {
                    income += transaction.amount
                } else {
                    expense += transaction.amount
                }
            }

            result.add(MonthlyData(monthLabel, income, expense))

            // Reset to beginning of month for next iteration
            calendar.add(Calendar.MILLISECOND, 1)
        }

        return result.asReversed()  // Return in chronological order
    }

    fun getExpenseForCategory(category: String): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return getAllTransactions()
            .filter { !it.isIncome && it.category.equals(category, ignoreCase = true) }
            .filter {
                val transactionDate = Calendar.getInstance().apply { timeInMillis = it.date }
                transactionDate.get(Calendar.MONTH) == currentMonth &&
                        transactionDate.get(Calendar.YEAR) == currentYear
            }
            .sumOf { it.amount }
    }

    // For file backup/restore (internal storage)
    fun exportToJson(): String {
        val transactions = getAllTransactions()
        return sharedPreferences.getString(KEY_TRANSACTIONS, "[]") ?: "[]"
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            JSONArray(jsonString) // Validate JSON format
            sharedPreferences.edit().putString(KEY_TRANSACTIONS, jsonString).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Data classes for analysis
    data class WeeklyData(val week: String, val income: Double, val expense: Double)
    data class MonthlyData(val month: String, val income: Double, val expense: Double)

    companion object {
        private const val TRANSACTION_PREFS = "transaction_prefs"
        private const val KEY_TRANSACTIONS = "transactions"
    }
}