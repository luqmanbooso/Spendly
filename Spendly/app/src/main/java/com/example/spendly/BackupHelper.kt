package com.example.spendly

import android.content.Context
import android.widget.Toast
import com.opencsv.CSVWriter
import org.json.JSONObject
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

    /**
     * Creates a full backup of app data including preferences and transactions
     */
    fun backupUserData() {
        try {
            // Get shared preferences
            val settingsPrefs = context.getSharedPreferences("com.example.spendly_preferences", Context.MODE_PRIVATE)
            val transactionPrefs = context.getSharedPreferences("transactions", Context.MODE_PRIVATE)
            val budgetPrefs = context.getSharedPreferences("budgets", Context.MODE_PRIVATE)

            // Create JSON object to store all data
            val backupData = JSONObject()

            // Add settings
            val settingsData = JSONObject()
            for (entry in settingsPrefs.all.entries) {
                settingsData.put(entry.key, entry.value.toString())
            }
            backupData.put("settings", settingsData)

            // Add transactions
            val transactionsData = JSONObject()
            for (entry in transactionPrefs.all.entries) {
                transactionsData.put(entry.key, entry.value.toString())
            }
            backupData.put("transactions", transactionsData)

            // Add budgets
            val budgetsData = JSONObject()
            for (entry in budgetPrefs.all.entries) {
                budgetsData.put(entry.key, entry.value.toString())
            }
            backupData.put("budgets", budgetsData)

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

            Toast.makeText(context, "Backup created successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Error creating backup: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    /**
     * Restores app data from the most recent backup file
     */
    fun restoreUserData() {
        try {
            // Get backup directory
            val backupDir = File(context.filesDir, "backups")

            // List all backup files
            val fileList = backupDir.listFiles { file ->
                file.name.startsWith("spendly_backup_") && file.name.endsWith(".json")
            }

            if (fileList.isNullOrEmpty()) {
                Toast.makeText(context, "No backup files found", Toast.LENGTH_SHORT).show()
                return
            }

            // Get the most recent backup file
            val latestBackup = fileList.maxByOrNull { it.lastModified() }

            if (latestBackup == null) {
                Toast.makeText(context, "Error finding backup file", Toast.LENGTH_SHORT).show()
                return
            }

            // Read file
            val jsonString = FileInputStream(latestBackup).bufferedReader().use { it.readText() }
            val backupData = JSONObject(jsonString)

            // Restore settings
            val settingsPrefs = context.getSharedPreferences("com.example.spendly_preferences", Context.MODE_PRIVATE)
            val settingsData = backupData.getJSONObject("settings")
            val settingsEditor = settingsPrefs.edit()
            settingsData.keys().forEach { key ->
                settingsEditor.putString(key, settingsData.getString(key))
            }
            settingsEditor.apply()

            // Restore transactions
            val transactionPrefs = context.getSharedPreferences("transactions", Context.MODE_PRIVATE)
            val transactionsData = backupData.getJSONObject("transactions")
            val transactionEditor = transactionPrefs.edit()
            transactionsData.keys().forEach { key ->
                transactionEditor.putString(key, transactionsData.getString(key))
            }
            transactionEditor.apply()

            // Restore budgets
            val budgetPrefs = context.getSharedPreferences("budgets", Context.MODE_PRIVATE)
            val budgetsData = backupData.getJSONObject("budgets")
            val budgetEditor = budgetPrefs.edit()
            budgetsData.keys().forEach { key ->
                budgetEditor.putString(key, budgetsData.getString(key))
            }
            budgetEditor.apply()

            Toast.makeText(context, "Data restored successfully", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(context, "Error restoring backup: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
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
     * Gets all backup files
     */
    fun getBackupFiles(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
            return emptyList()
        }

        return backupDir.listFiles { file ->
            file.name.startsWith("spendly_backup_") && file.name.endsWith(".json")
        }?.toList() ?: emptyList()
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
}

// Add this method to your BackupHelper class
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

