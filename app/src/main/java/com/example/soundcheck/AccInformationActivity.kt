package com.example.soundcheck

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.FirebaseDatabase

class AccInformationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AccountInfo"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acc_information)

        // -------- VIEW REFERENCES --------
        val btnBack       = findViewById<AppCompatImageButton>(R.id.btnBack)
        val usernameTxtInput = findViewById<TextInputLayout>(R.id.usernameTxtInput)
        val passwordTxtInput = findViewById<TextInputLayout>(R.id.passwordTxtInput)
        val etUsername    = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword    = findViewById<TextInputEditText>(R.id.etPassword)
        val btnSave       = findViewById<Button>(R.id.btnSaveSettings)

        btnBack.setOnClickListener { finish() }

        // -------- FIREBASE SETUP --------
        val database    = FirebaseDatabase.getInstance("https://soundcheck-db-default-rtdb.asia-southeast1.firebasedatabase.app")
        val accountRef  = database.getReference("account")

        // -------- LOAD CURRENT INFO FROM FIREBASE --------
        Log.d(TAG, "Loading account info...")

        accountRef.get()
            .addOnSuccessListener { snapshot ->
                val username = snapshot.child("username").getValue(String::class.java) ?: ""
                val password = snapshot.child("password").getValue(String::class.java) ?: ""

                Log.d(TAG, "Loaded → username=$username")

                // Set as current text so user sees existing values
                etUsername.setText(username)
                etPassword.setText(password)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load account info: ${e.message}")
                Toast.makeText(this, "Failed to load account info", Toast.LENGTH_SHORT).show()
            }

        // -------- SAVE TO FIREBASE --------
        btnSave.setOnClickListener {
            val newUsername = etUsername.text.toString().trim()
            val newPassword = etPassword.text.toString().trim()

            if (newUsername.isEmpty()) {
                usernameTxtInput.error = "Username cannot be empty"
                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {
                passwordTxtInput.error = "Password cannot be empty"
                return@setOnClickListener
            }

            // Clear errors
            usernameTxtInput.error = null
            passwordTxtInput.error = null

            val updates = mapOf(
                "username" to newUsername,
                "password" to newPassword
            )

            accountRef.updateChildren(updates)
                .addOnSuccessListener {
                    Log.d(TAG, "Account info saved → username=$newUsername")
                    Toast.makeText(this, "Account info saved!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save: ${e.message}")
                    Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
                }
        }

        // -------- EDIT ICON LISTENERS --------
        // Tapping the edit icon focuses the field
        usernameTxtInput.setEndIconOnClickListener {
            etUsername.requestFocus()
            etUsername.setSelection(etUsername.text?.length ?: 0)
        }

        passwordTxtInput.setEndIconOnClickListener {
            etPassword.requestFocus()
            etPassword.setSelection(etPassword.text?.length ?: 0)
        }
    }
}