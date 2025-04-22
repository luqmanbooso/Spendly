package com.example.spendly.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.spendly.R
import com.example.spendly.databinding.ItemCategoryExpenseBinding
import com.example.spendly.model.CategoryExpense
import com.example.spendly.utils.CurrencyFormatter

class CategoryExpenseAdapter(
    private val context: Context,
    private val categoryExpenses: List<CategoryExpense>,
    private val currencySymbol: String
) : RecyclerView.Adapter<CategoryExpenseAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCategoryExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = categoryExpenses.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categoryExpense = categoryExpenses[position]
        val binding = holder.binding

        // Set category name
        binding.categoryName.text = categoryExpense.category

        // Set amount
        binding.tvAmount.text = CurrencyFormatter.formatAmount(categoryExpense.amount, currencySymbol)

        // Set percentage
        binding.tvPercentage.text = "${categoryExpense.percentage.toInt()}%"

        // Set progress
        binding.progressCategory.progress = categoryExpense.percentage.toInt()

        // Set category icon and colors
        val colorResId = getCategoryColorResId(categoryExpense.category)
        val color = ContextCompat.getColor(context, colorResId)

        binding.categoryIconCard.setCardBackgroundColor(color)
        binding.progressCategory.progressTintList = ColorStateList.valueOf(color)
        binding.categoryIcon.setImageResource(getCategoryIconResId(categoryExpense.category))
    }

    private fun getCategoryColorResId(category: String): Int {
        return when (category.lowercase()) {
            "food" -> R.color.category_food
            "transport" -> R.color.category_transport
            "bills" -> R.color.category_bills
            "entertainment" -> R.color.category_entertainment
            "shopping" -> R.color.category_shopping
            "health" -> R.color.category_health
            "education" -> R.color.category_education
            else -> R.color.category_other
        }
    }

    private fun getCategoryIconResId(category: String): Int {
        return when (category.lowercase()) {
            "food" -> R.drawable.ic_category_food
            "transport" -> R.drawable.ic_category_transport
            "bills" -> R.drawable.ic_category_bills
            "entertainment" -> R.drawable.ic_category_entertainment
            "shopping" -> R.drawable.ic_category_shopping
            "health" -> R.drawable.ic_category_health
            "education" -> R.drawable.ic_category_education
            else -> R.drawable.ic_category_other
        }
    }
}