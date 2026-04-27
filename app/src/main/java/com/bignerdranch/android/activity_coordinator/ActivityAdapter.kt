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

    class ActivityViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.friend_name)
        val location: TextView = view.findViewById(R.id.friend_location)
        val description: TextView = view.findViewById(R.id.friend_bio)
        val addActivityButton: Button = view.findViewById(R.id.btn_add_friend)
        val avatar: TextView = view.findViewById(R.id.friend_avatar)
        val currentActivityLabel: TextView = view.findViewById(R.id.friend_current_activity_label)
        val currentActivity: TextView = view.findViewById(R.id.friend_current_activity)

        init {
            currentActivityLabel.visibility = View.GONE
            currentActivity.visibility = View.GONE
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        //Reuses item_friend.xml for now since the layout is identical
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return ActivityViewHolder(view)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val event = activities[position]
        val currentUid = UserSession.currentUserId ?: "" //necessary reference to currentUserId to check who's joining an event

        holder.name.text = event.eventName



        //mini block for combining inviter name and location
        val inviteText = "Invited by ${event.creatorName.ifEmpty { "a Friend" }}"
        val locationText = event.eventLocation.ifEmpty { "TBD" }
        holder.location.text = "$inviteText • $locationText"
        holder.location.setTextColor(android.graphics.Color.parseColor("#8888A4")) //color connector dot for consistency

        holder.description.text = event.eventDescription
        holder.avatar.text = event.eventName.take(1).uppercase() //Basic interaction with activity, more work to remove the profile square altogether

        //Button starts visible. It was not showing up and I think it just needs a firm push
        holder.addActivityButton.visibility = View.VISIBLE

        //Block for Button UI updating. If user clicks on the join button at any point and is now on the joined list
        if (event.joinedUsers.contains(currentUid)) {
            holder.addActivityButton.text = "✓ Joined"
            holder.addActivityButton.isEnabled = false
            holder.addActivityButton.alpha = 0.5f
            holder.addActivityButton.setOnClickListener(null)
        } else {
            holder.addActivityButton.text = "Join"
            holder.addActivityButton.isEnabled = true
            holder.addActivityButton.alpha = 1.0f
            holder.addActivityButton.setOnClickListener {
                onJoinClick(event)
                //In-the-moment UI feedback upon user joining event
                holder.addActivityButton.text = "✓ Joined"
                holder.addActivityButton.isEnabled = false
                holder.addActivityButton.alpha = 0.5f
            }
        }
    }

    override fun getItemCount() = activities.size

    fun updateData(newActivities: List<ScheduledEvent>) {
        activities = newActivities
        notifyDataSetChanged()
    }
}