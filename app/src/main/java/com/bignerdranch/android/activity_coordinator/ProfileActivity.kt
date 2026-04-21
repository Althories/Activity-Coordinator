package com.bignerdranch.android.activity_coordinator

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.firestore
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.registerForActivityResult
import com.bignerdranch.android.activity_coordinator.UserSession.currentUserId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.*
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.*
import java.util.Locale
import com.google.firebase.storage.*
import kotlinx.datetime.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
//import java.time.Instant
import kotlin.collections.joinToString
import kotlin.collections.take
import kotlin.text.split
import kotlin.toString


class ProfileActivity : AppCompatActivity() {
    var uid = UserSession.currentUserId
    var db = Firebase.firestore
    var TAG = "ProfileActivity"
    var chosenCats = mutableSetOf<String>()
    var changedCats = mutableSetOf<String>()
    var activitiesMap = mutableMapOf<String, MutableList<String>>() //Storeing activities
    var isEditing = false
    val storage = Firebase.storage // Firebase cloud storage, where all picture assets live

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        getData(uid.toString(),arrayOf("profileName","profileLocation","profileDescription","profileCurrentActivity","currentMin"))
        test()
            var catsExpanded = false
            var activitiesExpanded = false

            val catsHeader = findViewById<LinearLayout>(R.id.cats_section_header)
            val catsChevron = findViewById<TextView>(R.id.cats_chevron) // anything in the collapse fields i called chevron (chevron symbol ▲)
            val catsContent = findViewById<LinearLayout>(R.id.interest_picker_section)

            val activitiesHeader = findViewById<LinearLayout>(R.id.activities_section_header)
            val activitiesChevron = findViewById<TextView>(R.id.activities_chevron)
            val activitiesContent = findViewById<LinearLayout>(R.id.activities_section_content)

            catsHeader.setOnClickListener {
                catsExpanded = !catsExpanded
                catsContent.visibility = if (catsExpanded) android.view.View.VISIBLE else android.view.View.GONE
                catsChevron.text = if (catsExpanded) "▲" else "▼"
            }

        activitiesHeader.setOnClickListener {
            if (isEditing) {
                activitiesExpanded = !activitiesExpanded
                activitiesContent.visibility = if (activitiesExpanded) android.view.View.VISIBLE else android.view.View.GONE
                activitiesChevron.text = if (activitiesExpanded) "▲" else "▼"
            }
        }
        UserSession.fetchCategories(db) {
            runOnUiThread {
                getCats(uid.toString())
                loadActivities(uid.toString())
            }
        }
        ///                            Log.w("BHBDUIBHDIKBJ", temp.toString())
        ////                           val returned = csvToText("merge_dragons,cats,morger,music")

        // Hardcoded example user - to be replaced with currently logged-in user
        val user = hashMapOf(
            "uid" to 1,
            "name" to "CooperPtacek",
            "email" to "cptacek@gustavus.edu",
            "categories"  to "music,hiking,disc golf"
        )



        // Check if user with given UID exists in the database and if not, add them
        /*db.collection("users")
            .where(Filter.equalTo("uid", user["uid"]))
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty){
                    db.collection("users")
                        .add(user)
                        .addOnSuccessListener { documentReference ->
                            Log.d(TAG, "DocumentSnapshot added with ID: {documentReference.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding document", e)
                        }
                } else {
                    Log.d(TAG, "User with UID ${user["uid"]} already exists")
                }
            }*/

        //List of all editable profile text fields
        val editableFields = listOf(
            findViewById<EditText>(R.id.profileName),
            findViewById<EditText>(R.id.profileLocation),
            findViewById<EditText>(R.id.profileDescription),
            findViewById<EditText>(R.id.profileCurrentActivity),
            findViewById<EditText>(R.id.time_hour),
            findViewById<EditText>(R.id.time_min)


        )

        val avatar = findViewById<Button>(R.id.avatar_1)

