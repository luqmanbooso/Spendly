package com.example.spendly

import android.content.Context
import android.util.Log
import android.widget.Toast
import org.json.JSONObject
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


class BackupHelper(private val context: Context) {

    private val prefsManager = PrefsManager(context)
    private val transactionRepository = TransactionRepository(context)

    fun backupUserData(): Boolean {
        try {
            val settingsPrefs = context.getSharedPreferences("com.example.spendly_preferences", Context.MODE_PRIVATE)
            val userPrefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE)

            val backupData = JSONObject()

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
                Log.d("BackupHelper", "Backed up transaction: ${transaction.title}")
            }
            backupData.put("transactions", transactionsArray)
            Log.d("BackupHelper", "Backed up ${transactions.size} transactions")

            val budgetData = JSONObject().apply {
                put("monthly_budget", prefsManager.getMonthlyBudget())
                put("currency_symbol", prefsManager.getCurrencySymbol())
                put("notify_budget_warning", prefsManager.shouldNotifyBudgetWarning())
                put("budget_warning_threshold", prefsManager.getBudgetWarningThreshold())
                put("show_daily_reminders", prefsManager.shouldShowDailyReminders())
                put("reminder_time", prefsManager.getReminderTime())
            }
            backupData.put("budget_settings", budgetData)

            val categoryBudgets = JSONObject()
            val categories = listOf("food", "transport", "bills", "entertainment", 
                "shopping", "health", "education", "other")
            for (category in categories) {
                val budget = prefsManager.getCategoryBudget(category)
                categoryBudgets.put(category, budget)
            }
            backupData.put("category_budgets", categoryBudgets)

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

            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.getDefault())
            val backupFileName = "spendly_backup_${dateFormat.format(Date())}.json"

            val backupDir = File(context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

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

    fun exportDataAsJson(): String {
        val jsonObject = JSONObject()

        try {
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

            return jsonObject.toString(2)
        } catch (e: Exception) {
            Log.e("BackupHelper", "Error exporting data: ${e.message}", e)
            throw e
        }
    }

    fun exportDataAsText(): String {
        val stringBuilder = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        try {
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

    fun restoreUserData(backupData: String): Boolean {
        return try {
            val jsonObject = JSONObject(backupData)

            if (jsonObject.has("settings")) {
                val settingsPrefs = context.getSharedPreferences("com.example.spendly_preferences", Context.MODE_PRIVATE)
                val settingsData = jsonObject.getJSONObject("settings")
                val settingsEditor = settingsPrefs.edit()

                settingsEditor.clear()

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

            if (jsonObject.has("transactions")) {
                val transactionsArray = jsonObject.getJSONArray("transactions")
                val transactionList = mutableListOf<Transaction>()

                transactionRepository.deleteAllTransactions()

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

                for (transaction in transactionList) {
                    transactionRepository.saveTransaction(transaction)
                }
                Log.d("BackupHelper", "Restored ${transactionList.size} transactions")
            }

            if (jsonObject.has("budget_settings")) {
                val budgetData = jsonObject.getJSONObject("budget_settings")
                val monthlyBudget = budgetData.optDouble("monthly_budget", 0.0)
                val currencySymbol = budgetData.optString("currency_symbol", "$")
                val notifyBudgetWarning = budgetData.optBoolean("notify_budget_warning", true)
                val budgetWarningThreshold = budgetData.optInt("budget_warning_threshold", 80)
                val showDailyReminders = budgetData.optBoolean("show_daily_reminders", true)
                val reminderTime = budgetData.optString("reminder_time", "20:00")

                prefsManager.setMonthlyBudget(monthlyBudget)
                prefsManager.setCurrencySymbol(currencySymbol)
                prefsManager.setShouldNotifyBudgetWarning(notifyBudgetWarning)
                prefsManager.setBudgetWarningThreshold(budgetWarningThreshold)
                prefsManager.setShouldShowDailyReminders(showDailyReminders)
                prefsManager.setReminderTime(reminderTime)

                Log.d("BackupHelper", "Budget settings restored: monthly_budget=$monthlyBudget, currency=$currencySymbol")
            }

            if (jsonObject.has("category_budgets")) {
                val categoryBudgets = jsonObject.getJSONObject("category_budgets")
                val keys = categoryBudgets.keys()
                while (keys.hasNext()) {
                    val category = keys.next()
                    val budget = categoryBudgets.getDouble(category)
                    prefsManager.setCategoryBudget(category, budget)
                    Log.d("BackupHelper", "Restored category budget: $category = $budget")
                }
            }

            if (jsonObject.has("user_profile")) {
                val userPrefs = context.getSharedPreferences("user_profile", Context.MODE_PRIVATE)
                val userData = jsonObject.getJSONObject("user_profile")
                val userEditor = userPrefs.edit()

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

    fun importFromJson(jsonString: String): Boolean {
        try {
            val backupData = JSONObject(jsonString)

            if (backupData.has("settings")) {
                val settingsPrefs = context.getSharedPreferences("com.example.spendly_preferences", Context.MODE_PRIVATE)
                val settingsData = backupData.getJSONObject("settings")
                val settingsEditor = settingsPrefs.edit()

                settingsEditor.clear()

                val keys = settingsData.keys()
                val booleanKeys = listOf("budget_alerts", "daily_reminder")

                while (keys.hasNext()) {
                    val key = keys.next()
                    val valueStr = settingsData.getString(key)

                    if (booleanKeys.contains(key)) {
                        val boolValue = valueStr.equals("true", ignoreCase = true)
                        settingsEditor.putBoolean(key, boolValue)
                        Log.d("BackupHelper", "Restored boolean preference: $key = $boolValue")
                    } else {
                        settingsEditor.putString(key, valueStr)
                    }
                }
                settingsEditor.apply()
            }

            if (backupData.has("transactions")) {
                val transactionPrefs = context.getSharedPreferences("transactions", Context.MODE_PRIVATE)
                val transactionsData = backupData.getJSONObject("transactions")
                val transactionEditor = transactionPrefs.edit()

                transactionEditor.clear()

                val keys = transactionsData.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    transactionEditor.putString(key, transactionsData.getString(key))
                }
                transactionEditor.apply()
            }

            if (backupData.has("budgets")) {
                val budgetPrefs = context.getSharedPreferences("spendly_prefs", Context.MODE_PRIVATE)
                val budgetsData = backupData.getJSONObject("budgets")
                val budgetEditor = budgetPrefs.edit()

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