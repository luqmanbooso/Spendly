package com.example.spendly

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

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
        toolbar.setNavigationOnClickListener { onBackPressed() }

        // Initialize views
        editName = findViewById(R.id.editName)
        editEmail = findViewById(R.id.editEmail)
        saveButton = findViewById(R.id.btnSave)

        // Load profile data
        loadProfileData()

        // Save button click listener
        saveButton.setOnClickListener {
            saveProfileData()
        }
    }

    private fun loadProfileData() {
        val sharedPrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        editName.setText(sharedPrefs.getString("name", ""))
        editEmail.setText(sharedPrefs.getString("email", ""))
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

        // Save data to SharedPreferences
        val sharedPrefs = getSharedPreferences("user_profile", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("name", name)
            putString("email", email)
            apply()
        }

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
        finish()
    }
}