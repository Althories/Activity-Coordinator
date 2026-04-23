package com.bignerdranch.android.activity_coordinator

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import android.widget.Switch


class SettingsActivity : AppCompatActivity() {

    val db = Firebase.firestore
    val TAG = "SettingsActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Email section views
        val emailHeader = findViewById<LinearLayout>(R.id.email_header)
        val emailForm = findViewById<LinearLayout>(R.id.email_form)
        val emailChevron = findViewById<TextView>(R.id.email_chevron)
        val emailCurrentDisplay = findViewById<TextView>(R.id.email_current_display)
        val etNewEmail = findViewById<EditText>(R.id.et_new_email)
        val etEmailConfirmPw = findViewById<EditText>(R.id.et_email_confirm_password)
        val tvEmailError = findViewById<TextView>(R.id.tv_email_error)
        val btnSaveEmail = findViewById<Button>(R.id.btn_save_email)

        // Password section views
        val passwordHeader = findViewById<LinearLayout>(R.id.password_header)
        val passwordForm = findViewById<LinearLayout>(R.id.password_form)
        val passwordChevron = findViewById<TextView>(R.id.password_chevron)
        val etCurrentPassword = findViewById<EditText>(R.id.et_current_password)
        val etNewPassword = findViewById<EditText>(R.id.et_new_password)
        val etConfirmNewPassword = findViewById<EditText>(R.id.et_confirm_new_password)
        val tvPasswordError = findViewById<TextView>(R.id.tv_password_error)
        val btnSavePassword = findViewById<Button>(R.id.btn_save_password)

        // Privacy / session views
        val btnBlockedUsers = findViewById<LinearLayout>(R.id.btn_blocked_users)
        val toggleExactSearch = findViewById<Switch>(R.id.toggle_exact_name_search)
        val tvExactSearchSubtitle = findViewById<TextView>(R.id.tv_exact_search_subtitle)

        // Load current email into the subtitle
        val uid = UserSession.currentUserId
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val currentEmail = doc.getString("email") ?: ""
                    emailCurrentDisplay.text = currentEmail

