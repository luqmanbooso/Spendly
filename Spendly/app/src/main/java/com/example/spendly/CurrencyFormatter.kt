package com.example.spendly

import java.text.NumberFormat
import java.util.*

object CurrencyFormatter {

    fun formatAmount(amount: Double, currencySymbol: String = "$"): String {
        val format = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        return "$currencySymbol${format.format(amount)}"
    }
}