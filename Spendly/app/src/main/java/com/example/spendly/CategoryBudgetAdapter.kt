package com.example.spendly

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.spendly.R
import com.example.spendly.CategoryBudget
import com.example.spendly.databinding.ItemCategoryBudgetBinding
import com.example.spendly.CurrencyFormatter

class CategoryBudgetAdapter(
    private val context: Context,
    private val categoryBudgets: List<CategoryBudget>,
    private val currencySymbol: String,
    private val onEditClick: (CategoryBudget) -> Unit
) : RecyclerView.Adapter<CategoryBudgetAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemCategoryBudgetBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryBudgetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = categoryBudgets.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categoryBudget = categoryBudgets[position]
        val binding = holder.binding

        // Apply entrance animation
        val animation = AnimationUtils.loadAnimation(context, R.anim.fade_slide_in)
        holder.itemView.startAnimation(animation)

        // Set category name
        binding.tvCategoryName.text = categoryBudget.category

        // Set budget amounts
        val formattedBudget = CurrencyFormatter.formatAmount(categoryBudget.budget, currencySymbol)
        binding.tvBudgetAmount.text = formattedBudget

        val formattedSpent = CurrencyFormatter.formatAmount(categoryBudget.spent, currencySymbol)
        binding.tvBudgetSpent.text = "$formattedSpent spent"

        // Set remaining and percentage
        binding.tvBudgetPercentage.text = "${categoryBudget.percentage}% used"

        val formattedRemaining = CurrencyFormatter.formatAmount(categoryBudget.remaining, currencySymbol)
        binding.tvRemaining.text = "$formattedRemaining left"

        // Set progress bar
        binding.progressCategoryBudget.progress = categoryBudget.percentage

        // Set color based on usage
        val colorResId = when {
            categoryBudget.percentage >= 100 -> R.color.expense
            categoryBudget.percentage >= 80 -> R.color.warning
            else -> R.color.success
        }
        val color = ContextCompat.getColor(context, colorResId)
        binding.progressCategoryBudget.progressTintList = ColorStateList.valueOf(color)

        // Set category icon and background
        val categoryIconResId = getCategoryIconResId(categoryBudget.category)
        binding.categoryIcon.setImageResource(categoryIconResId)

        val categoryColor = getCategoryColorResId(categoryBudget.category)
        binding.categoryIconCard.setCardBackgroundColor(ContextCompat.getColor(context, categoryColor))

        // Set edit click listener
        binding.btnEdit.setOnClickListener {
            // Add animation when clicked
            val clickAnim = AnimationUtils.loadAnimation(context, R.anim.click_animation)
            binding.btnEdit.startAnimation(clickAnim)

            onEditClick(categoryBudget)
        }

        // Set card click listener
        binding.root.setOnClickListener { onEditClick(categoryBudget) }
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

    // Clear all animations when recycled
    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.clearAnimation()
    }
}