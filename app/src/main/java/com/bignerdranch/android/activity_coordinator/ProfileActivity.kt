package com.bignerdranch.android.activity_coordinator

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        //List of all editable profile text fields
        val editableFields = listOf(
            findViewById<EditText>(R.id.profileName),
            findViewById<EditText>(R.id.profileLocation),
            findViewById<EditText>(R.id.profileDescription)
        )

        val editButton = findViewById<Button>(R.id.edit_profile_button)
        var isEditing = false //Tells EditViews how they should look based on activity state

        //Controls whether EditViews are editable. editing boolean has the same T/F state as isEditing
        fun applyEditState(editing: Boolean) {
            editableFields.forEach { field ->
                field.isFocusableInTouchMode = editing
                field.isFocusable = editing
                field.background.alpha = if (editing) 255 else 0 //Determines edit drawable visibility
                if (!editing) field.clearFocus() //Hides edit bar drawable
            }
            editButton.text = if (editing) "Save" else "Edit"
        }

        //applyEditState is run ONCE at start of program to hide EditView bars in initial non-edit state
        applyEditState(false)

        //Edit mode button for profile page
        editButton.setOnClickListener {
            isEditing = !isEditing //Toggles edit state
            applyEditState(isEditing)
            }
        }
    }