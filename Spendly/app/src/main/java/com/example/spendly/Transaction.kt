package com.example.spendly

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val isIncome : Boolean,
    val date: Long = System.currentTimeMillis(),
    val type: TransactionType = TransactionType.EXPENSE,
    val currencyCode: String = getDefaultCurrencyCode()
) : Parcelable {
    companion object {
        private fun getDefaultCurrencyCode(): String {
            return "USD" // Default fallback
        }
    }
}

enum class TransactionType {
    INCOME, EXPENSE
}