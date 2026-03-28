package com.bignerdranch.android.activity_coordinator

//Helper data class for FriendAdapter and FilterActivity.
data class Friend(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val bio: String = "",
    val categories: List<String> = emptyList()
)
