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
        val currentUid = UserSession.currentUserId ?: "" //necessary ref to currentUserId to check who's joining an event

        holder.name.text = event.eventName
        holder.location.text = "Invited by ${event.creatorName.ifEmpty { "a Friend" }}" //TEMP changed to tell you who invited you
        holder.description.text = event.eventDescription

        //Button starts visible. It was not showing up and I think it just needs a firm push
        holder.actionButton.visibility = View.VISIBLE

        //If user clicks on the join button at any point and is now on the joined list
        if (event.joinedUsers.contains(currentUid)) {
            holder.actionButton.text = "✓ Joined"
            holder.actionButton.isEnabled = false
            holder.actionButton.alpha = 0.5f
            holder.actionButton.setOnClickListener(null)
        } else {
            holder.actionButton.text = "Join"
            holder.actionButton.isEnabled = true
            holder.actionButton.alpha = 1.0f
            holder.actionButton.setOnClickListener {
                onJoinClick(event)
                //In-the-moment UI feedback upon user joining event
                holder.actionButton.text = "✓ Joined"
                holder.actionButton.isEnabled = false
                holder.actionButton.alpha = 0.5f
            }
        }
    }

    override fun getItemCount() = activities.size

    fun updateData(newActivities: List<ScheduledEvent>) {
        activities = newActivities
        notifyDataSetChanged()
    }
}