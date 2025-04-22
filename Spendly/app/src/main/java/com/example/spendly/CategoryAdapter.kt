package com.example.spendly

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CategoryAdapter(
    private val context: Context,
    private val categories: List<Category>,
    private val onCategorySelected: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        holder.bind(category, context)

        holder.itemView.setOnClickListener {
            // Update selected state
            categories.forEach { it.isSelected = false }
            category.isSelected = true
            notifyDataSetChanged()

            onCategorySelected(category)
        }
    }

    override fun getItemCount() = categories.size

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.categoryIconCard)
        private val icon: ImageView = itemView.findViewById(R.id.categoryIcon)
        private val name: TextView = itemView.findViewById(R.id.categoryName)

        fun bind(category: Category, context: Context) {
            // Set icon and name
            icon.setImageResource(category.iconRes)
            name.text = category.title // Use title for display

            // Set color based on category
            val color = ContextCompat.getColor(context, category.colorRes)

            // Apply selection state
            if (category.isSelected) {
                cardView.setCardBackgroundColor(color)
                name.setTextColor(ContextCompat.getColor(context, R.color.primary))
                icon.setColorFilter(ContextCompat.getColor(context, R.color.white))
            } else {
                cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.background_light))
                name.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                icon.setColorFilter(color)
            }
        }
    }
}