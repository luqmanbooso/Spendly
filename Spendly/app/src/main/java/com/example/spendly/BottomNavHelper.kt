package com.example.spendly

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.spendly.AddTransactionActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

enum class NavSection {
    HOME, TRANSACTIONS, BUDGET, ANALYSIS, REPORTS
}

class BottomNavHelper(private val context: Context, private val rootView: View) {

    private val TAG = "BottomNavHelper"

    fun setupBottomNav(currentSection: NavSection) {
        try {
            // Find all navigation items
            val navHome = rootView.findViewById<View>(R.id.nav_home)
            val navTransactions = rootView.findViewById<View>(R.id.nav_transactions)
            val navBudget = rootView.findViewById<View>(R.id.nav_budget)
            val navAnalysis = rootView.findViewById<View>(R.id.nav_analysis)
            val fabAddTransaction = rootView.findViewById<FrameLayout>(R.id.nav_add_transaction)

            // Log if any views are null to help debugging
            if (navHome == null) Log.e(TAG, "navHome is null")
            if (navTransactions == null) Log.e(TAG, "navTransactions is null")
            if (navBudget == null) Log.e(TAG, "navBudget is null")
            if (navAnalysis == null) Log.e(TAG, "navAnalysis is null")
            if (fabAddTransaction == null) Log.e(TAG, "fabAddTransaction is null")

            // Safety check - at least make sure we don't crash
            if (navHome == null || navTransactions == null || navBudget == null ||
                navAnalysis == null || fabAddTransaction == null) {
                Log.e(TAG, "Some navigation views are missing. Layout may not be properly included.")
                return
            }

            // Reset all tabs to inactive state
            navHome.let { setNavItemState(it, false, "Home") }
            navTransactions.let { setNavItemState(it, false, "Transactions") }
            navBudget.let { setNavItemState(it, false, "Budget") }
            navAnalysis.let { setNavItemState(it, false, "Analysis") }

            // Set the active tab based on current section
            when (currentSection) {
                NavSection.HOME -> navHome.let { setNavItemState(it, true, "Home") }
                NavSection.TRANSACTIONS -> navTransactions.let { setNavItemState(it, true, "Transactions") }
                NavSection.BUDGET -> navBudget.let { setNavItemState(it, true, "Budget") }
                NavSection.ANALYSIS, NavSection.REPORTS -> navAnalysis.let { setNavItemState(it, true, "Analysis") }
            }

            // Set up click listeners
            if (currentSection != NavSection.HOME) {
                navHome.setOnClickListener {
                    navigateTo(MainActivity::class.java)
                }
            }

            if (currentSection != NavSection.TRANSACTIONS) {
                navTransactions.setOnClickListener {
                    navigateTo(TransactionActivity::class.java)
                }
            }

            if (currentSection != NavSection.BUDGET) {
                navBudget.setOnClickListener {
                    navigateTo(BudgetActivity::class.java)
                }
            }

            if (currentSection != NavSection.ANALYSIS && currentSection != NavSection.REPORTS) {
                navAnalysis.setOnClickListener {
                    navigateTo(AnalysisActivity::class.java)
                }
            }

            // Set up FAB
            fabAddTransaction.setOnClickListener {
                context.startActivity(Intent(context, AddTransactionActivity::class.java))
                (context as? Activity)?.overridePendingTransition(R.anim.slide_up, R.anim.fade_out)
            }
        } catch (e: Exception) {
            // Log any exceptions to help debugging
            Log.e(TAG, "Error setting up bottom nav: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setNavItemState(navItem: View, isActive: Boolean, labelText: String) {
        try {
            val icon = navItem.findViewById<ImageView>(getIconId(labelText))
            val text = navItem.findViewById<TextView>(getTextId(labelText))

            if (icon == null) {
                Log.e(TAG, "Icon not found for $labelText")
                return
            }
            if (text == null) {
                Log.e(TAG, "Text not found for $labelText")
                return
            }

            val colorId = if (isActive) R.color.primary else R.color.text_secondary
            val color = context.getColor(colorId)

            icon.setColorFilter(color)
            text.setTextColor(color)

            // Add animation if active
            if (isActive) {
                icon.animate()
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(200)
                    .withEndAction {
                        icon.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start()
                    }
                    .start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting nav item state for $labelText: ${e.message}")
        }
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
        try {
            val intent = Intent(context, activityClass)
            intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
            context.startActivity(intent)
            (context as? Activity)?.finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to ${activityClass.simpleName}: ${e.message}")
        }
    }
}