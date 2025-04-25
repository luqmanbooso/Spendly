package com.example.spendly

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class CategoryBudgetAdapter(
    private val context: Context,
    private val items: List<CategoryBudget>,
    private val currencySymbol: String,
    private val onClick: (CategoryBudget) -> Unit
) : RecyclerView.Adapter<CategoryBudgetAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_budget, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvCategory.text = item.category.capitalize()
        holder.tvBudget.text = CurrencyFormatter.formatAmount(item.budget, currencySymbol)
        holder.tvSpent.text = "${CurrencyFormatter.formatAmount(item.spent, currencySymbol)} spent"
        holder.tvRemaining.text = "${CurrencyFormatter.formatAmount(item.getRemaining(), currencySymbol)} left"

        val percentSpent = item.getPercentSpent()
        holder.progressBar.progress = percentSpent

        val colorRes = when {
            percentSpent >= 90 -> R.color.expense
            percentSpent >= 70 -> R.color.warning
            else -> R.color.success
        }
        holder.progressBar.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))

        setIconAndColor(holder, item.category)

        holder.itemView.setOnClickListener { onClick(item) }
    }

    private fun setIconAndColor(holder: ViewHolder, category: String) {
        val (iconRes, colorRes) = when (category.lowercase()) {
            "food" -> Pair(R.drawable.ic_category_food, R.color.category_food)
            "transport" -> Pair(R.drawable.ic_category_transport, R.color.category_transport)
            "bills" -> Pair(R.drawable.ic_category_bills, R.color.category_bills)
            "entertainment" -> Pair(R.drawable.ic_category_entertainment, R.color.category_entertainment)
            "shopping" -> Pair(R.drawable.ic_category_shopping, R.color.category_shopping)
            "health" -> Pair(R.drawable.ic_category_health, R.color.category_health)
            "education" -> Pair(R.drawable.ic_category_education, R.color.category_education)
            else -> Pair(R.drawable.ic_category_other, R.color.category_other)
        }

        holder.ivIcon.setImageResource(iconRes)
        holder.ivIcon.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, colorRes))
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
        val tvCategory: TextView = view.findViewById(R.id.tvCategoryName)
        val tvBudget: TextView = view.findViewById(R.id.tvBudget)
        val tvSpent: TextView = view.findViewById(R.id.tvSpent)
        val tvRemaining: TextView = view.findViewById(R.id.tvRemaining)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
    }
}

fun String.capitalize(): String {
    return if (isNotEmpty()) {
        this[0].uppercase() + substring(1)
    } else {
        this
    }
}