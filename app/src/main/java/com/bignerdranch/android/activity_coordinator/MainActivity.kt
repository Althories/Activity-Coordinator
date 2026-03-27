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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val Email    = findViewById<EditText>(R.id.email)
        val Password = findViewById<EditText>(R.id.password)
        val Error    = findViewById<TextView>(R.id.error)
        val btnLogin   = findViewById<Button>(R.id.loginbtn)

        btnLogin.setOnClickListener {
            val email = Email.text.toString().trim()
            val password = Password.text.toString().trim()

            if ((email == "dummy@email.com" && password == "password") || email == "" && password == "") {
                // Correct credentials, go to filter page
                startActivity(Intent(this, FilterActivity::class.java))
                finish() // remove login from stack so back button doesn't return here
            } else {
                // Wrong credentials, show error
                Error.visibility = View.VISIBLE
            }
        }
    }
}