package com.example.spendly

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import kotlin.math.max

class OnboardingPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        page.apply {
            val pageWidth = width

            when {
                position > 1 -> {
                    alpha = 0f
                }

                position > 0 -> {
                    translationX = -position * pageWidth

                    val scaleFactor = max(MIN_SCALE, 1 - position)
                    scaleX = scaleFactor
                    scaleY = scaleFactor

                    alpha = max(MIN_ALPHA, 1 - position)

                    translationZ = -position
                }

                position <= 0 -> {
                    translationX = 0f
                    scaleX = 1f
                    scaleY = 1f
                    alpha = 1f
                    translationZ = 0f

                    val title = page.findViewById<View>(R.id.onboardingTitle)
                    val desc = page.findViewById<View>(R.id.onboardingDesc)

                    title?.translationX = -position * 1.5f * page.width
                    desc?.translationX = -position * 2f * page.width
                    title?.alpha = 1 + position * 3
                    desc?.alpha = 1 + position * 3
                }

                else -> {
                    alpha = 0f
                }
            }
        }
    }

    companion object {
        private const val MIN_SCALE = 0.85f
        private const val MIN_ALPHA = 0.5f
    }
}