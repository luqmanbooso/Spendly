package com.example.spendly

/**
 * Data class for representing expense data by category for the analysis screen
 */
data class CategoryExpense(
    val category: String,
    val amount: Double,
    val percentage: Float = 0f
)