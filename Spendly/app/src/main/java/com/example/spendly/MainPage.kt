package com.example.spendly

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.spendly.databinding.ActivityMainPageBinding

class MainPage : AppCompatActivity() {

    private lateinit var binding: ActivityMainPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupAnimations()
        setupClickListeners()
    }

    private fun setupAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        val slideUpDelayed = AnimationUtils.loadAnimation(this, R.anim.slide_up_delayed)

        binding.ivAppLogo.startAnimation(fadeIn)
        binding.tvAppName.startAnimation(slideUp)
        binding.tvAppTagline.startAnimation(slideUpDelayed)
        binding.divider.startAnimation(slideUpDelayed)

        binding.tvWelcomeText.alpha = 0f
        binding.llFeatures.alpha = 0f
        binding.btnLogin.alpha = 0f
        binding.btnSignUp.alpha = 0f

        binding.tvWelcomeText.animate()
            .alpha(1f)
            .setStartDelay(500)
            .setDuration(500)
            .start()

        binding.llFeatures.animate()
            .alpha(1f)
            .setStartDelay(700)
            .setDuration(500)
            .start()

        binding.btnLogin.animate()
            .alpha(1f)
            .setStartDelay(900)
            .setDuration(500)
            .start()

        binding.btnSignUp.animate()
            .alpha(1f)
            .setStartDelay(1100)
            .setDuration(500)
            .start()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        binding.btnSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}