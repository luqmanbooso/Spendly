package com.example.spendly

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes

data class Category(
    val id: String,
    val name: String,
    @DrawableRes val iconRes: Int,
    @ColorRes val colorRes: Int,
    var isSelected: Boolean = false
)