        // Branden
        val pfp = findViewById<ImageView>(R.id.pfp) // Profile picture view
        // The user's profile picture is stored as a jpg named their uid.
        val pfpRef = UserSession.storage.reference.child(
            "userProfilePictures/$uid.jpg")
        // current profile picture will be stored as a bitmap in usersession
        pfp.setImageBitmap(UserSession.getPfp())

        var changedPfp = false // Flag for whether we should upload the profile pic when saving
        // ^ Branden

        val editButton = findViewById<Button>(R.id.edit_profile_button)
        //Controls whether EditViews are editable. editing boolean has the same T/F state as isEditing
        fun applyEditState(editing: Boolean) {
            findViewById<LinearLayout>(R.id.time_check).visibility =
                if (editing) android.view.View.VISIBLE else android.view.View.GONE
            editableFields.forEach { field ->
                field.isFocusableInTouchMode = editing
                field.isFocusable = editing
                field.background.alpha = if (editing) 255 else 0
                if (!editing) field.clearFocus()
            }
            val editSections = findViewById<LinearLayout>(R.id.edit_sections_wrapper)
            editSections.visibility = android.view.View.VISIBLE  // always visible
            findViewById<LinearLayout>(R.id.cats_section_header).visibility =
                if (editing) android.view.View.VISIBLE else android.view.View.GONE
            findViewById<LinearLayout>(R.id.interest_picker_section).visibility =
                if (editing) android.view.View.GONE else android.view.View.GONE //todo This line doesnt do anything

            if (editing) {
                catsExpanded = false
                catsContent.visibility = android.view.View.GONE
                catsChevron.text = "▼"
                // leave activitiesContent visible and expanded in edit mode
                activitiesExpanded = false
                activitiesContent.visibility = android.view.View.VISIBLE
                activitiesChevron.visibility = android.view.View.VISIBLE
                activitiesChevron.text = "▼"
            } else {
                activitiesContent.visibility = android.view.View.VISIBLE
                activitiesChevron.visibility = if (editing) android.view.View.VISIBLE else android.view.View.GONE
            }
            findViewById<LinearLayout>(R.id.bottom_sheet).visibility = android.view.View.GONE
            editButton.text = if (editing) "Save" else "Edit"
            // Avatar edit hint
            val avatar = findViewById<Button>(R.id.avatar_1)
            val pfp = findViewById<ImageView>(R.id.pfp)
            if (editing) {
                pfp.alpha = 0.4f
                avatar.text = "edit"
                avatar.setTextColor(Color.WHITE)
                avatar.textSize = 13f
            } else {
                pfp.alpha = 1f
                // Restore original image/initials
                val nameText = findViewById<EditText>(R.id.profileName).text.toString()
                avatar.text = nameText.split(" ").take(2).joinToString("") { it.first().uppercase() }
                avatar.textSize = 17f
            }
            if (!editing) { //when not editing, all edit features of the activities are supressed
                activitiesContent.visibility = android.view.View.VISIBLE
                activitiesChevron.visibility = android.view.View.GONE
            }
            //handles the changing headers in the dropdowns
            val activitiesTitle = findViewById<TextView>(R.id.activities_section_title)
            val activitiesSubtitle = findViewById<TextView>(R.id.activities_section_subtitle)
            if (editing) {
                activitiesTitle.text = "Edit Your Activities"
                activitiesSubtitle.visibility = android.view.View.VISIBLE
                activitiesSubtitle.text = "Add specific activities within your interests"

            } else {
                activitiesTitle.text = "Activities"
                activitiesSubtitle.visibility = android.view.View.GONE

            }
            val activityLabel = findViewById<TextView>(R.id.current_activity_label)
            val activityField = findViewById<EditText>(R.id.profileCurrentActivity)
            if (editing) {
                activityLabel.visibility = android.view.View.VISIBLE
                activityField.visibility = android.view.View.VISIBLE
            } else {
                val text = activityField.text.toString()
                val isBlank = text.isEmpty() || text == "null"
                activityLabel.visibility = if (isBlank) android.view.View.GONE else android.view.View.VISIBLE
                activityField.visibility = if (isBlank) android.view.View.GONE else android.view.View.VISIBLE
            }
            refreshActivitiesEditor(editing)

        }

