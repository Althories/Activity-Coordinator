package com.bignerdranch.android.activity_coordinator

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class FriendProfileActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private val TAG = "FriendProfileActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Reuse the exact same layout as ProfileActivity
        setContentView(R.layout.activity_profile)

        val friendUid = intent.getStringExtra("FRIEND_ID")
        if (friendUid == null) {
            Log.e(TAG, "No FRIEND_ID passed - finishing.")
            finish()
            return
        }

        // Repurpose the Edit button as a Back button
        val editButton = findViewById<Button>(R.id.edit_profile_button)
        editButton.text = "Back"
        editButton.setOnClickListener { finish() }

        // Hide every edit-only element up front
        // The edit_sections_wrapper holds interests picker + activities editor
        findViewById<LinearLayout>(R.id.edit_sections_wrapper).visibility = View.GONE

        findViewById<TextView>(R.id.profile_subtitle).visibility = View.GONE


        // Lock all editable text fields immediately so they render as plain text
        listOf(
            R.id.profileName,
            R.id.profileLocation,
            R.id.profileDescription,
            R.id.profileCurrentActivity
        ).forEach { id ->
            val field = findViewById<EditText>(id)
            field.isFocusable = false
            field.isFocusableInTouchMode = false
            field.background.alpha = 0
        }

        // Load the friend's data
        loadFriendProfile(friendUid)
    }


    private fun loadFriendProfile(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    Log.e(TAG, "Document for $uid not found")
                    finish()
                    return@addOnSuccessListener
                }

                // Text fields
                val name = doc.getString("profileName") ?: "Unknown"
                val location = doc.getString("profileLocation")?: ""
                val bio = doc.getString("profileDescription") ?: ""
                val activity = doc.getString("profileCurrentActivity")?: ""

                findViewById<EditText>(R.id.profileName).setText(name)
                findViewById<EditText>(R.id.profileLocation).setText(location)
                findViewById<EditText>(R.id.profileDescription).setText(bio)
                findViewById<EditText>(R.id.profileCurrentActivity).setText(activity)

                // Initials avatar (same logic as ProfileActivity.getData)
                val initials = name.split(" ").take(2)
                    .joinToString("") { it.firstOrNull()?.uppercase() ?: "" }
                findViewById<Button>(R.id.avatar_1).text = initials

                // "I Am Currently" label visibility
                val activityLabel = findViewById<TextView>(R.id.current_activity_label)
                val activityField = findViewById<EditText>(R.id.profileCurrentActivity)
                val activityBlank = activity.isEmpty() || activity == "null"
                activityLabel.visibility = if (activityBlank) View.GONE else View.VISIBLE
                activityField.visibility = if (activityBlank) View.GONE else View.VISIBLE

                // Profile picture
                val pfp = findViewById<ImageView>(R.id.pfp)
                val bitmap = UserSession.getPfp(uid)
                if (bitmap != null) pfp.setImageBitmap(bitmap)

                // Interest chips (read-only, reuses refreshDisplayChips logic)
                @Suppress("UNCHECKED_CAST")
                val categories = (doc.get("categories") as? List<String>) ?: emptyList()
                showDisplayChips(categories)

                // Activities (read-only)
                val rawActivities = doc.get("activities")
                val activitiesMap = mutableMapOf<String, MutableList<String>>()
                if (rawActivities is Map<*, *>) {
                    rawActivities.forEach { (k, v) ->
                        if (k is String && v is List<*>) {
                            activitiesMap[k] = v.filterIsInstance<String>().toMutableList()
                        }
                    }
                }
                showActivitiesReadOnly(categories, activitiesMap)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to load profile for $uid", e)
                finish()
            }
    }

    // Mirrors ProfileActivity.refreshDisplayChips() exactly
    private fun showDisplayChips(categories: List<String>) {
        val container = findViewById<LinearLayout>(R.id.profile_chips_display)
        container.removeAllViews()

        val dp = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val horizontalPadding = (64 * dp).toInt()

        var currentRow: LinearLayout? = null
        var currentRowWidth = 0

        categories.filter { it.isNotEmpty() && it != "null" }.forEach { label ->
            val chip = TextView(this)
            chip.text = label.replaceFirstChar { it.uppercase() }
            chip.setTextColor(Color.parseColor("#8888A4"))
            chip.textSize = 11f
            chip.isSingleLine = true
            chip.setBackgroundColor(Color.parseColor("#0D0D14"))
            val px9 = (9 * dp).toInt(); val px5 = (5 * dp).toInt()
            chip.setPadding(px9, px5, px9, px5)

            val chipLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            chipLp.marginEnd = (7 * dp).toInt()
            chip.layoutParams = chipLp

            chip.measure(
                View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val chipWidth = chip.measuredWidth + chipLp.marginEnd

            if (currentRow == null || currentRowWidth + chipWidth > screenWidth - horizontalPadding) {
                currentRow = LinearLayout(this)
                currentRow!!.orientation = LinearLayout.HORIZONTAL
                val rowLp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                rowLp.bottomMargin = (4 * dp).toInt()
                currentRow!!.layoutParams = rowLp
                container.addView(currentRow)
                currentRowWidth = 0
            }

            currentRow!!.addView(chip)
            currentRowWidth += chipWidth
        }
    }

    // Shows category headers + activity chips. No input rows, no remove buttons.
    private fun showActivitiesReadOnly(
        categories: List<String>,
        activitiesMap: Map<String, MutableList<String>>
    ) {
        // Only show the section if there's something to display (must have activities present)
        val categoriesWithActivities = categories.filter {
            activitiesMap[it]?.isNotEmpty() == true
        }
        if (categoriesWithActivities.isEmpty()) return

        // Make the wrapper and activities content visible; keep everything else hidden
        val wrapper = findViewById<LinearLayout>(R.id.edit_sections_wrapper)
        wrapper.visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.cats_section_header).visibility = View.GONE
        findViewById<LinearLayout>(R.id.interest_picker_section).visibility = View.GONE
        findViewById<LinearLayout>(R.id.activities_section_header).visibility = View.GONE

        // Show a plain "Activities" title directly in the section title TextView
        val activitiesTitle = findViewById<TextView>(R.id.activities_section_title)
        activitiesTitle.text = "Activities"
        findViewById<TextView>(R.id.activities_section_subtitle).visibility = View.GONE
        findViewById<TextView>(R.id.activities_chevron).visibility = View.GONE

        val activitiesContent = findViewById<LinearLayout>(R.id.activities_section_content)
        activitiesContent.visibility = View.VISIBLE

        val container = findViewById<LinearLayout>(R.id.activities_editor_container)
        container.removeAllViews()

        val dp = resources.displayMetrics.density

        categoriesWithActivities.forEach { cat ->
            val catActivities = activitiesMap[cat] ?: return@forEach

            // Category header row
            val catHeader = LinearLayout(this)
            catHeader.orientation = LinearLayout.HORIZONTAL
            catHeader.gravity = android.view.Gravity.CENTER_VERTICAL
            val headerLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            headerLp.topMargin = (14 * dp).toInt()
            headerLp.bottomMargin = (6 * dp).toInt()
            catHeader.layoutParams = headerLp
            catHeader.setBackgroundColor(Color.parseColor("#1E1E2A"))
            catHeader.setPadding(
                (10 * dp).toInt(), (8 * dp).toInt(),
                (10 * dp).toInt(), (8 * dp).toInt()
            )

            val catTitle = TextView(this)
            catTitle.text = cat.replaceFirstChar { it.uppercase() }
            catTitle.setTextColor(Color.WHITE)
            catTitle.textSize = 13f
            catTitle.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            catHeader.addView(catTitle)
            container.addView(catHeader)

            // Activity chip rows
            val chipsContainer = LinearLayout(this)
            chipsContainer.orientation = LinearLayout.VERTICAL
            chipsContainer.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            catActivities.forEachIndexed { index, act ->
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.gravity = android.view.Gravity.CENTER_VERTICAL
                val rowLp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                rowLp.bottomMargin = (6 * dp).toInt()
                row.layoutParams = rowLp
                row.setBackgroundColor(Color.parseColor("#0D0D14"))

                // Collapse rows beyond the first 2 by default (matches ProfileActivity behaviour)
                if (index >= 2 && catActivities.size > 2) {
                    row.visibility = View.GONE
                }

                val chip = TextView(this)
                chip.text = act
                chip.textSize = 11f
                val px8 = (8 * dp).toInt(); val px4 = (4 * dp).toInt()
                chip.setPadding(px8, px4, px8, px4)
                chip.setTextColor(Color.parseColor("#8888A4"))
                chip.setBackgroundColor(Color.TRANSPARENT)
                chip.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                row.addView(chip)
                chipsContainer.addView(row)
            }

            // "▼ N more" toggle (same logic as ProfileActivity)
            if (catActivities.size > 2) {
                val hiddenCount = catActivities.size - 2
                val toggleRow = TextView(this)
                toggleRow.text = "▼  $hiddenCount more"
                toggleRow.setTextColor(Color.parseColor("#8888A4"))
                toggleRow.textSize = 11f
                val px8 = (8 * dp).toInt(); val px4 = (4 * dp).toInt()
                toggleRow.setPadding(px8, px4, px8, px4)

                var expanded = false
                toggleRow.setOnClickListener {
                    expanded = !expanded
                    // Toggle visibility of rows beyond the first 2
                    for (i in 2 until chipsContainer.childCount - 1) {
                        chipsContainer.getChildAt(i).visibility =
                            if (expanded) View.VISIBLE else View.GONE
                    }
                    toggleRow.text = if (expanded) "▲  show less" else "▼  $hiddenCount more"
                }
                chipsContainer.addView(toggleRow)
            }

            container.addView(chipsContainer)
        }
        findViewById<LinearLayout>(R.id.nav_filter).setOnClickListener {
            startActivity(Intent(this, FilterActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_search).setOnClickListener {
            startActivity(Intent(this, FriendSearchActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }
}