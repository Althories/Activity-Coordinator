package com.bignerdranch.android.activity_coordinator

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ScheduleActivity : AppCompatActivity() {

    private lateinit var SearchCount: TextView
    private lateinit var Search: EditText
    private lateinit var btnClearSearch: Button
    private lateinit var layoutNoResults: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        //Initialize all the UI parts
        SearchCount = findViewById(R.id.search_count)
        Search = findViewById(R.id.search)
        btnClearSearch = findViewById(R.id.btn_clear_search)
        layoutNoResults = findViewById(R.id.layout_no_results)

        //Build UI elements
        setupSearchBar()
        setupNavBar()
    }

    private fun updateResults() {
        val query = Search.text.toString().trim().lowercase()
    }

    private fun setupSearchBar() {
        Search.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                btnClearSearch.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
                updateResults()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            Search.setText("")
        }
    }

    private fun setupNavBar() {
        findViewById<LinearLayout>(R.id.nav_filter).setOnClickListener {
            startActivity(Intent(this, FilterActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, FriendSearchActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}