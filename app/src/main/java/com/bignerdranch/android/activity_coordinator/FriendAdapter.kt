package com.bignerdranch.android.activity_coordinator

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


//Class manager. Bridges the gap between the raw Friend data and the RecyclerView UI, coordinates creation and recycling of profile cards.
class FriendAdapter(private var friends: List<Friend>) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    //Container class holding the actual references to the views (TextViews, Layouts) inside a single profile card
    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.friend_name)
        val avatar: TextView = view.findViewById(R.id.friend_avatar)
        val bio: TextView = view.findViewById(R.id.friend_bio)
        val location: TextView = view.findViewById(R.id.friend_location)
        val categoriesContainer: LinearLayout = view.findViewById(R.id.friend_interests_container)
    }

    //Called when the RecyclerView needs a new card layout. It inflates the item_friend.xml and wraps it in a FriendViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    //Called every time a card is shown to the user. Takes the data for a specific Friend and puts it onto the ViewHolder's views.
    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = friends[position]
        val context = holder.itemView.context
        val dp = context.resources.displayMetrics.density

        // Standard text bindings
        holder.name.text = friend.name
        holder.avatar.text = friend.name.take(2).uppercase()
        holder.location.text = friend.location
        holder.bio.text = friend.bio

        //All of the shit for category chips
        holder.categoriesContainer.removeAllViews() //Clears the container cache because recyclerview needs to recycle
        friend.categories.forEach { category -> //Loop through the friend's categories
            val chip = TextView(context)
            //XML equivalent applications to be done for each chip
            chip.text = category
            chip.setTextColor(Color.parseColor("#8888A4"))
            chip.textSize = 11f
            chip.setBackgroundColor(Color.parseColor("#0D0D14"))
            val paddingSide = (9 * dp).toInt()
            val paddingTopBottom = (5 * dp).toInt()
            chip.setPadding(paddingSide, paddingTopBottom, paddingSide, paddingTopBottom)
            //Spacing between chips
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, (7 * dp).toInt(), 0)
            chip.layoutParams = params
            //Finish by adding chip to the horizontal layout
            holder.categoriesContainer.addView(chip)
        }
    }
    override fun getItemCount() = friends.size //Tells the RecyclerView how many total items are in a user's friends list

    //Refreshes shown friends list after filtering and triggers the UI to redraw itself
    fun updateData(newFriends: List<Friend>) {
        friends = newFriends
        notifyDataSetChanged()
    }
}