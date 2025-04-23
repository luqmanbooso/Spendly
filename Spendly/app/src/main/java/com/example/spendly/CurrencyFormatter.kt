package com.example.spendly

import java.text.NumberFormat
import java.util.*

object CurrencyFormatter {
    fun formatAmount(amount: Double, currencyCode: String): String {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)
        numberFormat.currency = Currency.getInstance(currencyCode)
        return numberFormat.format(amount)
    }

    fun formatAmountShort(amount: Double, currencyCode: String): String {
        val suffix = when {
            amount >= 1_000_000 -> {
                Pair(amount / 1_000_000, "M")
            }
            amount >= 1_000 -> {
                Pair(amount / 1_000, "K")
            }
            else -> {
                Pair(amount, "")
            }
        }

        val numberFormat = NumberFormat.getInstance().apply {
            maximumFractionDigits = 2
        }

        val currencySymbol = Currency.getInstance(currencyCode).symbol
        return currencySymbol + numberFormat.format(suffix.first) + suffix.second
    }
}