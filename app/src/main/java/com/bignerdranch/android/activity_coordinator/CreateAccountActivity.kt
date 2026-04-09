package com.bignerdranch.android.activity_coordinator

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class CreateAccountActivity : AppCompatActivity() {
    val db = Firebase.firestore
    val TAG = "CreateAccountActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        val etEmail = findViewById<EditText>(R.id.ca_email)
        val etPassword = findViewById<EditText>(R.id.ca_password)
        val etName = findViewById<EditText>(R.id.ca_name)
        val tvError = findViewById<TextView>(R.id.ca_error)
        val btnCreate = findViewById<Button>(R.id.ca_btn_create)
        val btnLogin = findViewById<TextView>(R.id.ca_btn_login)

        // Go back to login if they already have an account
        btnLogin.setOnClickListener {
            finish()
        }
        //on clicking create button do basici verification of info
        btnCreate.setOnClickListener {
            val email = etEmail.text.toString().trim().lowercase()
            val password = etPassword.text.toString().trim()
            val name = etName.text.toString().trim()

            // Basic validation, all fields must be filled
            if (email.isEmpty() || password.isEmpty() || name.isEmpty()) {
                tvError.text = "Please fill in all fields"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (!email.contains("@")) {
                tvError.text = "Please enter a valid email"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (password.length < 6) {
                tvError.text = "Password must be at least 6 characters"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Disable button while processing to prevent double taps (it does bad things otherwise, dont ask me how i know)
            tvError.visibility = View.GONE
            btnCreate.isEnabled = false
            btnCreate.text = "Creating..."

            // Check if email is already in use
            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { existingUsers ->
                    if (!existingUsers.isEmpty) {
                        tvError.text = "An account with that email already exists"
                        tvError.visibility = View.VISIBLE
                        btnCreate.isEnabled = true
                        btnCreate.text = "Create Account"
                        return@addOnSuccessListener
                    }
                    // Email is free read the globals doc to get the used UID list
                    db.collection("globals")
                        .document("7Us0uNh9dpMsg2vyaPLW")
                        .get()
                        .addOnSuccessListener { globalsDoc ->
                            // Get used UIDs stored as a list of numbers in Firebase
                            val usedIds = (globalsDoc.get("used_ids") as? List<*>)
                                ?.mapNotNull { (it as? Long)?.toInt() }
                                ?.toMutableList()
                                ?: mutableListOf()
                            // Find the next lowest available UID starting from 1
                            var nextUid = 1
                            while (nextUid in usedIds) {
                                nextUid++
                            }
                            // Add the new UID to the used list before saving
                            usedIds.add(nextUid)
                            // Build new user object document name IS the uid, no uid field needed
                            val newUser = hashMapOf(
                                "profileName" to name,
                                "email" to email,
                                "password" to password,
                                "categories" to emptyList<String>(),
                                "profileLocation" to "",
                                "profileDescription" to "",
                                "friends" to emptyList<String>()
                            )
                            // Name the document with the nextUid
                            db.collection("users")
                                .document(nextUid.toString())
                                .set(newUser)
                                .addOnSuccessListener {
                                    Log.d(TAG, "New user created with document ID: $nextUid")
                                    // Store the numeric ID string in UserSession
                                    UserSession.currentUserId = nextUid.toString()
                                    UserSession.fetchCategories(db) //get all categories
                                    // Update the globals doc with the new UID in the list
                                    db.collection("globals")
                                        .document("7Us0uNh9dpMsg2vyaPLW")
                                        .update("used_ids", usedIds)
                                        .addOnSuccessListener { // open to profile if creation works
                                            Log.d(TAG, "Globals updated — UID $nextUid added to used_ids")
                                            startActivity(Intent(this, ProfileActivity::class.java))
                                            finish()
                                        }
                                        .addOnFailureListener { e ->  // show error message if creation fails for some stupid reason, let them try again
                                            Log.w(TAG, "Error updating globals", e)
                                            startActivity(Intent(this, ProfileActivity::class.java))
                                            finish()
                                        }
                                }
                        }
                }
        }
    }
}