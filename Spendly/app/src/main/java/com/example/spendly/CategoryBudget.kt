package com.example.spendly

data class CategoryBudget(
    val category: String,
    val budget: Double,
    val spent: Double
) {
    val percentage: Int
        get() = if (budget > 0) (spent / budget * 100).toInt().coerceIn(0, 100) else 0

    val remaining: Double
        get() = (budget - spent).coerceAtLeast(0.0)
}