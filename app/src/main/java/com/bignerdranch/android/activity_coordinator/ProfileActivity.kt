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
import android.widget.LinearLayout


class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // ======================================================================================= //
        // The following code is boilerplate from Firebase to test that firestore is set up correctly.
        val db = Firebase.firestore
        val TAG = "ProfileActivity"
        /*
        // Create a new user with a first and last name
        val user = hashMapOf(
            "first" to "Ada",
            "last" to "Lovelace",
            "born" to 1815,
        )

        // Add a new document with a generated ID
        db.collection("users")
            .add(user)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }

        // Create a new user with a first, middle, and last name
        val user2 = hashMapOf(
            "first" to "Alan",
            "middle" to "Mathison",
            "last" to "Turing",
            "born" to 1912,
        )

        // Add a new document with a generated ID
        db.collection("users")
            .add(user2)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }

        db.collection("users")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d(TAG, "${document.id} => ${document.data}")
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents.", exception)
            }

         */
        // ======================================================================================= //
//Branden
        // Hardcoded example user - to be replaced with currently logged-in user
        val user = hashMapOf(
            "uid" to 1,
            "name" to "CooperPtacek",
            "email" to "cptacek@gustavus.edu",
            "categories"  to "music,hiking,disc golf"
        )

        // Check if user with given UID exists in the database and if not, add them
        db.collection("users")
            .where(Filter.equalTo("uid", user["uid"]))
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty){
                    db.collection("users")
                        .add(user)
                        .addOnSuccessListener { documentReference ->
                            Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Error adding document", e)
                        }
                } else {
                    Log.d(TAG, "User with UID ${user["uid"]} already exists")
                }
            }
//Branden ^
        //List of all editable profile text fields
        val editableFields = listOf(
            findViewById<EditText>(R.id.profileName),
            findViewById<EditText>(R.id.profileLocation),
            findViewById<EditText>(R.id.profileDescription)
        )


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
            editButton.text = if (editing) "Save" else "Edit"
        }

        //applyEditState is run ONCE at start of program to hide EditView bars in initial non-edit state
        applyEditState(false)

        //Edit mode button for profile page
        editButton.setOnClickListener {
            isEditing = !isEditing //Toggles edit state
            applyEditState(isEditing)
            if (!isEditing) { // upon pressing Save  BRANDEN
                Log.d(TAG, (findViewById<EditText>(R.id.profileName).text).toString())
                Log.d(TAG, (findViewById<EditText>(R.id.profileLocation).text).toString())
                Log.d(TAG, (findViewById<EditText>(R.id.profileDescription).text).toString())

                // Update the database with the new profile information.
                db.collection("users")
                    .where(Filter.equalTo("uid", user["uid"]))
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (querySnapshot.size() > 1) {
                            error("There is more than 1 user in the database with the UID ${user["uid"]}!")
                        }
                        db.document("users/${querySnapshot.documents[0].id}")
                            .update("profileName", (findViewById<EditText>(R.id.profileName).text).toString(),
                                "profileLocation", (findViewById<EditText>(R.id.profileLocation).text).toString(),
                                "profileDescription", (findViewById<EditText>(R.id.profileDescription).text).toString())
                            .addOnSuccessListener {
                                Log.d(TAG, "Document at users/${querySnapshot.documents[0].id} successfully updated.")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error updating document", e)
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
        }
}