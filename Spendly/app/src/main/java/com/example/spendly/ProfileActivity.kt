package com.example.spendly

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager

class ProfileActivity : AppCompatActivity() {

    private lateinit var editName: EditText
    private lateinit var editEmail: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finishWithAnimation() }

        // Initialize views
        editName = findViewById(R.id.editName)
        editEmail = findViewById(R.id.editEmail)
        saveButton = findViewById(R.id.btnSave)

        // Apply entrance animation
        val container = findViewById<View>(R.id.profileContainer)
        container.alpha = 0f
        container.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        // Load profile data
        loadProfileData()

        // Save button click listener
        saveButton.setOnClickListener {
            saveProfileData()
        }
    }

    private fun loadProfileData() {
        val sharedPrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        val currentUserEmail = getCurrentUserEmail()

        if (!currentUserEmail.isNullOrEmpty()) {
            // User is logged in, use UserManager to get name
            val userManager = UserManager(this)
            val userName = userManager.getCurrentUserName() ?: ""

            editName.setText(userName)
            editEmail.setText(currentUserEmail)

            Log.d("ProfileActivity", "Loaded profile data: $userName, $currentUserEmail")
        } else {
            // Fall back to shared prefs
            editName.setText(sharedPrefs.getString("name", ""))
            editEmail.setText(sharedPrefs.getString("email", ""))

            Log.d("ProfileActivity", "Loaded profile from shared prefs")
        }
    }

    private fun getCurrentUserEmail(): String? {
        val userManager = UserManager(this)
        return userManager.getCurrentUserEmail()
    }

    private fun saveProfileData() {
        val name = editName.text.toString().trim()
        val email = editEmail.text.toString().trim()

        // Simple validation
        if (name.isEmpty()) {
            editName.error = "Name cannot be empty"
            return
        }

        if (email.isEmpty()) {
            editEmail.error = "Email cannot be empty"
            return
        }

        // Email format validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.error = "Please enter a valid email address"
            return
        }

        // Save data to SharedPreferences - both to user_profile and current user
        try {
            val userPrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
            userPrefs.edit().apply {
                putString("name", name)
                putString("email", email)
                apply()
            }

            // Update the current user info if logged in
            val userManager = UserManager(this)
            val currentUserEmail = userManager.getCurrentUserEmail()

            if (!currentUserEmail.isNullOrEmpty()) {
                // If email changed, update the logged in user
                if (email != currentUserEmail) {
                    userManager.saveUserEmail(email)
                }

                // We don't have a direct method to update userName in UserManager,
                // but next time it will load from user_profile
            }

            Log.d("ProfileActivity", "Profile updated: $name, $email")

            Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            finishWithAnimation()
        } catch (e: Exception) {
            Log.e("ProfileActivity", "Error saving profile data", e)
            Toast.makeText(this, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finishWithAnimation()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishWithAnimation()
    }

    private fun finishWithAnimation() {
        val container = findViewById<View>(R.id.profileContainer)
        container.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
            }
            .start()
    }
}