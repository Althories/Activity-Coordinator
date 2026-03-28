package com.bignerdranch.android.activity_coordinator

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.Filter
import kotlinx.coroutines.tasks.await
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FieldPath

class FilterActivity : AppCompatActivity() {

    private lateinit var bottomSheet: LinearLayout
    private lateinit var btnFilter: Button
    private lateinit var btnApplyFilter: Button
    private lateinit var scrollActiveFilters: HorizontalScrollView
    private lateinit var layoutActiveFilters: LinearLayout
    private lateinit var ResultCount: TextView
    private lateinit var friendAdapter: FriendAdapter
    private lateinit var recyclerView: RecyclerView

    private val activeFilters = mutableSetOf<String>()
    private val show_users = mutableSetOf<String>()
    private var currentUserId: String? = null //Stored ID of currently logged in user
    val TAG = "FilterActivity"
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter)

        //Retrieve the user's ID passed from the Login Activity.
        currentUserId = intent.getStringExtra("USER_ID")

        // Nav bar
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, FriendSearchActivity::class.java))
        }

        //Setup for recyclerView to do its magic. Initializes FriendAdapter as empty to be filled given user info
        recyclerView = findViewById(R.id.recycler_friends)
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        friendAdapter = FriendAdapter(emptyList())
        recyclerView.adapter = friendAdapter

        // Hook up views
        bottomSheet = findViewById(R.id.bottom_sheet)
        btnFilter = findViewById(R.id.btn_filter)
        btnApplyFilter = findViewById(R.id.btn_apply_filter)

        scrollActiveFilters = findViewById(R.id.scroll_active_filters)
        layoutActiveFilters = findViewById(R.id.layout_active_filters)
        ResultCount = findViewById(R.id.result_count)
        // Attach click listeners to all the interest chips in the filter sheet
        setupChips()

        // Open filters sheet
        btnFilter.setOnClickListener {
            findViewById<LinearLayout>(R.id.nav_bar).visibility = android.view.View.GONE
            // Make the sheet visible so it can be measured and animated
            bottomSheet.visibility = android.view.View.VISIBLE
            // Start the sheet just below the screen
            bottomSheet.translationY = bottomSheet.height.toFloat()
            bottomSheet.animate()
                .translationY(0f)
                .setDuration(300)
                .start()
        }

        // When Apply is tapped, slide the sheet back down then hide it
        btnApplyFilter.setOnClickListener {
            updateProfiles()
            bottomSheet.animate()
                .translationY(bottomSheet.height.toFloat())
                .setDuration(300)
                .withEndAction {
                    bottomSheet.visibility = android.view.View.GONE
                    findViewById<LinearLayout>(R.id.nav_bar).visibility = android.view.View.VISIBLE

                }
                .start()
        }
        updateProfiles() //Initial call to display all friends on FilterActivity boot
    }

    private fun updateProfiles() {
        val uid = UserSession.currentUserId //Fetches ID from UserSession object to prevent data loss upon switching activities

        if (uid == null) { //If the app process was killed, return to login
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        //Queries the database for the logged in user's document
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                if (!userDoc.exists()) {
                    Log.e("FIRESTORE_DEBUG", "CRITICAL: Document '$currentUserId' does not exist!")
                    return@addOnSuccessListener
                }

                //Fetches friend user ID array from logged in user's document
                val friendIdsRaw = userDoc.get("friends")
                val friendIdStrings = (friendIdsRaw as? List<*>)?.map { it.toString() } ?: emptyList()

                if (friendIdStrings.isNotEmpty()) {
                    db.collection("users")
                        .whereIn(FieldPath.documentId(), friendIdStrings)
                        .get()
                        .addOnSuccessListener { documents ->
                            //Convert Firestore documents to Friend objects to be processed by FriendAdapter.kt
                            val friendsList = documents.map { doc ->
                                Friend(
                                    id = doc.id,
                                    name = doc.getString("name") ?: "Unknown",
                                    categories = (doc.get("categories") as? List<String>) ?: emptyList(), //Adjustment to handle categories as an Array in Firestore
                                    location = doc.getString("profileLocation") ?: "Unknown Location",
                                    bio = doc.getString("profileDescription") ?: "No bio provided"
                                )
                            }

                            //Filtering logic. Determines what to show based on selected filters
                            val filteredFriends = if (activeFilters.isEmpty()) {
                                friendsList
                            } else {
                                //Check if any of the friend's categories match the active filters
                                friendsList.filter { friend ->
                                    activeFilters.any { filter ->
                                        friend.categories.any { it.equals(filter, ignoreCase = true) }
                                    }
                                }
                            }
                            //Update the UI based on filter results
                            displayFriends(filteredFriends)
                        }
                        .addOnFailureListener { e ->
                            Log.e("FIRESTORE_DEBUG", "Query Failed!", e)
                        }
                } else {
                    Log.w("FIRESTORE_DEBUG", "Friend list is empty in DB.")
                    displayFriends(emptyList()) //User has no friends
                }
            }
    }

    private fun setupChips() {
        // Maps each chip's view ID to the interest label it represents
        val chips = mapOf(
            R.id.chip_music   to "Music",
            R.id.chip_hiking  to "Hiking",
            R.id.chip_cooking to "Cooking",
            R.id.chip_gaming  to "Gaming",
            R.id.chip_reading to "Reading",
            R.id.chip_travel  to "Travel",
            R.id.chip_merge_dragons    to "Merge Dragons",
            R.id.chip_coding  to "Coding",
            R.id.chip_disc_golf to "Disc Golf"
        )
        // Loop through every chip and attach a click listener to each one
        chips.forEach { (chipId, label) ->
            val chip = findViewById<TextView>(chipId)
            chip.setOnClickListener {
                if (label in activeFilters) {
                    // Already selected, deselect it and reset to gray
                    activeFilters.remove(label)
                    chip.setTextColor(Color.parseColor("#8888A4"))
                    chip.setBackgroundColor(Color.parseColor("#16161F"))
                } else {
                    // Not selected — select it and highlight green
                    activeFilters.add(label)
                    chip.setTextColor(Color.parseColor("#2ECC71"))
                    chip.setBackgroundColor(Color.parseColor("#222ECC71"))
                }
                // Rebuild the active filter pills row to reflect the new state
                updateActiveFilterRow()
                // filter apply button is dynamic based on if filters are selected
                btnApplyFilter.text = if (activeFilters.isEmpty()) "Show All Friends" else "Show Matches"

            }
        }
    }

    //Helper function to updateProfile(). Calls friendAdapter to update the UI based on filter results
    private fun displayFriends(friends: List<Friend>) {
        friendAdapter.updateData(friends)
        ResultCount.text = if (activeFilters.isEmpty()) "All friends" else "${friends.size} matches found"
    }

    private fun updateActiveFilterRow() {
        // Remove all existing pills before rebuilding the row
        layoutActiveFilters.removeAllViews()

        if (activeFilters.isEmpty()) {
            // No filters active, hide the pill row and reset the subtitle
            scrollActiveFilters.visibility = android.view.View.GONE
            ResultCount.text = "All friends"
            return
        }

        scrollActiveFilters.visibility = android.view.View.VISIBLE
        // Update subtitle to show how many filters are active
        ResultCount.text = "${activeFilters.size} filter${if (activeFilters.size > 1) "s" else ""} active"

        val dp = resources.displayMetrics.density

        activeFilters.forEach { label ->
            val pill = TextView(this)
            // Show the label with an X so the user knows they can tap to remove it

            pill.text     = "$label  ⓧ"
            pill.textSize = 12f
            pill.setTypeface(null, Typeface.BOLD)
            // Style the pill green to match the selected chip color
            pill.setTextColor(Color.parseColor("#2ECC71"))
            pill.setBackgroundColor(Color.parseColor("#222ECC71"))
            // Add horizontal and vertical padding around the pill text
            pill.setPadding(
                (10 * dp).toInt(), (6 * dp).toInt(),
                (10 * dp).toInt(), (6 * dp).toInt()
            )

            val lp = LinearLayout.LayoutParams(
                // Set layout params with a right margin so pills don't touch each other
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = (8 * dp).toInt()
            pill.layoutParams = lp

            // tapping a pill removes that filter
            pill.setOnClickListener {
                activeFilters.remove(label)
                // also deselect the chip in the sheet
                val chipId = when (label) {
                    "Music"   -> R.id.chip_music
                    "Hiking"  -> R.id.chip_hiking
                    "Cooking" -> R.id.chip_cooking
                    "Gaming"  -> R.id.chip_gaming
                    "Reading" -> R.id.chip_reading
                    "Travel"  -> R.id.chip_travel
                    "Merge Dragons" -> R.id.chip_merge_dragons
                    "Coding"  -> R.id.chip_coding
                    "Disc Golf" -> R.id.chip_disc_golf
                    else      -> null
                }
                // Reset the chip in the sheet back to unselected gray
                chipId?.let { id ->
                    findViewById<TextView>(id).setTextColor(Color.parseColor("#8888A4"))
                    findViewById<TextView>(id).setBackgroundColor(Color.parseColor("#16161F"))
                }
                // Rebuild the pill row now that one filter was removed
                updateActiveFilterRow()
                updateProfiles()
                btnApplyFilter.text = if (activeFilters.isEmpty()) "Show All Friends" else "Show Matches"
            }
            // Add the finished pill into the horizontal scroll container
            layoutActiveFilters.addView(pill)

        }
    }

    //get the current selected filters for profile matching
    fun getActiveFilters(): Set<String> = activeFilters.toSet()
}