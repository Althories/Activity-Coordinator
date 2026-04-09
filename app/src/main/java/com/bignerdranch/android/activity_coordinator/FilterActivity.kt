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
import android.view.View
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
        friendAdapter = FriendAdapter(emptyList(), isSearchMode = false)
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
                                    name = doc.getString("profileName") ?: "Unknown",
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
//creates filter chips and handles selection logic (coloring)
    private fun setupChips() {
        val container = findViewById<LinearLayout>(R.id.chip_container)
        container.removeAllViews()
        // Use categories from session or fallback list (used for testing, fallback should never be seen but might prevent crashes if something goes wrong)
        val categories = UserSession.allCategories.ifEmpty {
            listOf("Music","Hiking","Cooking","Gaming","Reading","Travel","Merge Dragons","Coding","Disc Golf")
        }

        val dp = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val horizontalPadding = (48 * dp).toInt() // 24dp padding each side from bottom sheet

        var currentRow: LinearLayout? = null
        var currentRowWidth = 0
        // Create chip (TextView styled as button)
        categories.forEach { label ->
            val chip = TextView(this)
            chip.text = label.replaceFirstChar { it.uppercase() }
            chip.setTextColor(Color.parseColor("#8888A4"))
            chip.setBackgroundColor(Color.parseColor("#16161F"))
            chip.textSize = 13f
            chip.isSingleLine = true
            chip.maxLines = 1
            chip.tag = label
            val px14 = (14 * dp).toInt()
            val px9 = (9 * dp).toInt()
            chip.setPadding(px14, px9, px14, px9)
            // Layout params for spacing
            val chipLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            chipLp.marginEnd = (10 * dp).toInt()
            chip.layoutParams = chipLp
            // Measure chip width to wrap rows properly
            chip.measure(
                View.MeasureSpec.makeMeasureSpec(screenWidth - horizontalPadding, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val chipWidth = chip.measuredWidth + chipLp.marginEnd
            // Start new row if needed
            if (currentRow == null || currentRowWidth + chipWidth > screenWidth - horizontalPadding) {
                currentRow = LinearLayout(this)
                currentRow!!.orientation = LinearLayout.HORIZONTAL
                val rowLp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                rowLp.bottomMargin = (10 * dp).toInt()
                currentRow!!.layoutParams = rowLp
                container.addView(currentRow)
                currentRowWidth = 0
            }
            // Handle chip click (toggle selection)
            chip.setOnClickListener {
                if (label in activeFilters) {
                    activeFilters.remove(label)
                    chip.setTextColor(Color.parseColor("#8888A4"))
                    chip.setBackgroundColor(Color.parseColor("#16161F"))
                } else {
                    activeFilters.add(label)
                    chip.setTextColor(Color.parseColor("#2ECC71"))
                    chip.setBackgroundColor(Color.parseColor("#222ECC71"))
                }
                updateActiveFilterRow()
                // Update button text
                btnApplyFilter.text = if (activeFilters.isEmpty()) "Show All Friends" else "Show Matches"
            }

            currentRow!!.addView(chip)
            currentRowWidth += chipWidth
        }
    }
    //Helper function to updateProfile(). Calls friendAdapter to update the UI based on filter results
    private fun displayFriends(friends: List<Friend>) {
        friendAdapter.updateData(friends)
        ResultCount.text = if (activeFilters.isEmpty()) "All friends" else "${friends.size} matches found"
    }

    private fun updateActiveFilterRow() {
        layoutActiveFilters.removeAllViews()

        if (activeFilters.isEmpty()) {
            scrollActiveFilters.visibility = android.view.View.GONE
            ResultCount.text = "All friends"
            return
        }

        scrollActiveFilters.visibility = android.view.View.VISIBLE
        ResultCount.text = "${activeFilters.size} filter${if (activeFilters.size > 1) "s" else ""} active"

        val dp = resources.displayMetrics.density

        activeFilters.forEach { label ->
            val pill = TextView(this)
            pill.text = "$label  ⓧ"
            pill.textSize = 12f
            pill.setTypeface(null, Typeface.BOLD)
            pill.setTextColor(Color.parseColor("#2ECC71"))
            pill.setBackgroundColor(Color.parseColor("#222ECC71"))
            pill.setPadding(
                (10 * dp).toInt(), (6 * dp).toInt(),
                (10 * dp).toInt(), (6 * dp).toInt()
            )
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = (8 * dp).toInt()
            pill.layoutParams = lp

            pill.setOnClickListener {
                activeFilters.remove(label)
                // Find the chip by tag and reset its color
                val container = findViewById<LinearLayout>(R.id.chip_container)
                // Loop through each row inside the container
                // container.childCount = number of rows
                for (i in 0 until container.childCount) {
                    // Each child of container should be a LinearLayout (a row of chips)
                    // Safe cast (as?) is used in case something unexpected is in the container
                    // If casting fails (null), skip this iteration with 'continue'
                    val row = container.getChildAt(i) as? LinearLayout ?: continue
                    // Loop through each chip inside the current row
                    // row.childCount = number of chips in that row
                    for (j in 0 until row.childCount) {
                        val chip = row.getChildAt(j) as? TextView
                        if (chip?.tag == label) {
                            chip.setTextColor(Color.parseColor("#8888A4"))
                            chip.setBackgroundColor(Color.parseColor("#16161F"))
                        }
                    }
                }
                updateActiveFilterRow()
                updateProfiles()
                btnApplyFilter.text = if (activeFilters.isEmpty()) "Show All Friends" else "Show Matches"
            }
            layoutActiveFilters.addView(pill)
        }
    }

    //get the current selected filters for profile matching
    fun getActiveFilters(): Set<String> = activeFilters.toSet()
}