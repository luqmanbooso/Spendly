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
    val date: Long = System.currentTimeMillis(),
    val type: TransactionType = TransactionType.EXPENSE,
    val note: String = ""
) : Parcelable

enum class TransactionType {
    INCOME, EXPENSE
}