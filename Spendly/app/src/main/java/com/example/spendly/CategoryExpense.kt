package com.example.spendly


data class CategoryExpense(
    val category: String,
    val amount: Double,
    val percentage: Float = 0f
)