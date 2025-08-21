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
import android.animation.ValueAnimator
import android.view.animation.AnimationUtils
import com.example.spendly.AddTransactionActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

enum class NavSection {
    HOME, TRANSACTIONS, BUDGET, ANALYSIS, REPORTS
}

class BottomNavHelper(private val context: Context, private val rootView: View) {

    private val TAG = "BottomNavHelper"

    fun setupBottomNav(currentSection: NavSection) {
        try {
            val navHome = rootView.findViewById<View>(R.id.nav_home)
            val navTransactions = rootView.findViewById<View>(R.id.nav_transactions)
            val navBudget = rootView.findViewById<View>(R.id.nav_budget)
            val navAnalysis = rootView.findViewById<View>(R.id.nav_analysis)
            val fabAddTransaction = rootView.findViewById<FrameLayout>(R.id.nav_add_transaction)

            if (navHome == null) Log.e(TAG, "navHome is null")
            if (navTransactions == null) Log.e(TAG, "navTransactions is null")
            if (navBudget == null) Log.e(TAG, "navBudget is null")
            if (navAnalysis == null) Log.e(TAG, "navAnalysis is null")
            if (fabAddTransaction == null) Log.e(TAG, "fabAddTransaction is null")

            if (navHome == null || navTransactions == null || navBudget == null ||
                navAnalysis == null || fabAddTransaction == null) {
                Log.e(TAG, "Some navigation views are missing. Layout may not be properly included.")
                return
            }

            navHome.let { setNavItemState(it, false, "Home") }
            navTransactions.let { setNavItemState(it, false, "Transactions") }
            navBudget.let { setNavItemState(it, false, "Budget") }
            navAnalysis.let { setNavItemState(it, false, "Analysis") }

            when (currentSection) {
                NavSection.HOME -> navHome.let { setNavItemState(it, true, "Home") }
                NavSection.TRANSACTIONS -> navTransactions.let { setNavItemState(it, true, "Transactions") }
                NavSection.BUDGET -> navBudget.let { setNavItemState(it, true, "Budget") }
                NavSection.ANALYSIS, NavSection.REPORTS -> navAnalysis.let { setNavItemState(it, true, "Analysis") }
            }

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

            fabAddTransaction.setOnClickListener {
                context.startActivity(Intent(context, AddTransactionActivity::class.java))
                (context as? Activity)?.overridePendingTransition(R.anim.slide_up, R.anim.fade_out)
            }
        } catch (e: Exception) {
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

            val startColor = if (isActive) context.getColor(R.color.text_secondary) else context.getColor(R.color.primary)
            val endColor = if (isActive) context.getColor(R.color.primary) else context.getColor(R.color.text_secondary)

            val colorAnimator = ValueAnimator.ofArgb(startColor, endColor)
            colorAnimator.duration = 200
            colorAnimator.addUpdateListener { animation ->
                val animatedColor = animation.animatedValue as Int
                icon.setColorFilter(animatedColor)
                text.setTextColor(animatedColor)
            }
            colorAnimator.start()

            if (isActive) {
                val bounceAnim = AnimationUtils.loadAnimation(context, R.anim.nav_bounce)
                icon.startAnimation(bounceAnim)
                text.startAnimation(bounceAnim)
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
            
            (context as? Activity)?.overridePendingTransition(
                R.anim.fab_scale_down,
                R.anim.fab_scale_up
            )
            (context as? Activity)?.finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to ${activityClass.simpleName}: ${e.message}")
        }
    }
}