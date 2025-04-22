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

    private val SPLASH_DURATION = 4000L // 4 seconds
    private val TEXT_FLIP_INTERVAL = 1500L // 1.5 seconds

    // Loading states
    private val loadingStates = arrayOf(
        "Preparing your experience",
        "Setting up your dashboard",
        "Loading financial tools",
        "Almost ready..."
    )
    private var currentLoadingState = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make splash screen full screen
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        setContentView(R.layout.activity_splash_screen)

        // Hide action bar
        supportActionBar?.hide()

        // Set up animations
        setupAnimations()

        // Set up dynamic loading text
        setupDynamicLoadingText()

        // Set up progress indicator
        setupProgressBar()

        // Navigate to next screen after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DURATION)
    }

    private fun setupAnimations() {
        // Reference to views
        val logoImage = findViewById<ImageView>(R.id.logoImage)
        val appTitle = findViewById<TextView>(R.id.appTitle)
        val appTagline = findViewById<TextView>(R.id.appTagline)
        val flipper = findViewById<ViewFlipper>(R.id.textFlipper)
        val loadingContainer = findViewById<TextView>(R.id.loadingText)

        // Load animations
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        val slideUpDelayed = AnimationUtils.loadAnimation(this, R.anim.slide_up_delayed)
        val fadeInDelayed = AnimationUtils.loadAnimation(this, R.anim.fade_in_delayed)

        // Apply animations
        logoImage.startAnimation(fadeIn)
        appTitle.startAnimation(slideUp)
        appTagline.startAnimation(slideUpDelayed)
        loadingContainer.startAnimation(fadeInDelayed)

        // Start the text flipper with a delay
        Handler(Looper.getMainLooper()).postDelayed({
            flipper.startFlipping()
            flipper.flipInterval = TEXT_FLIP_INTERVAL.toInt()
        }, 1000)
    }

    private fun setupDynamicLoadingText() {
        val loadingText = findViewById<TextView>(R.id.loadingText)

        // Update loading text every second
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                loadingText.text = loadingStates[currentLoadingState % loadingStates.size]
                currentLoadingState++

                // Continue updating if we're still within the splash duration
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

        // Animate the progress bar
        val handler = Handler(Looper.getMainLooper())
        val updateInterval = 80L // Update every 80ms for smooth animation
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
        // For now, just navigate to a placeholder activity
        // In a real implementation, you'd check login state and navigate accordingly
        val prefsManager = PrefsManager(this)
        val userManager = UserManager(this)

        // If the user isn't properly logged in (either missing is_logged_in flag or email)
        if (!userManager.isLoggedIn() || userManager.getCurrentUserEmail() == null) {
            // Clear any partial login state
            userManager.logout()

            val intent = if (prefsManager.isFirstLaunch()) {
                Intent(this, OnboardingActivity::class.java)
            } else {
                Intent(this, LoginActivity::class.java)
            }

            startActivity(intent)
        } else {
            // User is logged in, go to main screen
            startActivity(Intent(this, MainActivity::class.java))
        }

        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}