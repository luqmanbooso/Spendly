package com.example.spendly

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val transactions: List<Transaction>,
    private val currencySymbol: String,
    private val onItemClick: (Transaction) -> Unit,
    private val onDeleteClick: ((Transaction) -> Unit)? = null
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction, currencySymbol)

        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }

        try {
            val deleteButton = holder.itemView.findViewById<ImageButton>(R.id.btnDelete)
            if (onDeleteClick != null) {
                deleteButton?.visibility = View.VISIBLE
                deleteButton?.setOnClickListener {
                    onDeleteClick.invoke(transaction)
                }
            } else {
                deleteButton?.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemCount() = transactions.size

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardCategoryIcon: CardView = itemView.findViewById(R.id.cardCategoryIcon)
        private val imgCategoryIcon: ImageView = itemView.findViewById(R.id.imgCategoryIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)

        fun bind(transaction: Transaction, currencySymbol: String) {
            tvTitle.text = transaction.title

            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val formattedDate = dateFormat.format(Date(transaction.date))

            tvDescription.text = "${transaction.category} • $formattedDate"

            val context = itemView.context
            val amountStr = if (!transaction.isIncome) {
                "-${CurrencyFormatter.formatAmount(transaction.amount, currencySymbol)}"
            } else {
                "+${CurrencyFormatter.formatAmount(transaction.amount, currencySymbol)}"
            }
            tvAmount.text = amountStr

            val amountColor = if (!transaction.isIncome) {
                ContextCompat.getColor(context, R.color.expense)
            } else {
                ContextCompat.getColor(context, R.color.success)
            }
            tvAmount.setTextColor(amountColor)

            val iconResId = getCategoryIconResource(transaction.category, transaction.isIncome)
            imgCategoryIcon.setImageResource(iconResId)

            val categoryColorResId = getCategoryColorResource(transaction.category, transaction.isIncome)
            cardCategoryIcon.setCardBackgroundColor(ContextCompat.getColor(context, categoryColorResId))
        }

        private fun getCategoryIconResource(category: String, isIncome: Boolean): Int {
            return when {
                isIncome -> {
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

        private fun getCategoryColorResource(category: String, isIncome: Boolean): Int {
            return when {
                isIncome -> {
                    when (category.lowercase()) {
                        "salary" -> R.color.category_salary
                        "business" -> R.color.category_business
                        "investment" -> R.color.category_investment
                        "gift" -> R.color.category_gift
                        else -> R.color.category_other
                    }
                }
                else -> {
                    when (category.lowercase()) {
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
            }
        }
    }
}