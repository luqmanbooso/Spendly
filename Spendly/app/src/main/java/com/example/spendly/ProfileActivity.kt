package com.example.spendly

import android.content.Context
import android.content.Intent
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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener { finishWithAnimation() }

        editName = findViewById(R.id.editName)
        editEmail = findViewById(R.id.editEmail)
        saveButton = findViewById(R.id.btnSave)

        val container = findViewById<View>(R.id.profileContainer)
        container.alpha = 0f
        container.animate()
            .alpha(1f)
            .setDuration(300)
            .start()

        loadProfileData()

        saveButton.setOnClickListener {
            saveProfileData()
        }
    }

    private fun loadProfileData() {
        val sharedPrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        
        editName.setText("")
        editEmail.setText("")

        val userManager = UserManager(this)
        val currentUserEmail = userManager.getCurrentUserEmail()
        val currentUserName = userManager.getCurrentUserName()

        if (!currentUserEmail.isNullOrEmpty() && !currentUserName.isNullOrEmpty()) {
            editName.setText(currentUserName)
            editEmail.setText(currentUserEmail)
            Log.d("ProfileActivity", "Loaded profile from UserManager: $currentUserName, $currentUserEmail")
        } else {
            val savedName = sharedPrefs.getString("name", null)
            val savedEmail = sharedPrefs.getString("email", null)
            
            if (!savedName.isNullOrEmpty() && !savedEmail.isNullOrEmpty()) {
                editName.setText(savedName)
                editEmail.setText(savedEmail)
                Log.d("ProfileActivity", "Loaded profile from shared prefs: $savedName, $savedEmail")
            }
        }
    }

    private fun getCurrentUserEmail(): String? {
        val userManager = UserManager(this)
        return userManager.getCurrentUserEmail()
    }

    private fun saveProfileData() {
        val name = editName.text.toString().trim()
        val email = editEmail.text.toString().trim()

        if (name.isEmpty()) {
            editName.error = "Name cannot be empty"
            return
        }

        if (email.isEmpty()) {
            editEmail.error = "Email cannot be empty"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.error = "Please enter a valid email address"
            return
        }

        try {
            val userPrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
            userPrefs.edit().apply {
                putString("name", name)
                putString("email", email)
                apply()
            }

            val userManager = UserManager(this)
            userManager.saveUserEmail(email)
            userManager.saveUserName(name)

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

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
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
                val resultIntent = Intent().apply {
                    putExtra("name", editName.text.toString().trim())
                    putExtra("email", editEmail.text.toString().trim())
                }
                setResult(RESULT_OK, resultIntent)
                
                finish()
                overridePendingTransition(R.anim.fade_in, R.anim.slide_down)
            }
            .start()
    }
}