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

                transactions.add(
                    Transaction(
                        id = jsonObject.getString("id"),
                        title = jsonObject.getString("title"),
                        amount = jsonObject.getDouble("amount"),
                        category = jsonObject.getString("category"),
                        date = jsonObject.getLong("date"),
                        type = if (jsonObject.getString("type") == "INCOME")
                            TransactionType.INCOME else TransactionType.EXPENSE,
                        note = jsonObject.optString("note", "")
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
            jsonObject.put("note", transaction.note)

            jsonArray.put(jsonObject)
        }

        sharedPreferences.edit().putString(KEY_TRANSACTIONS, jsonArray.toString()).apply()
    }

    fun getRecentTransactions(count: Int): List<Transaction> {
        return getAllTransactions().take(count)
    }

    fun getTotalIncomeForCurrentMonth(): Double {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        return getAllTransactions()
            .filter { it.type == TransactionType.INCOME }
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
            .filter { it.type == TransactionType.EXPENSE }
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
            .filter { it.type == TransactionType.EXPENSE }
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

    companion object {
        private const val TRANSACTION_PREFS = "transaction_prefs"
        private const val KEY_TRANSACTIONS = "transactions"
    }
}