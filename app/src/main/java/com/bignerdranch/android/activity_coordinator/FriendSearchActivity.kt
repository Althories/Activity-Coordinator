package com.bignerdranch.android.activity_coordinator

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FriendSearchActivity : AppCompatActivity() {
    // Each profile
    data class Profile(val searchName: String, val cardId: Int, val interests: List<String>)
    private val profiles = listOf(
        Profile("cooper ptacek", R.id.search_card_1, listOf("music", "hiking", "gaming")),
        Profile("nik kopek", R.id.search_card_2, listOf("gaming", "reading", "music")),
        Profile("bmcp solhjem",R.id.search_card_3, listOf("merge dragons"))
    )
    private val allInterests = listOf("Music", "Hiking", "Gaming", "Reading", "Merge Dragons", "Cooking", "Travel", "Coding", "Disc Golf")
    private val activeChips = mutableSetOf<String>()
    private lateinit var SearchCount: TextView
    private lateinit var Search: EditText
    private lateinit var btnClearSearch: Button
    private lateinit var layoutNoResults: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friendsearch)
        // Connect variables to their views in the XML by ID
        SearchCount = findViewById(R.id.search_count)
        Search = findViewById(R.id.search)
        btnClearSearch = findViewById(R.id.btn_clear_search)
        layoutNoResults = findViewById(R.id.layout_no_results)
        //inital builds/setups
        buildInterestChips()
        setupSearchBar()
        setupAddButtons()
        setupNavBar()
        updateResults()
    }
    // Interest Chips hypothetically could use similar function when making not hardcoded, pull from database
    private fun buildInterestChips() { //container is just the linear layout
        // connect the horizontal container that holds the chips
        val container = findViewById<LinearLayout>(R.id.layout_search_chips)
        allInterests.forEach { label ->
            // Create a new TextView to act as a chip
            val chip = TextView(this)
            chip.text = label
            chip.setTextColor(Color.parseColor("#8888A4"))
            chip.setBackgroundColor(Color.parseColor("#16161F"))
            // Add padding inside the pill
            chip.setPadding(14, 9, 14, 9)
            // Set the chip size to wrap its content and add a right margin between chips
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = 10
            chip.layoutParams = lp

            chip.setOnClickListener {
                val key = label.lowercase()
                if (key in activeChips) { // Already selected
                    activeChips.remove(key)
                    chip.setTextColor(Color.parseColor("#8888A4"))
                    chip.setBackgroundColor(Color.parseColor("#16161F"))
                } else { // Not selected
                    activeChips.add(key)
                    chip.setTextColor(Color.parseColor("#2ECC71"))
                    chip.setBackgroundColor(Color.parseColor("#222ECC71"))
                }
                // Show clear button if any chips are active or there is text in the search box
                btnClearSearch.visibility =
                    if (activeChips.isNotEmpty() || Search.text.isNotEmpty()) android.view.View.VISIBLE
                    else android.view.View.GONE
                updateResults()
            }
            // Add the chip into the horizontal scroll container
            container.addView(chip)
        }
    }
    // Search Bar
    private fun setupSearchBar() {
        // Watch for text changes as the user types documentation avaliable on android developer for textwatcher
        Search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Show clear button only when there is text
                btnClearSearch.visibility =
                    if (s.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
                // Refresh results every time the text changes
                updateResults()
            }
            override fun afterTextChanged(s: Editable?) {} //need all 3 functions for textwatcher, dont ask me why. I use da google
        })
        btnClearSearch.setOnClickListener {
            // clear the search text
            Search.setText("")
            // Clear all active chip selections
            activeChips.clear()
            // Reset all chip colors
            val container = findViewById<LinearLayout>(R.id.layout_search_chips)
            for (i in 0 until container.childCount) {
                val chip = container.getChildAt(i) as TextView
                chip.setTextColor(Color.parseColor("#8888A4"))
                chip.setBackgroundColor(Color.parseColor("#16161F"))
            }
            // Refresh results now that everything is cleared
            updateResults()
        }
    }
    // Add the Add buttons
    private fun setupAddButtons() {
        // Map each button ID to the person's display name
        val addButtons = mapOf(
            R.id.btn_add_1 to "Cooper Ptacek",
            R.id.btn_add_2 to "Nik Kopek",
            R.id.btn_add_3 to "BMCP Solhjem")

        addButtons.forEach { (btnId, name) ->
            val btn = findViewById<Button>(btnId)
            btn.setOnClickListener {
                // Change button to show added state
                btn.text = "✓ Added"
                btn.backgroundTintList = // Gray out the button so it looks inactive (backgroundTintList)
                    android.content.res.ColorStateList.valueOf(Color.parseColor("#2A2A38"))
                btn.isClickable = false
                Toast.makeText(this, "$name added as friend", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Basic Filter logic
    private fun updateResults() {
        val query = Search.text.toString().trim().lowercase() //current search text, trimmed and lowercased for comparison
        var visibleCount = 0

        profiles.forEach { profile ->
            val card = findViewById<LinearLayout>(profile.cardId)
            // Name matches if search box is empty or profile name contains the query
            val textMatch = query.isEmpty() || profile.searchName.contains(query) || profile.interests.any { it.contains(query) }            // Interest matches if no chips selected or profile has at least one active chip
            //can search interests or click the filters
            val interestMatch = activeChips.isEmpty() || profile.interests.any { it in activeChips }

            // Card shows if it matches both name query and active chips
            val show = textMatch && interestMatch
            card.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
            if (show) visibleCount++
        }

        // Update the subtitle text (WHEN FUNCTIONS ARE SO COOL)
        SearchCount.text = when {
            query.isEmpty() && activeChips.isEmpty() -> "Search by name or interest"
            visibleCount == 0 -> "No results found"
            else -> "$visibleCount result${if (visibleCount != 1) "s" else ""} found"
        }
        // Show the empty state layout only when no cards are visible
        layoutNoResults.visibility =
            if (visibleCount == 0) android.view.View.VISIBLE else android.view.View.GONE
    }
    private fun setupNavBar() { //makes nav bar on this page function
        findViewById<LinearLayout>(R.id.nav_filter).setOnClickListener {
            startActivity(Intent(this, FilterActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}