package com.bignerdranch.android.activity_coordinator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()    //Init Firestore instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val emailField    = findViewById<EditText>(R.id.email)
        val passwordField = findViewById<EditText>(R.id.password)
        val errorText    = findViewById<TextView>(R.id.error)
        val btnLogin   = findViewById<Button>(R.id.loginbtn)

        btnLogin.setOnClickListener {
            val emailInput = emailField.text.toString().trim()
            val passwordInput = passwordField.text.toString().trim()

            //Ensures user actually entered content for both input fields
            if (emailInput.isEmpty() || passwordInput.isEmpty()) {
                errorText.text = "Please fill in all fields."
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }

            //Firestore Query searching the "users" collection.
            //Compares the input email to the email field of each document (user) and returns a match
            db.collection("users")
                .whereEqualTo("email", emailInput)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {    //Input email does not match with an existing email in db
                        errorText.text = "Unregistered email entered"
                        errorText.visibility = View.VISIBLE
                    } else {
                        val document = documents.documents[0] //Grabs first result from firestone query. Ideally this is the only result
                        val dbPassword = document.getString("password") //fetches db password to compare with input

                        if (dbPassword == passwordInput) {  //Passwords match, user may login
                            startActivity(Intent(this, FilterActivity::class.java))
                            finish()
                        } else { //passwords did not match, womp womp
                            errorText.text = "Invalid password entered"
                            errorText.visibility = View.VISIBLE
                        }
                    }
                }
                .addOnFailureListener {e -> //Handler for permission issues
                    errorText.text = "Login failed: ${e.message}"
                    errorText.visibility = View.VISIBLE
                }
        }
    }
}