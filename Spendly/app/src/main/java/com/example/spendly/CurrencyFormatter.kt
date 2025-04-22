package com.example.spendly

import java.text.NumberFormat
import java.util.*

object CurrencyFormatter {
    fun formatAmount(amount: Double, currencySymbol: String): String {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)
        numberFormat.currency = Currency.getInstance("USD")
        val formatted = numberFormat.format(amount)
        return formatted.replace("$", currencySymbol)
    }

    fun formatAmountShort(amount: Double, currencySymbol: String): String {
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

        return currencySymbol + numberFormat.format(suffix.first) + suffix.second
    }
}