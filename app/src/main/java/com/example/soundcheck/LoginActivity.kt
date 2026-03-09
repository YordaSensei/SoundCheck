package com.example.soundcheck

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "Login"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // -------- VIEW REFERENCES --------
        val usernameInput = findViewById<TextInputLayout>(R.id.usernameInput)
        val passwordInput = findViewById<TextInputLayout>(R.id.passwordInput)
        val etUsername    = findViewById<TextInputEditText>(R.id.etUsername)
        val etPassword    = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin      = findViewById<Button>(R.id.btnLogin)
        val tvError       = findViewById<TextView>(R.id.tvError)

        // -------- FIREBASE --------
        val database   = FirebaseDatabase.getInstance("https://soundcheck-db-default-rtdb.asia-southeast1.firebasedatabase.app")
        val accountRef = database.getReference("account")

        // -------- LOGIN --------
        btnLogin.setOnClickListener {
            val inputUsername = etUsername.text.toString().trim()
            val inputPassword = etPassword.text.toString().trim()

            // Clear errors
            usernameInput.error = null
            passwordInput.error = null
            tvError.visibility  = View.GONE

            // Validate empty fields
            if (inputUsername.isEmpty()) {
                usernameInput.error = "Username is required"
                return@setOnClickListener
            }

            if (inputPassword.isEmpty()) {
                passwordInput.error = "Password is required"
                return@setOnClickListener
            }

            // Disable button while checking
            btnLogin.isEnabled = false
            btnLogin.text      = "Logging in..."

            accountRef.get()
                .addOnSuccessListener { snapshot ->
                    val savedUsername = snapshot.child("username").getValue(String::class.java) ?: ""
                    val savedPassword = snapshot.child("password").getValue(String::class.java) ?: ""

                    if (inputUsername == savedUsername && inputPassword == savedPassword) {
                        Log.d(TAG, "Login successful")
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        Log.d(TAG, "Login failed — wrong credentials")
                        tvError.text       = "Incorrect username or password"
                        tvError.visibility = View.VISIBLE
                        btnLogin.isEnabled = true
                        btnLogin.text      = "Login"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Login check failed: ${e.message}")
                    tvError.text       = "Connection error. Please try again."
                    tvError.visibility = View.VISIBLE
                    btnLogin.isEnabled = true
                    btnLogin.text      = "Login"
                }
        }
    }
}