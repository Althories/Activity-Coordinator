package com.bignerdranch.android.activity_coordinator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ActivityAdapter(
    private var activities: List<ScheduledEvent>,
    private val onJoinClick: (ScheduledEvent) -> Unit
) : RecyclerView.Adapter<ActivityAdapter.ActivityViewHolder>() {

    //TEMP reusing IDs from FriendAdapter.kt. I should separate this later TODO
    class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.friend_name)
        val location: TextView = view.findViewById(R.id.friend_location)
        val description: TextView = view.findViewById(R.id.friend_bio)
        val actionButton: Button = view.findViewById(R.id.btn_add_friend)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        //Reuses item_friend.xml for now since the layout is identical
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val event = activities[position]

        holder.name.text = event.eventName
        holder.location.text = "Invited by ${event.creatorName.ifEmpty { "a Friend" }}" //TEMP changed to tell you who invited you
        holder.description.text = event.eventDescription

        holder.actionButton.text = "Join"
        holder.actionButton.visibility = View.VISIBLE
        holder.actionButton.setOnClickListener {
            onJoinClick(event)
        }
    }

    override fun getItemCount() = activities.size

    fun updateData(newActivities: List<ScheduledEvent>) {
        activities = newActivities
        notifyDataSetChanged()
    }
}