        //applyEditState is run ONCE at start of program to hide EditView bars in initial non-edit state
        applyEditState(false)

        // Converts the current onscreen profile picture to a square bitmap of given size in pixels.
        // To be used to ready a profile picture for uploading and rescaling uploaded images.
        // - Branden
        fun getProfilePictureAsBitmap(imageSize: Int = UserSession.MAX_PFP_SIZE): Bitmap {
            val bitmap = (pfp.drawable as BitmapDrawable).bitmap
            // Size of image in bytes is at most imageSize squared plus ~35 bytes overhead
            // With imageSize = 200, size is at most about 40kb (usually less due to JPEG compression)
            val bitmapScaled = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
            return bitmapScaled
        }

        //Edit mode button for profile page
        editButton.setOnClickListener {
            isEditing = !isEditing //Toggles edit state
            applyEditState(isEditing)
            if (!isEditing) { // upon pressing Save  BRANDEN
                Log.d(TAG, (findViewById<EditText>(R.id.profileName).text).toString())
                Log.d(TAG, (findViewById<EditText>(R.id.profileLocation).text).toString())
                Log.d(TAG, (findViewById<EditText>(R.id.profileDescription).text).toString())
                refreshDisplayChips()
                // save activities to Firestore
                db.document("users/$uid").update("activities", activitiesMap as Map<String, List<String>>)
                    .addOnSuccessListener { Log.d(TAG, "Activities saved") }
                    .addOnFailureListener { e -> Log.w(TAG, "Error saving activities", e) }

                // The following code only runs if the profile picture was changed
                // - Branden
                if (changedPfp) {
                    // Upload picture to storage as "[user id].jpg"
                    val bitmapScaled = getProfilePictureAsBitmap()
                    val baos = ByteArrayOutputStream()
                    bitmapScaled.compress(
                        Bitmap.CompressFormat.JPEG, 100, baos)
                    val data = baos.toByteArray()
                    Log.d(TAG, "Selected photo is ${data.size} bytes")
                    // Save to a jpg file in storage named the user's uid
                    val uploadTask = pfpRef.putBytes(data)
                    uploadTask.addOnFailureListener { e ->
                        Log.w(TAG, "Upload failed", e)
                    }.addOnSuccessListener { taskSnapshot ->
                        Log.d(TAG, "Upload successful: $taskSnapshot")
                        UserSession.updatePfpLocally(bitmapScaled) // Update the current session's profile picture
                    }

                    changedPfp = false // reset flag
                }
                val current_tiem = try {
                    findViewById<EditText>(R.id.time_hour).text.toString().toInt()*60+findViewById<EditText>(R.id.time_min).text.toString().toInt()}
                catch(e : NumberFormatException){
                    0
                }


                // Update the database with the new profile information.
                db.collection("users")
                    .where(Filter.equalTo("uid", uid))
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot.size() > 1) {
                            //error("There is more than 1 user in the database with the UID ${user["uid"]}!")
                        }
                        db.document("users/$uid")
                            .update("profileName", (findViewById<EditText>(R.id.profileName).text).toString(),
                                "profileLocation", (findViewById<EditText>(R.id.profileLocation).text).toString(),
                                "profileDescription", (findViewById<EditText>(R.id.profileDescription).text).toString(),
                                "profileCurrentActivity", (findViewById<EditText>(R.id.profileCurrentActivity).text).toString(),
                                "currentMin", current_tiem,
                                "currentTime", Clock.System.now().toString()
                            )

                            .addOnSuccessListener {
                                Log.d(TAG, "Document at users/$uid.id} successfully updated.")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error updating document", e)
                            }
                        db.document("users/$uid").update("categories", chosenCats.toList())
                            .addOnSuccessListener {
                                Log.d(TAG, "Categories saved: $chosenCats")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error saving categories", e)
                            }}
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error finding document", e)
                    }
                }
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
        findViewById<LinearLayout>(R.id.nav_schedule).setOnClickListener {
            startActivity(Intent(this, ScheduleActivity::class.java))
        }

        // Handles selecting an image from images and applying it to the profile picture.
        val pickMedia = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()) { uri ->
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            if (uri != null) {
                Log.d("PhotoPicker", "Selected URI: $uri")
                pfp.setImageURI(uri)
                // pfp currently scales selected photo down but does not
                // adjust aspect ratio, cropping photo if it doesn't fit perfectly

                val bitmapScaled = getProfilePictureAsBitmap()
                // Use the scaled bitmap as the profile picture
                pfp.setImageBitmap(bitmapScaled)

                changedPfp = true // set flag

            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }

        // Handles running the camera and retrieving the photo
        val useCamera = registerForActivityResult(
            ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                Log.d("Camera", "Bitmap photo captured")
                val bitmapScaled = Bitmap.createScaledBitmap(
                    bitmap, UserSession.MAX_PFP_SIZE, UserSession.MAX_PFP_SIZE, true)
                pfp.setImageBitmap(bitmapScaled)

                changedPfp = true
            } else {
                Log.d("Camera", "No photo taken")
            }
        }

        // Upon clicking on the profile picture
        avatar.setOnClickListener {
            if (isEditing) { // Only allow editing in edit mode

                // Make a popup that prompts user to select or take a photo
                // Currently just uses an alert, we should probably make custom UI
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder
                    .setMessage("Update profile picture from:")
                    .setNegativeButton("Photos") { dialog, which ->
                        // Launch the photo picker and let the user choose only images.
                        pickMedia.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                    .setPositiveButton("Camera") { dialog, which ->
                        // Launch the camera.
                        useCamera.launch(null)
                    }
                    .setNeutralButton("Cancel") { dialog, which ->
                        dialog.cancel()
                    }

                val dialog: AlertDialog = builder.create()
                dialog.show()
            }
        }
    }

    fun getData(uid : String, names : Array<String>) {
        Log.w(TAG, "WE ran the function")
        //Log.w(TAG, "We ran the coroutine")


        val returnable = names.clone()
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                for (x in names.indices){
                    Log.w("for ref", names[x])
                    Log.w("grrrrrrrr", userDoc.get(names[x]).toString())
                    if(x==4)
                        returnable[4] = userDoc.get(names[x]).toString()
                    else
                        returnable[x] = userDoc.get(names[x])?.toString() ?: ""
                   }
                if(returnable[0] == "null") returnable[0] = "Your name here"

                findViewById<EditText>(R.id.profileName).setText(returnable[0])
                findViewById<EditText>(R.id.profileLocation).setText(returnable[1])
                findViewById<EditText>(R.id.profileDescription).setText(returnable[2])
                try{
                    findViewById<EditText>(R.id.time_hour).setText((returnable[4].toInt() / 60).toString())
                    findViewById<EditText>(R.id.time_min).setText((returnable[4].toInt() % 60).toString())}
                catch ( e : NumberFormatException){
                    Log.w("Error", e.toString())
                    Log.w("Error", returnable[4])
                }
                catch ( e : Resources.NotFoundException){
                    Log.w("Error", e.toString())
                    Log.w("Error", returnable[4])
                }


                val activityLabel = findViewById<TextView>(R.id.current_activity_label)
                val activityField = findViewById<EditText>(R.id.profileCurrentActivity)
                //logic to hide current profile activity if not specified
                if (returnable[3].isEmpty()) {
                    activityLabel.visibility = android.view.View.GONE
                    activityField.visibility = android.view.View.GONE
                } else {
                    activityLabel.visibility = android.view.View.VISIBLE
                    activityField.visibility = android.view.View.VISIBLE
                    activityField.setText(returnable[3])
                }

                Log.w(TAG, "This is the last thing before the crash")
                findViewById<Button>(R.id.avatar_1).setText(returnable[0].split(" ").take(2).joinToString(""){ it.first().uppercase() } )

                Log.w(TAG, "We have docs")
                time_check(uid.toString())
            }

        Log.w(TAG, "We got out of the loop")




    }
    fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { word ->
            word.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }

    fun getCats(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                var stringCats = userDoc.get("categories").toString()
                stringCats = stringCats.removeSurrounding("[", "]")
                val tempCats = stringCats.split(", ")
                for (x in tempCats) {
                    Log.w(TAG + " getCats", x)
                    if (x.isNotEmpty() && x !in chosenCats) chosenCats.add(x)
                }
                Log.w(TAG, "We have docs $stringCats")
                setupChips()
                refreshDisplayChips()
                refreshActivitiesEditor(false)
            }
    }
    private fun refreshDisplayChips() {
        val container = findViewById<LinearLayout>(R.id.profile_chips_display)
        container.removeAllViews()
        val dp = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val horizontalPadding = (64 * dp).toInt()

        var currentRow: LinearLayout? = null
        var currentRowWidth = 0

        chosenCats.filter { it.isNotEmpty() && it != "null" }.forEach { label ->
            val chip = TextView(this)
            chip.text = label.replaceFirstChar { it.uppercase() }
            chip.setTextColor(Color.parseColor("#8888A4"))
            chip.textSize = 11f
            chip.maxLines = 1
            chip.isSingleLine = true
            chip.setBackgroundColor(Color.parseColor("#0D0D14"))
            val px9 = (9 * dp).toInt(); val px5 = (5 * dp).toInt()
            chip.setPadding(px9, px5, px9, px5)

            // Set layout params BEFORE measuring
            val chipLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            chipLp.marginEnd = (7 * dp).toInt()
            chip.layoutParams = chipLp

            // Use AT_MOST so measuredWidth returns the actual rendered width
            chip.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(screenWidth, android.view.View.MeasureSpec.AT_MOST),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            )
            val chipWidth = chip.measuredWidth + chipLp.marginEnd

            if (currentRow == null || currentRowWidth + chipWidth > screenWidth - horizontalPadding) {
                currentRow = LinearLayout(this)
                currentRow!!.orientation = LinearLayout.HORIZONTAL
                val rowLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                rowLp.bottomMargin = (4 * dp).toInt()
                currentRow!!.layoutParams = rowLp
                container.addView(currentRow)
                currentRowWidth = 0
            }

            currentRow!!.addView(chip)
            currentRowWidth += chipWidth
        }
    }
    private fun setupChips() {
        val container = findViewById<LinearLayout>(R.id.profile_chip_container)
        container.removeAllViews()

        val categories = UserSession.allCategories.ifEmpty {
            listOf("movies","music","food","restaurants","games","reading","travel","hiking","disc golf","coding","merge dragons")
        }
        val dp = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val horizontalPadding = (32 * dp).toInt()

        var currentRow: LinearLayout? = null
        var currentRowWidth = 0

        categories.forEach { label ->
            val displayLabel = label.replaceFirstChar { it.uppercase() }

            val chip = TextView(this)
            chip.text = displayLabel
            chip.textSize = 13f
            chip.maxLines = 1
            chip.isSingleLine = true
            val px14 = (14 * dp).toInt()
            val px9 = (9 * dp).toInt()
            chip.setPadding(px14, px9, px14, px9)

            if (label in chosenCats) {
                chip.setTextColor(Color.parseColor("#2ECC71"))
                chip.setBackgroundColor(Color.parseColor("#222ECC71"))
            } else {
                chip.setTextColor(Color.parseColor("#8888A4"))
                chip.setBackgroundColor(Color.parseColor("#16161F"))
            }

            chip.setOnClickListener {
                if (label in chosenCats) {
                    chosenCats.remove(label)
                    chip.setTextColor(Color.parseColor("#8888A4"))
                    chip.setBackgroundColor(Color.parseColor("#16161F"))
                    activitiesMap.remove(label) //removes the associated activities when category is removed
                    db.document("users/$uid").update("activities", activitiesMap as Map<String, List<String>>)
                } else {
                    chosenCats.add(label)
                    chip.setTextColor(Color.parseColor("#2ECC71"))
                    chip.setBackgroundColor(Color.parseColor("#222ECC71"))
                }
                refreshDisplayChips()
                refreshActivitiesEditor(isEditing)
            }

            // Set layout params BEFORE measuring
            val chipLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            chipLp.marginEnd = (10 * dp).toInt()
            chip.layoutParams = chipLp

            // Measure with AT_MOST so the TextView has a real bound to measure against
            chip.measure(
                android.view.View.MeasureSpec.makeMeasureSpec(screenWidth, android.view.View.MeasureSpec.AT_MOST),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            )
            val chipWidth = chip.measuredWidth + chipLp.marginEnd

            // Start a new row if this chip won't fit
            if (currentRow == null || currentRowWidth + chipWidth > screenWidth - horizontalPadding) {
                currentRow = LinearLayout(this)
                currentRow!!.orientation = LinearLayout.HORIZONTAL
                val rowLp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                rowLp.bottomMargin = (8 * dp).toInt()
                currentRow!!.layoutParams = rowLp
                container.addView(currentRow)
                currentRowWidth = 0
            }

            currentRow!!.addView(chip)
            currentRowWidth += chipWidth
        }
    }
    // Fetches the user's saved activities from Firestore and populates the local activitiesMap
    private fun loadActivities(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val raw = doc.get("activities")
                if (raw is Map<*, *>) {
                    raw.forEach { (k, v) ->
                        // Only process entries where key is a String and value is a List, caused bug before present
                        if (k is String && v is List<*>) {
                            activitiesMap[k] = v.filterIsInstance<String>().toMutableList()
                        }
                    }
                }
                // Refresh the UI on the main thread after loading data
                runOnUiThread { refreshActivitiesEditor(false) }
            }
    }
    // Rebuilds the activities editor UI. Pass editing=true to show input rows,
    // remove buttons, and activity count badges.
    fun refreshActivitiesEditor(editing: Boolean = false) {
        val container = findViewById<LinearLayout>(R.id.activities_editor_container)
        container.removeAllViews()
        val dp = resources.displayMetrics.density

        // Show a placeholder if the user hasn't selected any interest categories yet
        if (chosenCats.isEmpty()) {
            val empty = TextView(this)
            empty.text = "Select some interests first"
            empty.setTextColor(Color.parseColor("#8888A4"))
            empty.textSize = 13f
            empty.setPadding(0, (8 * dp).toInt(), 0, 0)
            container.addView(empty)
            return
        }
        // Iterate over each valid selected category and build its section
        chosenCats.filter { it.isNotEmpty() && it != "null" }.forEach { cat ->
            val catActivities = activitiesMap.getOrPut(cat) { mutableListOf() }

            // Category Header Row
            val catHeader = LinearLayout(this)
            catHeader.orientation = LinearLayout.HORIZONTAL
            catHeader.gravity = android.view.Gravity.CENTER_VERTICAL
            val headerLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            headerLp.topMargin = (14 * dp).toInt()
            headerLp.bottomMargin = (6 * dp).toInt()
            catHeader.layoutParams = headerLp
            catHeader.setBackgroundColor(Color.parseColor("#1E1E2A"))
            catHeader.setPadding((10 * dp).toInt(), (8 * dp).toInt(), (10 * dp).toInt(), (8 * dp).toInt())
            // Category name label ("Sports", "Music")
            val catTitle = TextView(this)
            catTitle.text = cat.replaceFirstChar { it.uppercase() }
            catTitle.setTextColor(Color.WHITE)
            catTitle.textSize = 13f
            catTitle.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            catHeader.addView(catTitle)
            // Badge showing how many activities; only visible in edit mode
            val countBadge = TextView(this)
            countBadge.setTextColor(Color.parseColor("#2ECC71"))
            countBadge.textSize = 11f
            countBadge.text = "${catActivities.size} added"
            catHeader.addView(countBadge)
            countBadge.visibility = if (editing) android.view.View.VISIBLE else android.view.View.GONE
            container.addView(catHeader)

            val chipsContainer = LinearLayout(this)
            chipsContainer.orientation = LinearLayout.VERTICAL
            chipsContainer.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            // Rebuilds all chip rows from scratch (called on add/remove or expand/collapse)
            fun rebuildChips() {
                chipsContainer.removeAllViews()
                catActivities.forEachIndexed { index, act ->
                    // Each activity gets its own horizontal row
                    val rowChip = LinearLayout(this)
                    rowChip.orientation = LinearLayout.HORIZONTAL
                    rowChip.gravity = android.view.Gravity.CENTER_VERTICAL
                    val rowLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    rowLp.bottomMargin = (6 * dp).toInt()
                    rowChip.layoutParams = rowLp
                    rowChip.setBackgroundColor(if (editing) Color.parseColor("#222ECC71") else Color.parseColor("#0D0D14"))

                    // Collapse rows beyond the first 2 by default when there are more than 2
                    if (index >= 2 && catActivities.size > 2) {
                        rowChip.visibility = android.view.View.GONE
                    }

                    val chip = TextView(this)
                    chip.text = act
                    chip.textSize = 11f
                    val px8 = (8 * dp).toInt(); val px4 = (4 * dp).toInt()
                    chip.setPadding(px8, px4, px8, px4)
                    chip.setTextColor(if (editing) Color.parseColor("#2ECC71") else Color.parseColor("#8888A4"))
                    chip.setBackgroundColor(Color.TRANSPARENT)
                    chip.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    rowChip.addView(chip)
                    // In edit mode, show a remove (✕) button next to each activity
                    if (editing) {
                        val removeBtn = TextView(this)
                        removeBtn.text = "✕"
                        removeBtn.setTextColor(Color.parseColor("#2ECC71"))
                        removeBtn.textSize = 11f
                        val px8b = (8 * dp).toInt()
                        removeBtn.setPadding(px8b, 0, px8b, 0)
                        removeBtn.setOnClickListener {
                            catActivities.remove(act)
                            countBadge.text = "${catActivities.size} added"
                            rebuildChips()
                        }
                        rowChip.addView(removeBtn)
                    }

                    chipsContainer.addView(rowChip)
                }

                // Show a "▼ N more" toggle row when there are more than 2 activities
                if (catActivities.size > 2) {
                    val toggleRow = TextView(this)
                    val hiddenCount = catActivities.size - 2
                    toggleRow.text = "▼  $hiddenCount more"
                    toggleRow.setTextColor(Color.parseColor("#8888A4"))
                    toggleRow.textSize = 11f
                    val px8 = (8 * dp).toInt(); val px4 = (4 * dp).toInt()
                    toggleRow.setPadding(px8, px4, px8, px4)
                    var expanded = false
                    toggleRow.setOnClickListener {
                        expanded = !expanded
                        for (i in 2 until chipsContainer.childCount - 1) {
                            chipsContainer.getChildAt(i).visibility =
                                if (expanded) android.view.View.VISIBLE else android.view.View.GONE
                        }
                        toggleRow.text = if (expanded) "▲  show less" else "▼  $hiddenCount more"
                    }
                    chipsContainer.addView(toggleRow)
                }
                // Show a placeholder message if no activities have been added yet
                if (catActivities.isEmpty()) {
                    val placeholder = TextView(this)
                    placeholder.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    placeholder.text = "No activities yet"
                    placeholder.setTextColor(Color.parseColor("#555566"))
                    placeholder.textSize = 11f
                    chipsContainer.addView(placeholder)
                }
            }

            rebuildChips()
            container.addView(chipsContainer)
            // Add Activity Input Row (only shown in edit mode)
            val inputRow = LinearLayout(this)
            inputRow.orientation = LinearLayout.HORIZONTAL
            inputRow.gravity = android.view.Gravity.CENTER_VERTICAL
            val inputLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            inputLp.topMargin = (6 * dp).toInt()
            inputRow.layoutParams = inputLp
            // Text field for typing a new activity name
            val input = EditText(this)
            input.hint = "Add activity for ${cat.replaceFirstChar { it.uppercase() }}..."
            input.setHintTextColor(Color.parseColor("#555566"))
            input.setTextColor(Color.WHITE)
            input.textSize = 12f
            input.setBackgroundColor(Color.parseColor("#1A1A26"))
            val px10 = (10 * dp).toInt(); val px7 = (7 * dp).toInt()
            input.setPadding(px10, px7, px10, px7)
            input.inputType = android.text.InputType.TYPE_CLASS_TEXT
            input.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            // button to confirm adding a new activity
            val addBtn = Button(this)
            addBtn.text = "+"
            addBtn.setTextColor(Color.WHITE)
            addBtn.textSize = 16f
            addBtn.setBackgroundColor(Color.parseColor("#2ECC71"))
            val btnLp = LinearLayout.LayoutParams((40 * dp).toInt(), (40 * dp).toInt())
            btnLp.marginStart = (6 * dp).toInt()
            addBtn.layoutParams = btnLp
            addBtn.setPadding(0, 0, 0, 0)

            addBtn.setOnClickListener {
                val text = input.text.toString().trim()
                if (text.isNotEmpty() && text !in catActivities) {
                    catActivities.add(text)
                    activitiesMap[cat] = catActivities
                    input.setText("")
                    countBadge.text = "${catActivities.size} added"
                    rebuildChips()
                }
            }
            inputRow.addView(input)
            inputRow.addView(addBtn)
            // Only attach the input row to the container when in edit mode
            if (editing) container.addView(inputRow)
        }
    }
    fun test(){
        val x= 2
        val tiem = LocalDateTime(2026,4,17,10,24,0,0)
        val past = LocalDateTime(1985,12,21,6,34,2,0)
        val currentTime = Clock.System.now()
        val current2 = currentTime.plus(x, DateTimeUnit.MINUTE)
        val thing = tiem.toInstant(TimeZone.currentSystemDefault()) - currentTime
        val thing2 = past.toInstant(TimeZone.currentSystemDefault()) - currentTime
        if(thing.isNegative())
            //delete
            Log.w("please don't hate me", "no hate please")
        Log.w(TAG, currentTime.toString())
        Log.w(TAG, "$tiem $thing $thing2")
        val thing3 = Instant.parse("2026-04-17T19:42:52.615602Z")
        //I hate when malicious hacker inject nanoseconds into my birthday
    }
    fun time_check(uid : String){
        val mins = try {findViewById<EditText>(R.id.time_min).text.toString().toInt()}
        catch (e : NumberFormatException) {0}
        val hours = try {findViewById<EditText>(R.id.time_hour).text.toString().toInt()}
        catch (e : NumberFormatException) {0}

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->
                val raw = userDoc.get("currentTime").toString()

                var current = if( raw == "null" || raw == "0") Instant.parse("2021-04-17T19:42:52.615602Z")
                        else(Instant.parse(raw))
                current = current.plus(mins,DateTimeUnit.MINUTE)
                current = current.plus(hours,DateTimeUnit.HOUR)
                val now = Clock.System.now()
                Log.w("times", current.toString())
                Log.w("times", (current-now).toString() )
                if ((current-now).isNegative()){
                    db.document("users/$uid").update("currentTime", 0,"currentMin", 0, "profileCurrentActivity", "")
                    findViewById<EditText>(R.id.time_min).setText("0")
                    findViewById<EditText>(R.id.time_hour).setText("0")
                    findViewById<EditText>(R.id.profileCurrentActivity).setText("")
                    for (field in arrayOf("currentTime","currentMin","profileCurrentActivity")){

                    Log.w("DeleteCurrentActivity", field)}

                        }

                Log.w("var_dump","mins: $mins hours: $hours current: $current now: $now raw: $raw" )

            }



            }

    }


