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

        supportActionBar?.hide()

        val adapter = OnboardingAdapter(
            onboardingImages,
            onboardingTitles,
            onboardingDescriptions
        )
        binding.viewPager.adapter = adapter

        binding.viewPager.offscreenPageLimit = 1

        setupIndicators()
        updateIndicator(0)

        binding.viewPager.setPageTransformer { page, position ->
            val title = page.findViewById<TextView>(R.id.onboardingTitle)
            val desc = page.findViewById<TextView>(R.id.onboardingDesc)
            val image = page.findViewById<ImageView>(R.id.onboardingImage)

            when {
                position >= -0.1f && position <= 0.1f -> {
                    page.alpha = 1f
                    title?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in))
                    desc?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in))
                    image?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_in_slow))
                }

                position > 0.1f && position < 1f -> {
                    page.alpha = 1 - position
                    title?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_out))
                    desc?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_out))
                    image?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out))
                }

                position < -0.1f && position > -1f -> {
                    page.alpha = 1 + position
                    title?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_out))
                    desc?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_down_fade_out))
                    image?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.fade_out))
                }

                else -> {
                    page.alpha = 0f
                }
            }
        }

        binding.btnNext.setOnClickListener {
            if (binding.viewPager.currentItem < onboardingImages.size - 1) {
                binding.viewPager.currentItem++
            } else {
                finishOnboarding()
            }
        }

        binding.btnSkip.setOnClickListener {
            finishOnboarding()
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicator(position)

                if (position == onboardingImages.size - 1) {
                    binding.btnNext.text = "Get Started"
                    binding.btnSkip.visibility = View.INVISIBLE
                } else {
                    binding.btnNext.text = "Continue"
                    binding.btnSkip.visibility = View.VISIBLE
                }
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

                indicators[i]?.animate()
                    ?.scaleX(1.0f)
                    ?.scaleY(1.0f)
                    ?.setDuration(300)
                    ?.start()
            }
        }
    }

    private fun finishOnboarding() {
        prefsManager.setFirstLaunchComplete()

        val intent = Intent(this, MainPage::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}