package com.example.spendly

data class CategoryBudget(
    val category: String,
    val budget: Double,
    val spent: Double
) {
    fun getPercentSpent(): Int {
        return if (budget > 0) ((spent / budget) * 100).toInt().coerceIn(0, 100) else 0
    }

    fun getRemaining(): Double {
        return (budget - spent).coerceAtLeast(0.0)
    }
}