package com.bignerdranch.android.activity_coordinator

import android.graphics.Bitmap

//Helper data class for FriendAdapter and FilterActivity.
data class Friend(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val bio: String = "",
    val currentActivity: String = "",
    val pfp: Bitmap? = null,
    val categories: List<String> = emptyList(),
    val exactNameSearch: Boolean = false
)
