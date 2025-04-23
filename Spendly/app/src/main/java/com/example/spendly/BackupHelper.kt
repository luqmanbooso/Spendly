package com.example.spendly

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.opencsv.CSVWriter
import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class for backup, restore and export functionality
 */
class BackupHelper(private val context: Context) {

    private val prefs = context.getSharedPreferences("com.example.spendly_preferences", Context.MODE_PRIVATE)
    private val transactionRepository = TransactionRepository(context)

    /**
     * Creates a full backup of app data including preferences and transactions
     */
    fun backupUserData(): Boolean {
        try {
            // Get shared preferences
            val settingsPrefs = context.getSharedPreferences("com.example.spendly_preferences", Context.MODE_PRIVATE)
            val budgetPrefs = context.getSharedPreferences("budgets", Context.MODE_PRIVATE)
            val userPrefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE)
            val transactionPrefs = context.getSharedPreferences("transactions", Context.MODE_PRIVATE)

            // Create JSON object to store all data
            val backupData = JSONObject()

            // Add settings
            val settingsData = JSONObject()
            for (entry in settingsPrefs.all.entries) {
                when (entry.value) {
                    is Boolean -> settingsData.put(entry.key, entry.value as Boolean)
                    is String -> settingsData.put(entry.key, entry.value as String)
                    is Int -> settingsData.put(entry.key, entry.value as Int)
                    is Float -> settingsData.put(entry.key, entry.value as Float)
                    is Long -> settingsData.put(entry.key, entry.value as Long)
                    else -> settingsData.put(entry.key, entry.value.toString())
                }
            }
            backupData.put("settings", settingsData)

            // Add transactions
            val transactions = transactionRepository.getAllTransactions()
            val transactionsArray = JSONArray()
            for (transaction in transactions) {
                val transactionObj = JSONObject().apply {
                    put("id", transaction.id)
                    put("title", transaction.title)
                    put("amount", transaction.amount)
                    put("category", transaction.category)
                    put("date", transaction.date)
                    put("type", transaction.type.toString())
                    put("isIncome", transaction.isIncome)
                }
                transactionsArray.put(transactionObj)
            }
            backupData.put("transactions", transactionsArray)

            // Add budgets
            val budgetsData = JSONObject()
            for (entry in budgetPrefs.all.entries) {
                when (entry.value) {
                    is Boolean -> budgetsData.put(entry.key, entry.value as Boolean)
                    is String -> budgetsData.put(entry.key, entry.value as String)
                    is Int -> budgetsData.put(entry.key, entry.value as Int)
                    is Float -> budgetsData.put(entry.key, entry.value as Float)
                    is Long -> budgetsData.put(entry.key, entry.value as Long)
                    else -> budgetsData.put(entry.key, entry.value.toString())
                }
            }
            backupData.put("budgets", budgetsData)

            // Add user profile
            val userData = JSONObject()
            for (entry in userPrefs.all.entries) {
                when (entry.value) {
                    is Boolean -> userData.put(entry.key, entry.value as Boolean)
                    is String -> userData.put(entry.key, entry.value as String)
                    is Int -> userData.put(entry.key, entry.value as Int)
                    is Float -> userData.put(entry.key, entry.value as Float)
                    is Long -> userData.put(entry.key, entry.value as Long)
                    else -> userData.put(entry.key, entry.value.toString())
                }
            }
            backupData.put("user_profile", userData)

            // Create filename with date
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
            val backupFileName = "spendly_backup_${dateFormat.format(Date())}.json"

            // Create a directory for backups if it doesn't exist
            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Write to file
            val file = File(backupDir, backupFileName)
            FileOutputStream(file).use {
                it.write(backupData.toString().toByteArray())
            }

            Log.d("BackupHelper", "Backup created successfully at: ${file.absolutePath}")
            return true

        } catch (e: Exception) {
            Log.e("BackupHelper", "Error creating backup: ${e.message}", e)
            return false
        }
    }

    /**
     * Exports transactions and budgets as JSON
     */
    fun exportDataAsJson(): String {
        val jsonObject = JSONObject()

        try {
            // Add transactions
            val transactions = transactionRepository.getAllTransactions()
            val transactionsArray = JSONArray()
            for (transaction in transactions) {
                val transactionObj = JSONObject().apply {
                    put("id", transaction.id)
                    put("title", transaction.title)
                    put("amount", transaction.amount)
                    put("category", transaction.category)
                    put("date", transaction.date)
                    put("type", transaction.type.toString())
                    put("isIncome", transaction.isIncome)
                }
                transactionsArray.put(transactionObj)
            }
            jsonObject.put("transactions", transactionsArray)

            // Add budgets
            val budgetPrefs = context.getSharedPreferences("budgets", Context.MODE_PRIVATE)
            val budgetsData = JSONObject()
            for (entry in budgetPrefs.all.entries) {
                when (entry.value) {
                    is Boolean -> budgetsData.put(entry.key, entry.value as Boolean)
                    is String -> budgetsData.put(entry.key, entry.value as String)
                    is Int -> budgetsData.put(entry.key, entry.value as Int)
                    is Float -> budgetsData.put(entry.key, entry.value as Float)
                    is Long -> budgetsData.put(entry.key, entry.value as Long)
                    else -> budgetsData.put(entry.key, entry.value.toString())
                }
            }
            jsonObject.put("budgets", budgetsData)

            return jsonObject.toString(2) // Pretty print with 2 spaces indentation
        } catch (e: Exception) {
            Log.e("BackupHelper", "Error exporting data: ${e.message}", e)
            throw e
        }
    }

    /**
     * Exports transactions and budgets as text
     */
    fun exportDataAsText(): String {
        val stringBuilder = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        try {
            // Add transactions
            stringBuilder.append("=== TRANSACTIONS ===\n\n")
            val transactions = transactionRepository.getAllTransactions()
            for (transaction in transactions) {
                stringBuilder.append("ID: ${transaction.id}\n")
                stringBuilder.append("Title: ${transaction.title}\n")
                stringBuilder.append("Amount: ${transaction.amount}\n")
                stringBuilder.append("Category: ${transaction.category}\n")
                stringBuilder.append("Date: ${dateFormat.format(Date(transaction.date))}\n")
                stringBuilder.append("Type: ${transaction.type}\n")
                stringBuilder.append("Is Income: ${transaction.isIncome}\n")
                stringBuilder.append("-------------------\n")
            }

            // Add budgets
            stringBuilder.append("\n=== BUDGETS ===\n\n")
            val budgetPrefs = context.getSharedPreferences("budgets", Context.MODE_PRIVATE)
            for (entry in budgetPrefs.all.entries) {
                stringBuilder.append("${entry.key}: ${entry.value}\n")
            }

            return stringBuilder.toString()
        } catch (e: Exception) {
            Log.e("BackupHelper", "Error exporting data: ${e.message}", e)
            throw e
        }
    }

    /**
     * Parses text format backup data into JSON format
     */
    private fun parseTextBackup(textData: String): JSONObject {
        val jsonObject = JSONObject()
        val transactionsArray = JSONArray()
        val budgetsData = JSONObject()

        try {
            val lines = textData.lines()
            var currentSection = ""
            var currentTransaction: JSONObject? = null

            for (line in lines) {
                when {
                    line.startsWith("=== TRANSACTIONS ===") -> {
                        currentSection = "transactions"
                    }
                    line.startsWith("=== BUDGETS ===") -> {
                        currentSection = "budgets"
                    }
                    line.startsWith("-------------------") -> {
                        if (currentTransaction != null) {
                            transactionsArray.put(currentTransaction)
                            currentTransaction = null
                        }
                    }
                    else -> {
                        when (currentSection) {
                            "transactions" -> {
                                if (line.startsWith("ID: ")) {
                                    currentTransaction = JSONObject()
                                    currentTransaction.put("id", line.substring(4))
                                } else if (currentTransaction != null) {
                                    when {
                                        line.startsWith("Title: ") -> currentTransaction.put("title", line.substring(7))
                                        line.startsWith("Amount: ") -> currentTransaction.put("amount", line.substring(8).toDouble())
                                        line.startsWith("Category: ") -> currentTransaction.put("category", line.substring(10))
                                        line.startsWith("Date: ") -> {
                                            val dateStr = line.substring(6)
                                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                            val date = dateFormat.parse(dateStr)
                                            currentTransaction.put("date", date?.time ?: System.currentTimeMillis())
                                        }
                                        line.startsWith("Type: ") -> {
                                            val type = line.substring(6)
                                            currentTransaction.put("type", type)
                                            currentTransaction.put("isIncome", type == "INCOME")
                                        }
                                    }
                                }
                            }
                            "budgets" -> {
                                if (line.contains(": ")) {
                                    val parts = line.split(": ")
                                    if (parts.size == 2) {
                                        val key = parts[0]
                                        val value = parts[1]
                                        try {
                                            when {
                                                value.equals("true", ignoreCase = true) -> budgetsData.put(key, true)
                                                value.equals("false", ignoreCase = true) -> budgetsData.put(key, false)
                                                value.contains(".") -> budgetsData.put(key, value.toDouble())
                                                value.toIntOrNull() != null -> budgetsData.put(key, value.toInt())
                                                else -> budgetsData.put(key, value)
                                            }
                                        } catch (e: Exception) {
                                            budgetsData.put(key, value)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add the last transaction if exists
            if (currentTransaction != null) {
                transactionsArray.put(currentTransaction)
            }

            jsonObject.put("transactions", transactionsArray)
            jsonObject.put("budgets", budgetsData)
        } catch (e: Exception) {
            Log.e("BackupHelper", "Error parsing text backup: ${e.message}", e)
            throw e
        }

        return jsonObject
    }

    /**
     * Restores user data from a backup file
     */
    fun restoreUserData(backupData: String): Boolean {
        return try {
            val jsonObject = JSONObject(backupData)

            // Restore settings
            if (jsonObject.has("settings")) {
            val settingsPrefs = context.getSharedPreferences("com.example.spendly_preferences", Context.MODE_PRIVATE)
                val settingsData = jsonObject.getJSONObject("settings")
            val settingsEditor = settingsPrefs.edit()

                // Clear existing settings
                settingsEditor.clear()

                // Process keys, handling boolean values specially
                val keys = settingsData.keys()
                val booleanKeys = listOf("budget_alerts", "daily_reminder")

                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = settingsData[key]
                    
                    when {
                        booleanKeys.contains(key) -> {
                            val boolValue = when (value) {
                                is Boolean -> value
                                is String -> value.equals("true", ignoreCase = true)
                                else -> false
                            }
                            settingsEditor.putBoolean(key, boolValue)
                            Log.d("BackupHelper", "Restored boolean setting: $key = $boolValue")
                        }
                        value is Boolean -> settingsEditor.putBoolean(key, value)
                        value is String -> settingsEditor.putString(key, value)
                        value is Int -> settingsEditor.putInt(key, value)
                        value is Long -> settingsEditor.putLong(key, value)
                        value is Float -> settingsEditor.putFloat(key, value)
                        value is Double -> settingsEditor.putFloat(key, value.toFloat())
                        else -> settingsEditor.putString(key, value.toString())
                    }
                }
                settingsEditor.apply()
                Log.d("BackupHelper", "Settings restored successfully")
            }

            // Restore transactions
            if (jsonObject.has("transactions")) {
                val transactionsArray = jsonObject.getJSONArray("transactions")
                val transactionList = mutableListOf<Transaction>()

                for (i in 0 until transactionsArray.length()) {
                    val transactionObj = transactionsArray.getJSONObject(i)
                    val transaction = Transaction(
                        id = transactionObj.optString("id", UUID.randomUUID().toString()),
                        title = transactionObj.optString("title", ""),
                        amount = transactionObj.optDouble("amount", 0.0),
                        category = transactionObj.optString("category", ""),
                        date = transactionObj.optLong("date", System.currentTimeMillis()),
                        isIncome = transactionObj.optBoolean("isIncome", false),
                        type = if (transactionObj.optString("type", "EXPENSE") == "INCOME") 
                            TransactionType.INCOME else TransactionType.EXPENSE
                    )
                    transactionList.add(transaction)
                    Log.d("BackupHelper", "Restored transaction: ${transaction.title}")
                }

                // Save each transaction individually
                for (transaction in transactionList) {
                    transactionRepository.saveTransaction(transaction)
                }
                Log.d("BackupHelper", "Restored ${transactionList.size} transactions")
            }

            // Restore budgets
            if (jsonObject.has("budgets")) {
            val budgetPrefs = context.getSharedPreferences("budgets", Context.MODE_PRIVATE)
                val budgetsData = jsonObject.getJSONObject("budgets")
            val budgetEditor = budgetPrefs.edit()

                // Clear existing data first
                budgetEditor.clear()

                val keys = budgetsData.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = budgetsData[key]
                    
                    when (value) {
                        is Boolean -> budgetEditor.putBoolean(key, value)
                        is String -> budgetEditor.putString(key, value)
                        is Int -> budgetEditor.putInt(key, value)
                        is Long -> budgetEditor.putLong(key, value)
                        is Float -> budgetEditor.putFloat(key, value)
                        is Double -> budgetEditor.putFloat(key, value.toFloat())
                        else -> budgetEditor.putString(key, value.toString())
                    }
                    Log.d("BackupHelper", "Restored budget: $key = $value")
                }
                budgetEditor.apply()
                Log.d("BackupHelper", "Budgets restored successfully")
            }

            // Restore user profile
            if (jsonObject.has("user_profile")) {
                val userPrefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE)
                val userData = jsonObject.getJSONObject("user_profile")
                val userEditor = userPrefs.edit()

                // Clear existing data first
                userEditor.clear()

                val keys = userData.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = userData[key]
                    
                    when (value) {
                        is Boolean -> userEditor.putBoolean(key, value)
                        is String -> userEditor.putString(key, value)
                        is Int -> userEditor.putInt(key, value)
                        is Long -> userEditor.putLong(key, value)
                        is Float -> userEditor.putFloat(key, value)
                        is Double -> userEditor.putFloat(key, value.toFloat())
                        else -> userEditor.putString(key, value.toString())
                    }
                    Log.d("BackupHelper", "Restored user profile: $key = $value")
                }
                userEditor.apply()
                Log.d("BackupHelper", "User profile restored successfully")
            }

            true
        } catch (e: Exception) {
            Log.e("BackupHelper", "Error restoring data: ${e.message}", e)
            false
        }
    }

    /**
     * Exports the given list of transactions as a CSV file
     */
    fun exportTransactionsAsCSV(transactions: List<Transaction>): File {
        // Create a directory for exports if it doesn't exist
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        // Create a new file with current timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "spendly_transactions_$timestamp.csv"
        val file = File(exportDir, fileName)

        file.bufferedWriter().use { writer ->
            // Write CSV header
            writer.write("ID,Date,Title,Amount,Category,Type,Note\n")

            // Write transaction data
            transactions.forEach { transaction ->
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(Date(transaction.date))

                val type = if (transaction.type == TransactionType.INCOME) "Income" else "Expense"

                // Format as CSV line, properly escaping fields
                val line = listOf(
                    transaction.id,
                    date,
                    escapeCSV(transaction.title),
                    transaction.amount.toString(),
                    escapeCSV(transaction.category),
                    type,
                ).joinToString(",")

                writer.write("$line\n")
            }
        }

        Toast.makeText(context, "Transactions exported to CSV successfully", Toast.LENGTH_SHORT).show()
        return file
    }

    /**
     * Properly escapes strings for CSV format
     */
    private fun escapeCSV(input: String): String {
        // If input contains commas, quotes, or newlines, wrap in quotes and escape any quotes
        return if (input.contains(",") || input.contains("\"") || input.contains("\n")) {
            "\"${input.replace("\"", "\"\"")}\""
        } else {
            input
        }
    }

    /**
     * Performs a full backup of all transactions
     */
    fun backupAllTransactions(repository: TransactionRepository): File {
        val transactions = repository.getAllTransactions()
        return exportTransactionsAsCSV(transactions)
    }

    /**
     * Gets all backup files sorted by date (newest first)
     */
    fun getBackupFiles(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
            return emptyList()
        }

        return backupDir.listFiles { file ->
            file.name.startsWith("spendly_backup_") && file.name.endsWith(".json")
        }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
    }

    /**
     * Gets all export files
     */
    fun getExportFiles(): List<File> {
        val exportDir = File(context.filesDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
            return emptyList()
        }

        return exportDir.listFiles { file ->
            file.name.startsWith("spendly_transactions_") && file.name.endsWith(".csv")
        }?.toList() ?: emptyList()
    }

    /**
     * Export transactions to CSV file
     */
    fun exportTransactionsToCSV(transactions: List<Transaction>, csvFile: File): Boolean {
        try {
            val fileWriter = FileWriter(csvFile)
            val csvWriter = CSVWriter(fileWriter)

            // Write header
            val header = arrayOf("Date", "Type", "Category", "Title", "Amount", "Notes")
            csvWriter.writeNext(header)

            // Write data
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            for (transaction in transactions) {
                val dateStr = dateFormat.format(Date(transaction.date))
                val typeStr = if (transaction.type == TransactionType.INCOME) "Income" else "Expense"
                val row = arrayOf(
                    dateStr,
                    typeStr,
                    transaction.category,
                    transaction.title,
                    transaction.amount.toString(),
                )
                csvWriter.writeNext(row)
            }

            csvWriter.close()
            fileWriter.close()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Import data from JSON string
     */
    fun importFromJson(jsonString: String): Boolean {
        try {
            val backupData = JSONObject(jsonString)

            // Restore settings - carefully handling type conversion
            if (backupData.has("settings")) {
                val settingsPrefs = context.getSharedPreferences("com.example.spendly_preferences", Context.MODE_PRIVATE)
                val settingsData = backupData.getJSONObject("settings")
                val settingsEditor = settingsPrefs.edit()

                // First, clear existing settings to avoid conflicts
                settingsEditor.clear()

                // Process keys, handling boolean values specially
                val keys = settingsData.keys()
                val booleanKeys = listOf("budget_alerts", "daily_reminder")

                while (keys.hasNext()) {
                    val key = keys.next()
                    val valueStr = settingsData.getString(key)

                    // Handle booleans specially
                    if (booleanKeys.contains(key)) {
                        val boolValue = valueStr.equals("true", ignoreCase = true)
                        settingsEditor.putBoolean(key, boolValue)
                        Log.d("BackupHelper", "Restored boolean preference: $key = $boolValue")
                    } else {
                        // For other types, just store as string
                        settingsEditor.putString(key, valueStr)
                    }
                }
                settingsEditor.apply()
            }

            // Restore transactions
            if (backupData.has("transactions")) {
                val transactionPrefs = context.getSharedPreferences("transactions", Context.MODE_PRIVATE)
                val transactionsData = backupData.getJSONObject("transactions")
                val transactionEditor = transactionPrefs.edit()

                // Clear existing data first
                transactionEditor.clear()

                val keys = transactionsData.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    transactionEditor.putString(key, transactionsData.getString(key))
                }
                transactionEditor.apply()
            }

            // Restore budgets
            if (backupData.has("budgets")) {
                val budgetPrefs = context.getSharedPreferences("spendly_prefs", Context.MODE_PRIVATE)
                val budgetsData = backupData.getJSONObject("budgets")
                val budgetEditor = budgetPrefs.edit()

                // Clear existing data first
                budgetEditor.clear()

                val keys = budgetsData.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    budgetEditor.putString(key, budgetsData.getString(key))
                }
                budgetEditor.apply()
            }

            return true
        } catch (e: Exception) {
            Log.e("BackupHelper", "Import error: ${e.message}", e)
            return false
        }
    }
}