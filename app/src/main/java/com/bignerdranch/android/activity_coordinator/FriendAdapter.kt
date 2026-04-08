package com.bignerdranch.android.activity_coordinator

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


//Class manager. Bridges the gap between the raw Friend data and the RecyclerView UI, coordinates creation and recycling of profile cards.
class FriendAdapter(
    private var friends: List<Friend>,
    private val isSearchMode: Boolean, //Lets the adapter know whether the user is looking at the Filter activity or Friend Search Activity
    private val onAddClick: ((Friend) -> Unit)? = null
) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    //Container class holding the actual references to the views (TextViews, Layouts) inside a single profile card
    class FriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.friend_name)
        val avatar: TextView = view.findViewById(R.id.friend_avatar)
        val pfp: ImageView = view.findViewById(R.id.friend_pfp)
        val bio: TextView = view.findViewById(R.id.friend_bio)
        val location: TextView = view.findViewById(R.id.friend_location)
        val categoriesContainer: LinearLayout = view.findViewById(R.id.friend_interests_container)
        val actionButton: Button = view.findViewById(R.id.btn_add_friend) //Add friend button w/visibility dependent on circumstances
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
        holder.avatar.text = friend.name.split(" ").take(2).joinToString("") { it.first().uppercase() }
        holder.location.text = friend.location
        holder.bio.text = friend.bio

        //All of the shit for category chips
        holder.categoriesContainer.removeAllViews()
        holder.categoriesContainer.orientation = LinearLayout.VERTICAL

        val screenWidth = context.resources.displayMetrics.widthPixels
        val containerWidth = screenWidth - (64 * dp).toInt() // account for card padding
        var currentRow: LinearLayout? = null
        var currentRowWidth = 0
        var rowCount = 0

        friend.categories.forEach { category ->
            val chip = TextView(context)
            chip.text = category.replaceFirstChar { it.uppercase() }
            chip.setTextColor(Color.parseColor("#8888A4"))
            chip.textSize = 11f
            chip.isSingleLine = true
            chip.maxLines = 1
            chip.setBackgroundColor(Color.parseColor("#0D0D14"))
            chip.setPadding((9 * dp).toInt(), (5 * dp).toInt(), (9 * dp).toInt(), (5 * dp).toInt())

            val chipLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            chipLp.marginEnd = (7 * dp).toInt()
            chip.layoutParams = chipLp

            chip.measure(
                View.MeasureSpec.makeMeasureSpec(containerWidth, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            val chipWidth = chip.measuredWidth + chipLp.marginEnd

            if (currentRow == null || currentRowWidth + chipWidth > containerWidth) {
                if (rowCount >= 2) return@forEach
                currentRow = LinearLayout(context)
                currentRow!!.orientation = LinearLayout.HORIZONTAL
                val rowLp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                rowLp.bottomMargin = (4 * dp).toInt()
                currentRow!!.layoutParams = rowLp
                holder.categoriesContainer.addView(currentRow)
                currentRowWidth = 0
                rowCount++
            }

            currentRow!!.addView(chip)
            currentRowWidth += chipWidth
        }

        //"Add friend" button tracking for the UI
        val addBtn = holder.itemView.findViewById<Button>(R.id.btn_add_friend)
        addBtn.setOnClickListener {
            onAddClick?.invoke(friend) //Tell the activity which friend was added
            addBtn.text = "✓ Added"
            addBtn.isEnabled = false //Disable the button immediately so it can't be spammed
        }

        //If the user is in the FriendSearch activity, show the Add Friend button on user's profile
        if (isSearchMode) {
            holder.actionButton.visibility = View.VISIBLE
            holder.actionButton.text = "+ Add"
            holder.actionButton.isEnabled = true //CRITICAL re-enable add button if user intends to add multiple friends in one go
            holder.actionButton.setOnClickListener {
                onAddClick?.invoke(friend)
                holder.actionButton.text = "✓ Added"
                holder.actionButton.isEnabled = false
            }
        } else {
            holder.actionButton.visibility = View.GONE //Not on the search activity, hide the add friend button
        }

        // Set friend profile picture
        holder.pfp.setImageBitmap(UserSession.getPfp(friend.id))
    }

    override fun getItemCount() = friends.size //Tells the RecyclerView how many total items are in a user's friends list

    //Refreshes shown friends list after filtering and triggers the UI to redraw itself
    fun updateData(newFriends: List<Friend>) {
        friends = newFriends
        notifyDataSetChanged()
    }
}