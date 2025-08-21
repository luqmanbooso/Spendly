package com.example.spendly

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.progressindicator.LinearProgressIndicator

class SplashScreen : AppCompatActivity() {

    private val SPLASH_DURATION = 4000L
    private val TEXT_FLIP_INTERVAL = 1500L

    private val loadingStates = arrayOf(
        "Preparing your experience",
        "Setting up your dashboard",
        "Loading financial tools",
        "Almost ready..."
    )
    private var currentLoadingState = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_splash_screen)

        supportActionBar?.hide()

        setupAnimations()

        setupDynamicLoadingText()

        setupProgressBar()

        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DURATION)
    }

    private fun setupAnimations() {
        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val appTitle = findViewById<TextView>(R.id.appTitle)
        val appTagline = findViewById<TextView>(R.id.appTagline)
        val flipper = findViewById<ViewFlipper>(R.id.textFlipper)
        val loadingContainer = findViewById<TextView>(R.id.loadingText)

        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        val slideUpDelayed = AnimationUtils.loadAnimation(this, R.anim.slide_up_delayed)
        val fadeInDelayed = AnimationUtils.loadAnimation(this, R.anim.fade_in_delayed)

        logoImage.startAnimation(fadeIn)
        appTitle.startAnimation(slideUp)
        appTagline.startAnimation(slideUpDelayed)
        loadingContainer.startAnimation(fadeInDelayed)

        Handler(Looper.getMainLooper()).postDelayed({
            flipper.startFlipping()
            flipper.flipInterval = TEXT_FLIP_INTERVAL.toInt()
        }, 1000)
    }

    private fun setupDynamicLoadingText() {
        val loadingText = findViewById<TextView>(R.id.loadingText)

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                loadingText.text = loadingStates[currentLoadingState % loadingStates.size]
                currentLoadingState++

                if (currentLoadingState * 1000 < SPLASH_DURATION) {
                    handler.postDelayed(this, 1000)
                }
            }
        }, 1000)
    }

    private fun setupProgressBar() {
        val progressBar = findViewById<LinearProgressIndicator>(R.id.loadingProgress)
        progressBar.max = 100
        progressBar.progress = 0

        val handler = Handler(Looper.getMainLooper())
        val updateInterval = 80L
        val progressStep = 100 * updateInterval / SPLASH_DURATION

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (progressBar.progress < 100) {
                    progressBar.progress = (progressBar.progress + progressStep).toInt().coerceAtMost(100)
                    handler.postDelayed(this, updateInterval)
                }
            }
        }, updateInterval)
    }

    private fun navigateToNextScreen() {
        val prefsManager = PrefsManager(this)
        val userManager = UserManager(this)

        if (!userManager.isLoggedIn() || userManager.getCurrentUserEmail() == null) {
            userManager.logout()

            val intent = if (prefsManager.isFirstLaunch()) {
                Intent(this, OnboardingActivity::class.java)
            } else {
                Intent(this, MainPage::class.java)
            }

            startActivity(intent)
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }

        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}