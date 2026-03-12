package com.bignerdranch.android.activity_coordinator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge

class FilterActivity : AppCompatActivity() {

    private lateinit var bottomSheet: LinearLayout
    private lateinit var btnFilter: Button
    private lateinit var btnApplyFilter: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_filter)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.filter)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Hook up views
        bottomSheet = findViewById(R.id.bottom_sheet)
        btnFilter = findViewById(R.id.btn_filter)
        btnApplyFilter = findViewById(R.id.btn_apply_filter)

        // Open filters sheet
        btnFilter.setOnClickListener {
            bottomSheet.visibility = android.view.View.VISIBLE
            bottomSheet.translationY = bottomSheet.height.toFloat()
            bottomSheet.animate()
                .translationY(0f)
                .setDuration(300)
                .start()
        }

        // Close filters sheet
        btnApplyFilter.setOnClickListener {
            bottomSheet.animate()
                .translationY(bottomSheet.height.toFloat())
                .setDuration(300)
                .withEndAction {
                    bottomSheet.visibility = android.view.View.GONE
                }
                .start()
        }
    }
}