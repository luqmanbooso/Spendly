package com.example.spendly

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.spendly.R
import com.example.spendly.databinding.ItemCategoryBinding
import com.example.spendly.Category

class CategoryAdapter(
    private val context: Context,
    private val categories: List<Category>,
    private val onCategoryClick: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        val binding = holder.binding

        // Set category name
        binding.categoryName.text = category.name

        // Set category icon
        binding.categoryIcon.setImageResource(category.iconRes)

        // Set category colors
        val color = ContextCompat.getColor(context, category.colorRes)

        // Update selection state
        updateSelectionState(binding, category, color)

        // Click listener
        binding.categoryContainer.setOnClickListener {
            // If already selected, do nothing
            if (category.isSelected) return@setOnClickListener

            // Deselect all categories
            categories.forEach { it.isSelected = false }

            // Select this category
            category.isSelected = true

            // Play selection animation
            if (binding.categorySelectionAnim != null) {
                binding.categorySelectionAnim.visibility = View.VISIBLE
                binding.categorySelectionAnim.playAnimation()
            } else {
                // Fall back to simple animation if Lottie not available
                val bounceAnim = AnimationUtils.loadAnimation(context, R.anim.bounce_animation)
                binding.categoryIcon.startAnimation(bounceAnim)
            }

            // Update all views
            notifyDataSetChanged()

            // Call the click listener
            onCategoryClick(category)
        }
    }

    private fun updateSelectionState(binding: ItemCategoryBinding, category: Category, color: Int) {
        if (category.isSelected) {
            binding.categoryIconCard.setCardBackgroundColor(color)
            binding.categoryIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.white))
            binding.categoryName.setTextColor(color)
            binding.categoryContainer.setBackgroundResource(R.drawable.bg_selected_category)

            // Apply slight elevation
            binding.categoryIconCard.cardElevation = 4f
        } else {
            binding.categoryIconCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.background_light))
            binding.categoryIcon.imageTintList = ColorStateList.valueOf(color)
            binding.categoryName.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            binding.categoryContainer.background = null

            // No elevation
            binding.categoryIconCard.cardElevation = 0f
        }
    }
}