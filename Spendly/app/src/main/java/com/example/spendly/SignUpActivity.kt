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
import com.example.spendly.databinding.ActivitySignUpBinding
import com.google.android.material.snackbar.Snackbar

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignUpBinding
    private lateinit var userManager: UserManager
    private var isValidName = false
    private var isValidEmail = false
    private var isValidPassword = false
    private var isValidConfirmPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userManager = UserManager(this)

        // Hide action bar
        supportActionBar?.hide()

        // Set up click listeners
        setupClickListeners()

        // Set up form validation
        setupFormValidation()

        // Format "Log In" text with underline
        val loginText = SpannableString(binding.tvLogin.text)
        loginText.setSpan(UnderlineSpan(), 0, loginText.length, 0)
        binding.tvLogin.text = loginText

        // Apply animations
        applyEntryAnimations()
    }

    private fun setupClickListeners() {
        // Sign up button click
        binding.btnSignUp.setOnClickListener {
            if (validateForm()) {
                attemptRegistration()
            } else {
                showErrorAnimation()
            }
        }

        // Login text click
        binding.tvLogin.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun setupFormValidation() {
        // Name validation
        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateName()
                updateSignUpButtonState()
            }
        })

        // Email validation
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateEmail()
                updateSignUpButtonState()
            }
        })

        // Password validation
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword()
                // Validate confirm password as well since the base password changed
                if (binding.etConfirmPassword.text.toString().isNotEmpty()) {
                    validateConfirmPassword()
                }
                updateSignUpButtonState()
            }
        })

        // Confirm password validation
        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateConfirmPassword()
                updateSignUpButtonState()
            }
        })
    }

    private fun validateName(): Boolean {
        val name = binding.etName.text.toString().trim()
        isValidName = name.length >= 3

        if (!isValidName && name.isNotEmpty()) {
            binding.tilName.error = "Name must be at least 3 characters"
        } else {
            binding.tilName.error = null
        }

        return isValidName
    }

    private fun validateEmail(): Boolean {
        val email = binding.etEmail.text.toString().trim()
        isValidEmail = email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

        if (!isValidEmail && email.isNotEmpty()) {
            binding.tilEmail.error = "Please enter a valid email address"
        } else if (userManager.isEmailTaken(email)) {
            isValidEmail = false
            binding.tilEmail.error = "This email is already registered"
        } else {
            binding.tilEmail.error = null
        }

        return isValidEmail
    }

    private fun validatePassword(): Boolean {
        val password = binding.etPassword.text.toString()

        // Password criteria: at least 8 characters, 1 uppercase, 1 digit
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasDigit = password.any { it.isDigit() }

        isValidPassword = hasMinLength && hasUppercase && hasDigit

        if (!isValidPassword && password.isNotEmpty()) {
            var errorMsg = "Password must have:"
            if (!hasMinLength) errorMsg += "\n• At least 8 characters"
            if (!hasUppercase) errorMsg += "\n• At least 1 uppercase letter"
            if (!hasDigit) errorMsg += "\n• At least 1 digit"

            binding.tilPassword.error = errorMsg
        } else {
            binding.tilPassword.error = null
        }

        return isValidPassword
    }

    private fun validateConfirmPassword(): Boolean {
        val password = binding.etPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        isValidConfirmPassword = confirmPassword.isNotEmpty() && confirmPassword == password

        if (!isValidConfirmPassword && confirmPassword.isNotEmpty()) {
            binding.tilConfirmPassword.error = "Passwords do not match"
        } else {
            binding.tilConfirmPassword.error = null
        }

        return isValidConfirmPassword
    }

    private fun validateForm(): Boolean {
        // Validate all fields
        val isNameValid = validateName()
        val isEmailValid = validateEmail()
        val isPasswordValid = validatePassword()
        val isConfirmPasswordValid = validateConfirmPassword()

        return isNameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid
    }

    private fun updateSignUpButtonState() {
        binding.btnSignUp.isEnabled = binding.etName.text.toString().isNotEmpty() &&
                binding.etEmail.text.toString().isNotEmpty() &&
                binding.etPassword.text.toString().isNotEmpty() &&
                binding.etConfirmPassword.text.toString().isNotEmpty()
    }

    private fun attemptRegistration() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSignUp.isEnabled = false

        // Simulate network operation
        binding.root.postDelayed({
            binding.progressBar.visibility = View.INVISIBLE
            binding.btnSignUp.isEnabled = true

            if (userManager.registerUser(name, email, password)) {
                // Registration successful
                // Navigate back to login screen with success message
                val intent = Intent(this, LoginActivity::class.java)
                intent.putExtra("REGISTRATION_SUCCESS", true)
                intent.putExtra("EMAIL", email)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            } else {
                // Registration failed
                showErrorMessage("Registration failed. Please try again.")
                showErrorAnimation()
            }
        }, 1500)
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
        binding.signupForm.startAnimation(shake)
    }

    private fun applyEntryAnimations() {
        val slideRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right)

        binding.signupForm.startAnimation(slideRight)

        // Staggered animation for form elements
        binding.tvCreateAccount.alpha = 0f
        binding.tilName.alpha = 0f
        binding.tilEmail.alpha = 0f
        binding.tilPassword.alpha = 0f
        binding.tilConfirmPassword.alpha = 0f
        binding.btnSignUp.alpha = 0f
        binding.loginContainer.alpha = 0f

        binding.tvCreateAccount.animate().alpha(1f).setStartDelay(100).setDuration(400).start()
        binding.tilName.animate().alpha(1f).setStartDelay(200).setDuration(400).start()
        binding.tilEmail.animate().alpha(1f).setStartDelay(300).setDuration(400).start()
        binding.tilPassword.animate().alpha(1f).setStartDelay(400).setDuration(400).start()
        binding.tilConfirmPassword.animate().alpha(1f).setStartDelay(500).setDuration(400).start()
        binding.btnSignUp.animate().alpha(1f).setStartDelay(600).setDuration(400).start()
        binding.loginContainer.animate().alpha(1f).setStartDelay(700).setDuration(400).start()
    }
}