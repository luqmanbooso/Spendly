package com.example.spendly

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.UnderlineSpan
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.spendly.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var userManager: UserManager
    private var isValidEmail = false
    private var isValidPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userManager = UserManager(this)

        // Hide action bar
        supportActionBar?.hide()

        // Set up click listeners
        setupClickListeners()

        // Set up form validation
        setupFormValidation()

        // Apply animations
        applyEntryAnimations()

        // Format "Sign Up" text with underline
        val signUpText = SpannableString(binding.tvSignUp.text)
        signUpText.setSpan(UnderlineSpan(), 0, signUpText.length, 0)
        binding.tvSignUp.text = signUpText

        // Check if coming from registration success
        if (intent.getBooleanExtra("REGISTRATION_SUCCESS", false)) {
            showSuccessMessage("Registration successful! Please log in")

            // Pre-fill email if provided
            intent.getStringExtra("EMAIL")?.let {
                binding.etEmail.setText(it)
                binding.etPassword.requestFocus()
            }
        }
    }

    private fun setupClickListeners() {
        // Login button click
        binding.btnLogin.setOnClickListener {
            if (validateForm()) {
                attemptLogin()
            } else {
                showErrorAnimation()
            }
        }

        // Sign up text click
        binding.tvSignUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Forgot password click
        binding.tvForgotPassword.setOnClickListener {
            val email = binding.etEmail.text.toString()
            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                // Simulate password recovery process
                binding.btnLogin.isEnabled = false
                binding.progressBar.visibility = View.VISIBLE

                // Simulate network operation
                binding.root.postDelayed({
                    binding.progressBar.visibility = View.INVISIBLE
                    binding.btnLogin.isEnabled = true
                    showSuccessMessage("Password reset email sent to $email")
                }, 1500)
            } else {
                showErrorMessage("Please enter a valid email address first")
            }
        }
    }

    private fun setupFormValidation() {
        // Email validation
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateEmail()
                updateLoginButtonState()
            }
        })

        // Password validation
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword()
                updateLoginButtonState()
            }
        })
    }

    private fun validateEmail(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        isValidEmail = email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

        if (!isValidEmail && email.isNotEmpty()) {
            binding.tilEmail.error = "Please enter a valid email address"
        } else {
            binding.tilEmail.error = null
        }

        return isValidEmail
    }

    private fun validatePassword(): Boolean {
        val password = binding.etPassword.text.toString()
        isValidPassword = password.length >= 6

        if (!isValidPassword && password.isNotEmpty()) {
            binding.tilPassword.error = "Password must be at least 6 characters"
        } else {
            binding.tilPassword.error = null
        }

        return isValidPassword
    }

    private fun validateForm(): Boolean {
        return validateEmail() && validatePassword()
    }

    private fun updateLoginButtonState() {
        binding.btnLogin.isEnabled = binding.etEmail.text.toString().isNotEmpty() &&
                binding.etPassword.text.toString().isNotEmpty()
    }

    private fun attemptLogin() {
        val email = binding.etEmail.text.toString()
        val password = binding.etPassword.text.toString()

        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        // Simulate network operation
        binding.root.postDelayed({
            binding.progressBar.visibility = View.INVISIBLE
            binding.btnLogin.isEnabled = true

            if (userManager.verifyCredentials(email, password)) {
                // Login successful
                userManager.setLoggedIn(true)
                userManager.saveUserEmail(email)

                // Launch main activity
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            } else {
                // Login failed
                showErrorMessage("Invalid email or password")
                binding.tilPassword.error = "Invalid email or password"
                binding.etPassword.text = null
                binding.etPassword.requestFocus()
                showErrorAnimation()
            }
        }, 1000)
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.error))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    private fun showSuccessMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.success))
            .setTextColor(ContextCompat.getColor(this, R.color.white))
            .show()
    }

    private fun showErrorAnimation() {
        val shake = AnimationUtils.loadAnimation(this, R.anim.shake)
        binding.loginForm.startAnimation(shake)
    }

    private fun applyEntryAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        binding.appLogo.startAnimation(fadeIn)
        binding.tvWelcomeBack.startAnimation(slideUp)
        binding.loginForm.startAnimation(slideUp)

        // Staggered animation for form elements
        binding.tilEmail.alpha = 0f
        binding.tilPassword.alpha = 0f
        binding.btnLogin.alpha = 0f
        binding.tvForgotPassword.alpha = 0f
        binding.signupContainer.alpha = 0f

        binding.tilEmail.animate().alpha(1f).setStartDelay(300).setDuration(400).start()
        binding.tilPassword.animate().alpha(1f).setStartDelay(400).setDuration(400).start()
        binding.btnLogin.animate().alpha(1f).setStartDelay(500).setDuration(400).start()
        binding.tvForgotPassword.animate().alpha(1f).setStartDelay(600).setDuration(400).start()
        binding.signupContainer.animate().alpha(1f).setStartDelay(700).setDuration(400).start()
    }
}