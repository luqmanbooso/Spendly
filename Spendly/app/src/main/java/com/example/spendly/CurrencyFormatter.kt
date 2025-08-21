package com.example.spendly

import java.text.NumberFormat
import java.util.*

object CurrencyFormatter {
    fun formatAmount(amount: Double, currencySymbol: String): String {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale.US)
        val currency = try {
            Currency.getInstance(currencySymbol)
        } catch (e: IllegalArgumentException) {
            Currency.getAvailableCurrencies().firstOrNull { 
                it.symbol == currencySymbol 
            } ?: Currency.getInstance("USD")
        }
        numberFormat.currency = currency
        return numberFormat.format(amount)
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

        val currency = try {
            Currency.getInstance(currencySymbol)
        } catch (e: IllegalArgumentException) {
            Currency.getAvailableCurrencies().firstOrNull { 
                it.symbol == currencySymbol 
            } ?: Currency.getInstance("USD")
        }

        return currency.symbol + numberFormat.format(suffix.first) + suffix.second
    }
}