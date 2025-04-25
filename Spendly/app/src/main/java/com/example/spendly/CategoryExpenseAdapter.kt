package com.example.spendly

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CategoryExpenseAdapter(
    private val context: Context,
    private val items: List<CategoryExpense>,
    private val currencySymbol: String
) : RecyclerView.Adapter<CategoryExpenseAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_expense, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvCategory.text = item.category
        holder.tvAmount.text = CurrencyFormatter.formatAmount(item.amount, currencySymbol)
        holder.tvPercentage.text = String.format("%.1f%%", item.percentage)

        holder.progressBar.progress = item.percentage.toInt()

        when (item.category.lowercase()) {
            "food" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_category_food)
                setItemColor(holder, R.color.category_food)
            }
            "transport" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_category_transport)
                setItemColor(holder, R.color.category_transport)
            }
            "bills" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_category_bills)
                setItemColor(holder, R.color.category_bills)
            }
            "entertainment" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_category_entertainment)
                setItemColor(holder, R.color.category_entertainment)
            }
            "shopping" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_category_shopping)
                setItemColor(holder, R.color.category_shopping)
            }
            "health" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_category_health)
                setItemColor(holder, R.color.category_health)
            }
            "education" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_category_education)
                setItemColor(holder, R.color.category_education)
            }
            else -> {
                holder.ivIcon.setImageResource(R.drawable.ic_category_other)
                setItemColor(holder, R.color.category_other)
            }
        }
    }

    private fun setItemColor(holder: ViewHolder, colorResId: Int) {
        val color = ContextCompat.getColor(context, colorResId)
        holder.progressBar.progressTintList = ContextCompat.getColorStateList(context, colorResId)
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.imgCategoryIcon)
        val tvCategory: TextView = view.findViewById(R.id.tvCategoryName)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvPercentage: TextView = view.findViewById(R.id.tvPercentage)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
    }
}