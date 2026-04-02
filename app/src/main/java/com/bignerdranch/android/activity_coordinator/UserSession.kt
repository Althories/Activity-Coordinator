package com.bignerdranch.android.activity_coordinator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.storage.storage

//Variables in this file stay alive as long as the app process is running
object UserSession {
    //This variable stays alive as long as the app process is running
    var currentUserId: String? = null

    // vv Branden
    var pfp: Bitmap? = null

    const val MAX_PFP_SIZE = 200
    val storage = Firebase.storage // Firebase cloud storage, where all picture assets live

    // Download the user's profile picture from storage and save it as a variable here
    // idk if i should be using an intent for this but it works
    fun getUserPfp() {
        if (currentUserId == null) {
            return
        }
        val pfpRef = storage.reference.child(
            "userProfilePictures/$currentUserId.jpg")
        pfpRef.getBytes((MAX_PFP_SIZE * MAX_PFP_SIZE + 100).toLong())
            .addOnSuccessListener { bytes ->
                pfp = (BitmapFactory
                    .decodeByteArray(bytes, 0, bytes.size))
            }.addOnFailureListener { e ->
                Log.w("UserSession","Failed to fetch profile pic", e)
            }
    }
}