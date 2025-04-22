package com.example.spendly

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.example.spendly.MainActivity
import com.example.spendly.R
//import com.example.spendly.AddTransactionActivity
//import com.example.spendly.AnalysisActivity
//import com.example.spendly.BudgetActivity
//import com.example.spendly.TransactionsActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

enum class NavSection {
    HOME, TRANSACTIONS, BUDGET, ANALYSIS
}

class BottomNavHelper(private val context: Context, private val rootView: View) {

    fun setupBottomNav(currentSection: NavSection) {
        // Get all nav items
        val navHome = rootView.findViewById<View>(R.id.nav_home)
        val navTransactions = rootView.findViewById<View>(R.id.nav_transactions)
        val navBudget = rootView.findViewById<View>(R.id.nav_budget)
        val navAnalysis = rootView.findViewById<View>(R.id.nav_analysis)
        val fabAddTransaction = rootView.findViewById<FloatingActionButton>(R.id.fab_add_transaction)

        // Reset all to inactive
        setNavItemState(navHome, false, "Home")
        setNavItemState(navTransactions, false, "Transactions")
        setNavItemState(navBudget, false, "Budget")
        setNavItemState(navAnalysis, false, "Analysis")

        // Set active based on current section
        when (currentSection) {
            NavSection.HOME -> setNavItemState(navHome, true, "Home")
            NavSection.TRANSACTIONS -> setNavItemState(navTransactions, true, "Transactions")
            NavSection.BUDGET -> setNavItemState(navBudget, true, "Budget")
            NavSection.ANALYSIS -> setNavItemState(navAnalysis, true, "Analysis")
        }

        // Set up click listeners
        if (currentSection != NavSection.HOME) {
            navHome.setOnClickListener {
                navigateTo(MainActivity::class.java)
            }
        }

        if (currentSection != NavSection.TRANSACTIONS) {
            navTransactions.setOnClickListener {
                navigateTo(TransactionsActivity::class.java)
            }
        }

        if (currentSection != NavSection.BUDGET) {
            navBudget.setOnClickListener {
                navigateTo(BudgetActivity::class.java)
            }
        }

        if (currentSection != NavSection.ANALYSIS) {
            navAnalysis.setOnClickListener {
                navigateTo(AnalysisActivity::class.java)
            }
        }

        // Set up FAB
        fabAddTransaction.setOnClickListener {
            context.startActivity(Intent(context, AddTransactionActivity::class.java))
        }
    }

    private fun setNavItemState(navItem: View, isActive: Boolean, labelText: String) {
        val icon = navItem.findViewById<ImageView>(getIconId(labelText))
        val text = navItem.findViewById<TextView>(getTextId(labelText))

        val colorId = if (isActive) R.color.primary else R.color.icon_inactive
        val textColorId = if (isActive) R.color.primary else R.color.text_secondary

        icon.setColorFilter(context.getColor(colorId))
        text.setTextColor(context.getColor(textColorId))
    }

    private fun getIconId(section: String): Int {
        return when (section) {
            "Home" -> R.id.icon_home
            "Transactions" -> R.id.icon_transactions
            "Budget" -> R.id.icon_budget
            "Analysis" -> R.id.icon_analysis
            else -> 0
        }
    }

    private fun getTextId(section: String): Int {
        return when (section) {
            "Home" -> R.id.text_home
            "Transactions" -> R.id.text_transactions
            "Budget" -> R.id.text_budget
            "Analysis" -> R.id.text_analysis
            else -> 0
        }
    }

    private fun navigateTo(activityClass: Class<*>) {
        val intent = Intent(context, activityClass)
        intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        context.startActivity(intent)
        (context as Activity).finish()
    }
}