                    // exactNameSearch field defaults to false if not yet set
                    val exactSearch = doc.getBoolean("exactNameSearch") ?: false
                    toggleExactSearch.isChecked = exactSearch
                    updateExactSearchSubtitle(tvExactSearchSubtitle, exactSearch)
                }
        }

        // Exact Name Search toggle
        toggleExactSearch.setOnCheckedChangeListener { _, isChecked ->
            updateExactSearchSubtitle(tvExactSearchSubtitle, isChecked)
            if (uid == null) return@setOnCheckedChangeListener
            db.collection("users").document(uid)
                .update("exactNameSearch", isChecked)
                .addOnSuccessListener {
                    Log.d(TAG, "exactNameSearch set to $isChecked")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Failed to update exactNameSearch", e)
                    // Revert the toggle visually if the save failed
                    toggleExactSearch.isChecked = !isChecked
                    Toast.makeText(this, "Failed to save — try again", Toast.LENGTH_SHORT).show()
                }
        }

        // Collapsible: Change Email
        emailHeader.setOnClickListener {
            if (emailForm.visibility == View.GONE) {
                emailForm.visibility = View.VISIBLE
                emailChevron.text = "▲"
            } else {
                emailForm.visibility = View.GONE
                emailChevron.text = "▼"
            }
        }

        //Collapsible: Change Password
        passwordHeader.setOnClickListener {
            if (passwordForm.visibility == View.GONE) {
                passwordForm.visibility = View.VISIBLE
                passwordChevron.text = "▲"
            } else {
                passwordForm.visibility = View.GONE
                passwordChevron.text = "▼"
            }
        }

        //Save Email
        btnSaveEmail.setOnClickListener {
            val newEmail    = etNewEmail.text.toString().trim().lowercase()
            val confirmPass = etEmailConfirmPw.text.toString().trim()

            tvEmailError.visibility = View.GONE

            if (newEmail.isEmpty() || confirmPass.isEmpty()) {
                tvEmailError.text = "Please fill in all fields"
                tvEmailError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (!newEmail.contains("@")) {
                tvEmailError.text = "Please enter a valid email"
                tvEmailError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (uid == null) {
                tvEmailError.text = "No user session found — please log in again"
                tvEmailError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnSaveEmail.isEnabled = false
            btnSaveEmail.text = "Saving..."

            // Verify password first
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val storedPassword = doc.getString("password") ?: ""

                    if (confirmPass != storedPassword) {
                        tvEmailError.text = "Incorrect password"
                        tvEmailError.visibility = View.VISIBLE
                        btnSaveEmail.isEnabled = true
                        btnSaveEmail.text = "Save Email"
                        return@addOnSuccessListener
                    }

                    // Check new email isn't already taken
                    db.collection("users")
                        .whereEqualTo("email", newEmail)
                        .get()
                        .addOnSuccessListener { existing ->
                            if (!existing.isEmpty) {
                                tvEmailError.text = "That email is already in use"
                                tvEmailError.visibility = View.VISIBLE
                                btnSaveEmail.isEnabled = true
                                btnSaveEmail.text = "Save Email"
                                return@addOnSuccessListener
                            }

                            // All good — update the email
                            db.collection("users").document(uid)
                                .update("email", newEmail)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Email updated to $newEmail")
                                    emailCurrentDisplay.text = newEmail
                                    etNewEmail.text.clear()
                                    etEmailConfirmPw.text.clear()
                                    emailForm.visibility = View.GONE
                                    emailChevron.text = "▼"
                                    btnSaveEmail.isEnabled = true
                                    btnSaveEmail.text = "Save Email"
                                    Toast.makeText(this, "Email updated!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Failed to update email", e)
                                    tvEmailError.text = "Something went wrong - try again"
                                    tvEmailError.visibility = View.VISIBLE
                                    btnSaveEmail.isEnabled = true
                                    btnSaveEmail.text = "Save Email"
                                }
                        }
                }
        }

        // Save Password
        btnSavePassword.setOnClickListener {
            val currentPw = etCurrentPassword.text.toString().trim()
            val newPw = etNewPassword.text.toString().trim()
            val confirmPw = etConfirmNewPassword.text.toString().trim()

            tvPasswordError.visibility = View.GONE

            if (currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
                tvPasswordError.text = "Please fill in all fields"
                tvPasswordError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (newPw.length < 6) {
                tvPasswordError.text = "New password must be at least 6 characters"
                tvPasswordError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (newPw != confirmPw) {
                tvPasswordError.text = "New passwords do not match"
                tvPasswordError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            if (uid == null) {
                tvPasswordError.text = "No user session found - please log in again"
                tvPasswordError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            btnSavePassword.isEnabled = false
            btnSavePassword.text = "Saving..."

            // Verify current password
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val storedPassword = doc.getString("password") ?: ""

                    if (currentPw != storedPassword) {
                        tvPasswordError.text = "Current password is incorrect"
                        tvPasswordError.visibility = View.VISIBLE
                        btnSavePassword.isEnabled = true
                        btnSavePassword.text = "Save Password"
                        return@addOnSuccessListener
                    }

                    // Update to new password
                    db.collection("users").document(uid)
                        .update("password", newPw)
                        .addOnSuccessListener {
                            Log.d(TAG, "Password updated for user $uid")
                            etCurrentPassword.text.clear()
                            etNewPassword.text.clear()
                            etConfirmNewPassword.text.clear()
                            passwordForm.visibility = View.GONE
                            passwordChevron.text = "▼"
                            btnSavePassword.isEnabled = true
                            btnSavePassword.text = "Save Password"
                            Toast.makeText(this, "Password updated!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to update password", e)
                            tvPasswordError.text = "Something went wrong — try again"
                            tvPasswordError.visibility = View.VISIBLE
                            btnSavePassword.isEnabled = true
                            btnSavePassword.text = "Save Password"
                        }
                }
        }

        // Blocked Users
        // Hook is wired up and ready TODO
        btnBlockedUsers.setOnClickListener {
            Toast.makeText(this, "Coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Log Out
        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            // Clear the user session
            UserSession.currentUserId = null
            // Go back to login and clear the back stack
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK //starts the activity in a fresh task, and FLAG_ACTIVITY_CLEAR_TASK wipes out every activity that was in the back stack
            startActivity(intent) //launches MainActivity with the flags applied
            finish() //Closes ProfileActivity

        }
        findViewById<LinearLayout>(R.id.nav_filter).setOnClickListener {
            startActivity(Intent(this, FilterActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, FriendSearchActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_schedule).setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }
    }

    // Updates the subtitle text under the toggle to reflect current state
    private fun updateExactSearchSubtitle(subtitle: TextView, isChecked: Boolean) {
        subtitle.text = if (isChecked)
            "Only appear when your full name is searched"
        else
            "Appear in all search results"
    }
}