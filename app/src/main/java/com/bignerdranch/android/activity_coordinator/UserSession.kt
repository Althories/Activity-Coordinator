package com.bignerdranch.android.activity_coordinator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage

//Variables in this file stay alive as long as the app process is running
object UserSession {
    //This variable stays alive as long as the app process is running
    var currentUserId: String? = null

    // vv Branden
    // Maps user IDs to bitmaps
    var loadedPfps: MutableMap<Int, Bitmap> = mutableMapOf()

    const val MAX_PFP_SIZE = 200
    val storage = Firebase.storage // Firebase cloud storage, where all picture assets live
    val db = Firebase.firestore

    // Returns a given user's profile picture as bitmap. If it isn't saved, download it.
    // Defaults to the currently logged in user.
    fun getPfp(uid: String? = null): Bitmap? {
        // default variable is immutable so i need a mutable one
        var uidToFetch: Int = 0
        var returnedPfp: Bitmap? = null
        // By default we should use the current user's id
        if (uid == null) {
            if (currentUserId == null) {
                return null
            }
            uidToFetch = currentUserId!!.toInt()
        } else { // otherwise use what was inputted
            uidToFetch = uid.toInt()
        }
        // if the pfp is already loaded dont bother fetching it
        if (uidToFetch in loadedPfps) {
            return loadedPfps[uidToFetch]
        } else {
            val pfpRef = storage.reference.child(
                "userProfilePictures/$uidToFetch.jpg"
            )
            pfpRef.getBytes((100000).toLong()) // Hardcoded to 100kb
                .addOnSuccessListener { bytes ->
                    returnedPfp = (BitmapFactory
                        .decodeByteArray(bytes, 0, bytes.size))
                    if (returnedPfp != null) {
                        loadedPfps[uidToFetch] = returnedPfp
                    }
                    Log.d("UserSession", "Loaded $uidToFetch.jpg")
                }.addOnFailureListener { e ->
                    Log.w("UserSession", "Failed to fetch profile pic", e)
                }
            return returnedPfp
        }
    }

    // Update given loaded user's pfp with a given bitmap.
    // By default, update the logged in user.
    fun updatePfpLocally(img: Bitmap, uid: String? = null) {
        if (uid == null) {
            loadedPfps[currentUserId!!.toInt()] = img
        } else {
            loadedPfps[uid.toInt()] = img
        }
    }

    // Update EVERY user's profile picture.
    // Downloads every single user's profile picture if it isn't already downloaded,
    // so not ideal. But FriendAdapter already does this.
    fun updateUserPfps() {
        db.collection("users").get()
        .addOnSuccessListener { allDocs ->
            for (doc in allDocs) {
                getPfp(doc.id)
            }
        }
    }
}