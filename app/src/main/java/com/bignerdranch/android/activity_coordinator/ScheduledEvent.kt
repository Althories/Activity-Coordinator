package com.bignerdranch.android.activity_coordinator

//Helper data class for ActivityAdapter and ScheduleActivity. Interacts with the database.
data class ScheduledEvent (
    val eventId: String = "",  //populated by document's ID
    val creatorId: String = "",
    val creatorName: String = "",
    val eventName: String = "",
    val eventDescription: String = "",
    val eventLocation: String = "", //TEMP currently replaced by creatorName "Invited by" message
    val invitedFriends: List<String> = emptyList(),
    val joinedUsers: List<String> = emptyList() //NEW: Track who clicked Join on activity
    )