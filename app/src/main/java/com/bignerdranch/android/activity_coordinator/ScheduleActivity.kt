package com.bignerdranch.android.activity_coordinator

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore

class ScheduleActivity : AppCompatActivity() {

    private lateinit var SearchCount: TextView
    private lateinit var Search: EditText
    private lateinit var btnClearSearch: Button
    private lateinit var layoutNoResults: LinearLayout
    private val db = Firebase.firestore
    private lateinit var activityAdapter: ActivityAdapter
    private val allActivities = mutableListOf<ScheduledEvent>()
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)

        //Initialize existing UI
        recyclerView = findViewById(R.id.recycler_search_results)
        recyclerView.layoutManager = LinearLayoutManager(this)

        activityAdapter = ActivityAdapter(allActivities) { event ->
            joinActivity(event)
        }
        recyclerView.adapter = activityAdapter

        fetchInvitedActivities() //Initial fetch of all friend-invited activities

        //Initialize all the UI parts
        SearchCount = findViewById(R.id.search_count)
        Search = findViewById(R.id.search)
        btnClearSearch = findViewById(R.id.btn_clear_search)
        layoutNoResults = findViewById(R.id.layout_no_results)

        //Build UI elements
        setupSearchBar()
        setupNavBar()

        //Button for creating a new activity (event)
        val btnNewActivity = findViewById<Button>(R.id.btn_new_activity)
        btnNewActivity.setOnClickListener {
            //TODO make the call....
            Toast.makeText(this, "This button works", Toast.LENGTH_SHORT).show()
        }
    }

    //Searches activities collection to check whether friended users have put the current user's ID into their invite list for an activity
    private fun fetchInvitedActivities() {
        val currentUid = UserSession.currentUserId ?: return

        db.collection("activities")
            .whereArrayContains("invitedFriends", currentUid)
            .get()
            .addOnSuccessListener { documents ->
                allActivities.clear()
                for (doc in documents) {
                    //.toObject gets the name, location, etc.
                    //.copy(id = doc.id) grabs the "envelope" ID and puts it in the object
                    val event = doc.toObject(ScheduledEvent::class.java).copy(eventId = doc.id)
                    allActivities.add(event)
                }
                activityAdapter.updateData(allActivities)
            }
    }

    //DB action to add current user to an activity they clicked "Join" on.
    //This preserves the UI state of activities the user has joined when leaving ScheduleActivity
    private fun joinActivity(event: ScheduledEvent) {
        val currentUid = UserSession.currentUserId ?: return

        db.collection("activities").document(event.eventId)
            .update("joinedUsers", FieldValue.arrayUnion(currentUid)) //arrayUnion handles multiple users joining an event at the same time
            .addOnSuccessListener {
                Toast.makeText(this, "You have joined ${event.eventName}!", Toast.LENGTH_SHORT).show()
                //the local button update handles the immediate UI feedback
            }
            .addOnFailureListener { e ->
                Log.e("DATABASE_ERROR", "Failed to join activity", e)
                Toast.makeText(this, "Error. Try again", Toast.LENGTH_SHORT).show()
            }
    }

    //Interacts with the search bar to show updated activity search results.
    private fun updateResults() {
        val query = Search.text.toString().trim().lowercase()

        //Filter the master list of all activities, very similar to FriendSearchActivity implementation
        val filtered = allActivities.filter { event ->
            //Return true if search is empty OR if the name matches the query
            query.isEmpty() || event.eventName.lowercase().contains(query)
        }
        //Push the filtered list to the adapter
        activityAdapter.updateData(filtered)

        //Update the UI's response to search results
        SearchCount.text = when {
            filtered.isEmpty() -> "No activities found"
            query.isEmpty() -> "Search by activity name"
            else -> "${filtered.size} result${if (filtered.size != 1) "s" else ""} found"
        }
        //Toggle "No Results" magnifying glass tingy
        layoutNoResults.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
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