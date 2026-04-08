package com.bignerdranch.android.activity_coordinator

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

class FriendSearchActivity : AppCompatActivity() {
    private lateinit var friendAdapter: FriendAdapter
    private val allUsersFromDb = mutableListOf<Friend>() //list of non-friends
    private val activeChips = mutableSetOf<String>()
    private val db = Firebase.firestore
    private lateinit var SearchCount: TextView
    private lateinit var Search: EditText
    private lateinit var btnClearSearch: Button
    private lateinit var layoutNoResults: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private val allCategories = listOf("Music", "Hiking", "Gaming", "Reading", "Merge Dragons", "Cooking", "Travel", "Coding", "Disc Golf") //TODO replace with db query

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friendsearch)

        //Initialize all the UI parts
        SearchCount = findViewById(R.id.search_count)
        Search = findViewById(R.id.search)
        btnClearSearch = findViewById(R.id.btn_clear_search)
        layoutNoResults = findViewById(R.id.layout_no_results)
        recyclerView = findViewById(R.id.recycler_search_results)

        recyclerView.layoutManager = LinearLayoutManager(this) //Setup RecyclerView with the "Add Friend" logic

        //lambda block {} that runs whenever an "Add" button is clicked in the list
        friendAdapter = FriendAdapter(allUsersFromDb, isSearchMode = true) { selectedFriend ->
            addNewFriendToDb(selectedFriend)
        }
        recyclerView.adapter = friendAdapter

        //Build UI elements
        buildInterestChips()
        setupSearchBar()
        setupNavBar()

        fetchPotentialFriends() //Initial db fetch on launch
    }

    private fun fetchPotentialFriends() {
        val currentUid = UserSession.currentUserId ?: return
        allUsersFromDb.clear() //Prevents having duplicate friends after adding a friend once

        //Firestore query to do pretty much the same thing as fetching profile information in FilterActivity.kt
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { userDoc ->
                val myFriends = userDoc.get("friends") as? List<String> ?: emptyList()

                db.collection("users").get()
                    .addOnSuccessListener { allDocs ->
                        //Clear again to prevent async calls
                        allUsersFromDb.clear()

                        for (doc in allDocs) {
                            val uid = doc.id

                            //Only show if not current user and not already a friend in friends field of current user
                            if (uid != currentUid && !myFriends.contains(uid)) {
                                allUsersFromDb.add(Friend(
                                    id = uid,
                                    pfp = UserSession.getPfp(uid),
                                    name = doc.getString("profileName") ?: "Unknown",
                                    location = doc.getString("profileLocation") ?: "Unknown Location",
                                    bio = doc.getString("profileDescription") ?: "",
                                    categories = doc.get("categories") as? List<String> ?: emptyList()
                                ))
                            }
                        }
                        updateResults() //Refresh the UI with the clean list
                    }
            }
    }

    private fun addNewFriendToDb(friend: Friend) {
        val currentUid = UserSession.currentUserId ?: return

        //Add the friend's ID to the current user's "friends" array
        db.collection("users").document(currentUid)
            .update("friends", FieldValue.arrayUnion(friend.id))
            .addOnSuccessListener {
                Toast.makeText(this, "Added ${friend.name}!", Toast.LENGTH_SHORT).show()
                //Call to refresh list to remove the person just added
                fetchPotentialFriends()
            }
            .addOnFailureListener { e ->
                Log.e("SEARCH_DEBUG", "Failed to add friend", e)
            }
    }

    private fun updateResults() {
        val query = Search.text.toString().trim().lowercase()

        //Filters master list of users based on both text search and chip selection
        val filtered = allUsersFromDb.filter { user ->
            val matchesText = query.isEmpty() ||
                    user.name.lowercase().contains(query) ||
                    user.categories.any { it.lowercase().contains(query) }

            val matchesChips = activeChips.isEmpty() ||
                    user.categories.any { it.lowercase() in activeChips }

            matchesText && matchesChips
        }
        friendAdapter.updateData(filtered) //Update the adapter with the new filtered list
        //Update UI counters and visibility
        SearchCount.text = when {
            filtered.isEmpty() -> "No results found"
            query.isEmpty() && activeChips.isEmpty() -> "Search by name or interest"
            else -> "${filtered.size} result${if (filtered.size != 1) "s" else ""} found"
        }
        layoutNoResults.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun buildInterestChips() {
        val container = findViewById<LinearLayout>(R.id.layout_search_chips)
        allCategories.forEach { label ->
            val chip = TextView(this)
            chip.text = label
            chip.setTextColor(Color.parseColor("#8888A4"))
            chip.setBackgroundColor(Color.parseColor("#16161F"))
            chip.setPadding(35, 20, 35, 20) //Adjusted for pixel density

            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = 20
            chip.layoutParams = lp

            chip.setOnClickListener {
                val key = label.lowercase()
                if (key in activeChips) {
                    activeChips.remove(key)
                    chip.setTextColor(Color.parseColor("#8888A4"))
                    chip.setBackgroundColor(Color.parseColor("#16161F"))
                } else {
                    activeChips.add(key)
                    chip.setTextColor(Color.parseColor("#2ECC71"))
                    chip.setBackgroundColor(Color.parseColor("#222ECC71"))
                }
                updateResults()
            }
            container.addView(chip)
        }
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
            activeChips.clear()
            fetchPotentialFriends() //Reset all chip visuals manually or by rebuilding
        }
    }

    private fun setupNavBar() {
        findViewById<LinearLayout>(R.id.nav_filter).setOnClickListener {
            startActivity(Intent(this, FilterActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}