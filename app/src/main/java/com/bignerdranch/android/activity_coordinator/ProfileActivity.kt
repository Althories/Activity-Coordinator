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

import kotlin.collections.joinToString
import kotlin.collections.take
import kotlin.text.split


class ProfileActivity : AppCompatActivity() {
    var uid = UserSession.currentUserId
    var db = Firebase.firestore
    var TAG = "ProfileActivity"
    var chosenCats = mutableSetOf<String>()
    var subCats = mutableSetOf<String>()
    var addCats = mutableSetOf<String>()
    val storage = Firebase.storage // Firebase cloud storage, where all picture assets live

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        getData(uid.toString(),arrayOf("profileName","profileLocation","profileDescription"))

        getCats(uid.toString())
        ///                            Log.w("BHBDUIBHDIKBJ", temp.toString())
        ////                           val returned = csvToText("merge_dragons,cats,morger,music")
        findViewById<Button>(R.id.btn_logout).setOnClickListener {
            // Clear the user session
            UserSession.currentUserId = null
            // Go back to login and clear the back stack
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK //starts the activity in a fresh task, and FLAG_ACTIVITY_CLEAR_TASK wipes out every activity that was in the back stack
            startActivity(intent) //launches MainActivity with the flags applied
            finish() //Closes ProfileActivity

        }




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
            findViewById<EditText>(R.id.profileDescription)


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
        var isEditing = false //Tells EditViews how they should look based on activity state

        //Controls whether EditViews are editable. editing boolean has the same T/F state as isEditing
        fun applyEditState(editing: Boolean) {
            editableFields.forEach { field ->
                field.isFocusableInTouchMode = editing
                field.isFocusable = editing
                field.background.alpha = if (editing) 255 else 0 //Determines edit drawable visibility
                if (!editing) field.clearFocus() //Hides edit bar drawable
            }
            findViewById<LinearLayout>(R.id.bottom_sheet).visibility = if (editing) android.view.View.VISIBLE else android.view.View.GONE
            editButton.text = if (editing) "Save" else "Edit"
            // TODO: Should probably have something that indicates you can edit the profile pic
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
                var temp = 0
                findViewById<EditText>(R.id.chip1).setText("")
                findViewById<EditText>(R.id.chip2).setText("")
                findViewById<EditText>(R.id.chip3).setText("")
                findViewById<EditText>(R.id.chip3).visibility = android.view.View.GONE
                findViewById<EditText>(R.id.chip2).visibility = android.view.View.GONE
                findViewById<EditText>(R.id.chip1).visibility = android.view.View.GONE

                for(x in chosenCats) {  //TODO End Hardcoding
                    if (temp == 2 && x != "") {
                        findViewById<EditText>(R.id.chip3).setText(x)
                        findViewById<EditText>(R.id.chip3).visibility = android.view.View.VISIBLE
                        temp += 1
                    }
                    if (temp == 1 && x != "") {
                        findViewById<EditText>(R.id.chip2).setText(x)
                        findViewById<EditText>(R.id.chip2).visibility = android.view.View.VISIBLE
                        temp += 1
                    }
                    if (temp == 0 && x != "") {
                        findViewById<EditText>(R.id.chip1).setText(x)
                        findViewById<EditText>(R.id.chip1).visibility = android.view.View.VISIBLE
                        temp += 1
                    }
                }

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
                                "profileDescription", (findViewById<EditText>(R.id.profileDescription).text).toString())
                            .addOnSuccessListener {
                                Log.d(TAG, "Document at users/$uid.id} successfully updated.")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error updating document", e)
                            }


                        for (cat in addCats) {
                        db.document("users/$uid").update("categories", FieldValue.arrayUnion(cat))
                            Log.w(TAG,cat)
                        }
                        for (cat in subCats) {
                            db.document("users/$uid").update("categories", FieldValue.arrayRemove(cat))
                            Log.w(TAG,cat)
                        }
                    }
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
                for (x in names.indices)
                    returnable[x] = userDoc.get(names[x]).toString()
                if(returnable[0] == "null") returnable[0] = "Your name here"
                findViewById<EditText>(R.id.profileName).setText(returnable[0])
                findViewById<EditText>(R.id.profileLocation).setText(returnable[1])
                findViewById<EditText>(R.id.profileDescription).setText(returnable[2])
                Log.w(TAG, "This is the last thing before the crash")
                findViewById<Button>(R.id.avatar_1).setText(returnable[0].split(" ").take(2).joinToString(""){ it.first().uppercase() } )

                Log.w(TAG, "We have docs")
            }

        Log.w(TAG, "We got out of the loop")




    }
    fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { word ->
            word.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
        }

    fun getCats(uid : String){
        db.collection("users").document(uid).get()
            .addOnSuccessListener { userDoc ->

                var stringCats = userDoc.get("categories").toString()
                stringCats = stringCats.removeSurrounding("[","]")
                val tempCats = stringCats.split(", ")
                for (x in tempCats) {
                    Log.w(TAG+" getCats",x)
                    if(x !in chosenCats) chosenCats.add(x)
                }
                var temp = 0
                findViewById<EditText>(R.id.chip3).visibility = android.view.View.GONE
                findViewById<EditText>(R.id.chip2).visibility = android.view.View.GONE
                findViewById<EditText>(R.id.chip1).visibility = android.view.View.GONE
                for(x in chosenCats) {  //TODO end hardcoding
                    if (temp == 2 && x != "") {
                        findViewById<EditText>(R.id.chip3).setText(x)
                        findViewById<EditText>(R.id.chip3).visibility = android.view.View.VISIBLE
                        temp += 1
                    }
                    if (temp == 1 && x != "") {
                        findViewById<EditText>(R.id.chip2).setText(x)
                        findViewById<EditText>(R.id.chip2).visibility = android.view.View.VISIBLE
                        temp += 1
                    }
                    if (temp == 0 && x != "") {
                        findViewById<EditText>(R.id.chip1).setText(x)
                        findViewById<EditText>(R.id.chip1).visibility = android.view.View.VISIBLE
                        temp += 1
                    }
                }
                Log.w(TAG, "We have docs $stringCats")
                setupChips()
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
            if (label !in chosenCats) {
                chip.setTextColor(Color.parseColor("#8888A4"))
                chip.setBackgroundColor(Color.parseColor("#16161F"))
            } else {
                chip.setTextColor(Color.parseColor("#2ECC71"))
                chip.setBackgroundColor(Color.parseColor("#222ECC71"))
            }
            chip.setOnClickListener {
                if (label in chosenCats) {
                    // Already selected, deselect it and reset to gray
                    chosenCats.remove(label)
                    if (label in addCats) {addCats.remove(label)}
                    else  {subCats.add(label)}
                    chip.setTextColor(Color.parseColor("#8888A4"))
                    chip.setBackgroundColor(Color.parseColor("#16161F"))
                } else {
                    // Not selected — select it and highlight green
                    chosenCats.add(label)
                    addCats.add(label)
                    chip.setTextColor(Color.parseColor("#2ECC71"))
                    chip.setBackgroundColor(Color.parseColor("#222ECC71"))
                }
                // Rebuild the active filter pills row to reflect the new state




            }
        }
    }


}
