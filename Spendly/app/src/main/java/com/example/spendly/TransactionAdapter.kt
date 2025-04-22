package com.example.spendly

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.spendly.R
import com.example.spendly.databinding.ItemTransactionBinding
import com.example.spendly.Transaction
import com.example.spendly.TransactionType
import com.example.spendly.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val currencySymbol: String,
    private val onTransactionClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val binding = holder.binding
        val context = binding.root.context

        // Set title and category
        binding.tvTitle.text = transaction.title
        binding.tvCategory.text = transaction.category

        // Format date
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(transaction.date))
        binding.tvDate.text = formattedDate

        // Set amount with proper formatting and color
        val amountPrefix = if (transaction.type == TransactionType.EXPENSE) "- " else "+ "
        val amountColor = if (transaction.type == TransactionType.EXPENSE)
            context.getColor(R.color.expense) else context.getColor(R.color.success)

        binding.tvAmount.text = amountPrefix + CurrencyFormatter.formatAmount(
            transaction.amount, currencySymbol
        )
        binding.tvAmount.setTextColor(amountColor)

        // Set category icon background color
        val bgColor = if (transaction.type == TransactionType.EXPENSE)
            context.getColor(R.color.expense) else context.getColor(R.color.success)
        binding.imgCategory.backgroundTintList = ColorStateList.valueOf(bgColor)

        // Set category icon
        val iconResId = getCategoryIcon(transaction.category, transaction.type)
        binding.imgCategory.setImageResource(iconResId)

        // Set click listener
        binding.root.setOnClickListener {
            onTransactionClick(transaction)
        }
    }

    private fun getCategoryIcon(category: String, type: TransactionType): Int {
        return when {
            type == TransactionType.INCOME -> {
                when (category.lowercase()) {
                    "salary" -> R.drawable.ic_category_salary
                    "business" -> R.drawable.ic_category_business
                    "investment" -> R.drawable.ic_category_investment
                        else -> R.drawable.ic_category_other_income
                }
            }
            else -> {
                when (category.lowercase()) {
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
    }

    override fun getItemCount(): Int = transactions.size
}