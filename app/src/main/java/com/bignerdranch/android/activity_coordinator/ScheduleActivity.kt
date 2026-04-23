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
import com.google.firebase.firestore.Filter

class ScheduleActivity : AppCompatActivity() {

    private lateinit var SearchCount: TextView
    private lateinit var Search: EditText
    private lateinit var btnClearSearch: Button
    private lateinit var layoutNoResults: LinearLayout
    private val db = Firebase.firestore
    private lateinit var activityAdapter: ActivityAdapter
    private val allActivities = mutableListOf<ScheduledEvent>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var createSheet: LinearLayout

    //Invite friend to activity vars
    private val selectedFriendIds = mutableSetOf<String>()
    private lateinit var inviteAdapter: FriendAdapter //Reuses existing adapter
    private val myFriendsList = mutableListOf<Friend>()
    private var currentUserName: String = "Me" // Fallback for invite name

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

        //Initialize UI parts
        SearchCount = findViewById(R.id.search_count)
        Search = findViewById(R.id.search)
        btnClearSearch = findViewById(R.id.btn_clear_search)
        layoutNoResults = findViewById(R.id.layout_no_results)

        //Build UI elements
        setupSearchBar()
        setupNavBar()
        createActivityLogic()

        fetchInvitedActivities() //Initial fetch of all friend-invited activities
        fetchCurrentUserName() //For creating activities
    }

    //View: Searches activities collection to check whether friended users have put the current user's ID into their invite list for an activity
    private fun fetchInvitedActivities() {
        val currentUid = UserSession.currentUserId ?: return

        db.collection("activities")
            .where( //Shows activity if you were invited OR you created the activity
                Filter.or(
                Filter.arrayContains("invitedFriends", currentUid),
                Filter.equalTo("creatorId", currentUid)
            ))
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
            }
            .addOnFailureListener { e ->
                Log.e("DATABASE_ERROR", "Failed to join activity", e)
                Toast.makeText(this, "Error joining activity. Try again", Toast.LENGTH_SHORT).show()
            }
    }

    //Shows updated activity search results.
    private fun updateResults() {
        val query = Search.text.toString().trim().lowercase()

        //Filter the master list of all activities, very similar to FriendSearchActivity implementation
        val filtered = allActivities.filter { event ->
            query.isEmpty() || event.eventName.lowercase().contains(query) //Return true if search is empty OR if the name matches the query
        }
        activityAdapter.updateData(filtered) //Push the filtered list to the adapter

        //Update the UI's response to search results
        SearchCount.text = when {
            filtered.isEmpty() -> "No activities found"
            query.isEmpty() -> "Search by activity name"
            else -> "${filtered.size} result${if (filtered.size != 1) "s" else ""} found"
        }
        layoutNoResults.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE //Toggle "No Results" magnifying glass tingy
    }

    private fun createActivityLogic() {
        //UI Initialization
        createSheet = findViewById(R.id.create_activity_sheet)
        val btnAddHeader = findViewById<Button>(R.id.btn_new_activity)
        val btnConfirm = findViewById<Button>(R.id.btn_confirm_create)
        val editName = findViewById<EditText>(R.id.edit_event_name)
        val editLocation = findViewById<EditText>(R.id.edit_event_location)
        val editDesc = findViewById<EditText>(R.id.edit_event_description)
        //Initialize the Adapter and RecyclerView to show friends to invite
        val inviteRecyclerView = findViewById<RecyclerView>(R.id.recycler_invite_friends)
        inviteRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        //List and a callback that toggles the ID in our selection set
        inviteAdapter = FriendAdapter(myFriendsList, isSearchMode = true) { selectedFriend ->
            if (selectedFriendIds.contains(selectedFriend.id)) {
                selectedFriendIds.remove(selectedFriend.id)
            } else {
                selectedFriendIds.add(selectedFriend.id)
            }
        }
        inviteRecyclerView.adapter = inviteAdapter

        //Toggle sheet visibility and fetch data depending on what the user's doing
        btnAddHeader.setOnClickListener {
            if (createSheet.visibility == View.GONE) {
                createSheet.visibility = View.VISIBLE
                fetchFriendsForInvites() //Call because the adapter now exists. Will round em up
            } else {
                createSheet.visibility = View.GONE
            }
        }

        //Confirm creation of new activity
        btnConfirm.setOnClickListener {
            val name = editName.text.toString().trim()
            val loc = editLocation.text.toString().trim()
            val desc = editDesc.text.toString().trim()

            //Prevents user from creating a new completely empty activity
            if (name.isNotEmpty()) {
                createActivityInDb(name, loc, desc) //Next function!
            } else {
                Toast.makeText(this, "Please enter an activity name.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //Handles the creation of new activities in the database. Called by createActivityLogic()
    private fun createActivityInDb(name: String, location: String, description: String) {
        val currentUid = UserSession.currentUserId ?: return

        val newEvent = ScheduledEvent(
            creatorId = currentUid,
            creatorName = UserSession.userName, //Uses fetched username
            eventName = name,
            eventLocation = location,
            eventDescription = description,
            joinedUsers = listOf(currentUid),
            invitedFriends = selectedFriendIds.toList()
        )

        db.collection("activities").add(newEvent)
            .addOnSuccessListener {
                Toast.makeText(this, "Activity Created and Invites sent", Toast.LENGTH_SHORT).show()
                createSheet.visibility = View.GONE
                selectedFriendIds.clear() //Reset for next event user creates
                fetchInvitedActivities() //Refresh activity
            }
    }

    //Function for grabbing friend information for the friends list in creating a new activity
    private fun fetchFriendsForInvites() {
        val currentUid = UserSession.currentUserId ?: return

        //Get the current user's friend list IDs
        db.collection("users").document(currentUid).get()
            .addOnSuccessListener { doc ->
                val friendIds = doc.get("friends") as? List<String> ?: emptyList()

                if (friendIds.isNotEmpty()) {
                    //Fetch the actual user objects for those IDs for detailed information
                    db.collection("users")
                        .where(Filter.inArray(com.google.firebase.firestore.FieldPath.documentId(), friendIds))
                        .get()
                        .addOnSuccessListener { snapshots ->
                            myFriendsList.clear()
                            for (friendDoc in snapshots) {
                                //Map Firestore document to friend model
                                val friend = Friend(
                                    id = friendDoc.id,
                                    name = friendDoc.getString("profileName") ?: "Unknown",
                                    bio = friendDoc.getString("bio") ?: "",
                                    location = friendDoc.getString("location") ?: "",
                                    categories = friendDoc.get("interests") as? List<String> ?: emptyList()
                                )
                                myFriendsList.add(friend)
                            }
                            inviteAdapter.updateData(myFriendsList)
                        }
                        .addOnFailureListener { e ->
                            Log.e("INVITE_ERROR", "Failed to fetch friend details", e)
                        }
                } else {
                    //If user has no friends, clear the list (devastating)
                    myFriendsList.clear()
                    inviteAdapter.updateData(myFriendsList)
                }
            }
            .addOnFailureListener { e ->
                Log.e("INVITE_ERROR", "Failed to fetch user friend list", e)
            }
    }

    private fun fetchCurrentUserName() {
        val uid = UserSession.currentUserId ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            currentUserName = doc.getString("profileName") ?: "Me"
            //Updates the session so it's saved globally
            UserSession.userName = currentUserName
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
        findViewById<LinearLayout>(R.id.nav_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
}