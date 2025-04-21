package com.example.spendly

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.spendly.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefsManager: PrefsManager
    private lateinit var indicators: Array<ImageView?>

    // Data for onboarding screens
    private val onboardingImages = intArrayOf(
        R.drawable.onboarding_expense_tracker,
        R.drawable.onboarding_budget_management,
        R.drawable.onboarding_insights
    )

    private val onboardingTitles = arrayOf(
        "Track Your Expenses",
        "Budget Management",
        "Financial Insights"
    )

    private val onboardingDescriptions = arrayOf(
        "Keep track of your daily spending habits and categorize transactions effortlessly.",
        "Set monthly budgets for different categories and get notified when you're reaching your limits.",
        "Get a clear picture of your financial habits with detailed stats and visualizations."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PrefsManager(this)

        // Hide action bar
        supportActionBar?.hide()

        // Set up the adapter for ViewPager with stable IDs to prevent recycling issues
        val adapter = OnboardingAdapter(
            onboardingImages,
            onboardingTitles,
            onboardingDescriptions
        )
        binding.viewPager.adapter = adapter

        // Prevent content bleeding between pages
        binding.viewPager.offscreenPageLimit = 1

        // Setup custom indicators
        setupIndicators()
        updateIndicator(0)

        // Simple fix for visibility issues
        binding.viewPager.setPageTransformer { page, position ->
            // Find the views
            val title = page.findViewById<TextView>(R.id.onboardingTitle)
            val desc = page.findViewById<TextView>(R.id.onboardingDesc)

            when {
                // Current page (fully visible)
                position >= -0.1f && position <= 0.1f -> {
                    page.alpha = 1f
                    title?.alpha = 1f
                    desc?.alpha = 1f
                }

                // Adjacent pages (partially visible)
                position > 0.1f && position < 1f -> {
                    // Page to the right (next page)
                    page.alpha = 1 - position
                    title?.alpha = 0f
                    desc?.alpha = 0f
                }

                position < -0.1f && position > -1f -> {
                    // Page to the left (previous page)
                    page.alpha = 1 + position
                    title?.alpha = 0f
                    desc?.alpha = 0f
                }

                // Pages not visible
                else -> {
                    page.alpha = 0f
                }
            }
        }

        // Set up the Next button
        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem < onboardingImages.size - 1) {
                // If not on last page, go to next page
                binding.viewPager.currentItem++
            } else {
                // If on last page, finish onboarding
                finishOnboarding()
            }
        }

        // Set up the Skip button
        binding.btnSkip.setOnClickListener {
            finishOnboarding()
        }

        // Update button text when page changes
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicator(position)

                // Change button text on last page
                if (position == onboardingImages.size - 1) {
                    binding.btnNext.text = "Get Started"
                    binding.btnSkip.visibility = View.INVISIBLE
                } else {
                    binding.btnNext.text = "Continue"
                    binding.btnSkip.visibility = View.VISIBLE
                }

                // Apply button animation
                binding.btnNext.startAnimation(AnimationUtils.loadAnimation(
                    this@OnboardingActivity, R.anim.button_pulse))
            }
        })
    }

    private fun setupIndicators() {
        val indicatorsCount = onboardingImages.size
        indicators = arrayOfNulls(indicatorsCount)

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(12, 0, 12, 0)

        for (i in 0 until indicatorsCount) {
            indicators[i] = ImageView(this)
            indicators[i]?.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    R.drawable.indicator_inactive
                )
            )
            indicators[i]?.layoutParams = params
            binding.indicatorContainer.addView(indicators[i])
        }
    }

    private fun updateIndicator(position: Int) {
        for (i in indicators.indices) {
            if (i == position) {
                indicators[i]?.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.indicator_active
                    )
                )

                // Scale up with animation
                indicators[i]?.animate()
                    ?.scaleX(1.2f)
                    ?.scaleY(1.2f)
                    ?.setDuration(300)
                    ?.start()
            } else {
                indicators[i]?.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.indicator_inactive
                    )
                )

                // Reset scale with animation
                indicators[i]?.animate()
                    ?.scaleX(1.0f)
                    ?.scaleY(1.0f)
                    ?.setDuration(300)
                    ?.start()
            }
        }
    }

    private fun finishOnboarding() {
        // Mark first launch as completed
        prefsManager.setFirstLaunchComplete()

        // Navigate to login activity with animation
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}