package com.bignerdranch.android.activity_coordinator

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge

class FilterActivity : AppCompatActivity() {

    private lateinit var bottomSheet: LinearLayout
    private lateinit var btnFilter: Button
    private lateinit var btnApplyFilter: Button
    private lateinit var scrollActiveFilters: HorizontalScrollView
    private lateinit var layoutActiveFilters: LinearLayout
    private lateinit var ResultCount: TextView
    private val activeFilters = mutableSetOf<String>()

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

        scrollActiveFilters = findViewById(R.id.scroll_active_filters)
        layoutActiveFilters = findViewById(R.id.layout_active_filters)
        ResultCount = findViewById(R.id.result_count)
        setupChips()

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
    private fun setupChips() {
        val chips = mapOf(
            R.id.chip_music   to "Music",
            R.id.chip_hiking  to "Hiking",
            R.id.chip_cooking to "Cooking",
            R.id.chip_gaming  to "Gaming",
            R.id.chip_reading to "Reading",
            R.id.chip_travel  to "Travel",
            R.id.chip_yoga    to "Yoga",
            R.id.chip_coffee  to "Coffee",
            R.id.chip_fitness to "Fitness"
        )

        chips.forEach { (chipId, label) ->
            val chip = findViewById<TextView>(chipId)
            chip.setOnClickListener {
                if (label in activeFilters) {
                    activeFilters.remove(label)
                    chip.setTextColor(Color.parseColor("#8888A4"))
                    chip.setBackgroundColor(Color.parseColor("#16161F"))
                } else {
                    activeFilters.add(label)
                    chip.setTextColor(Color.parseColor("#2ECC71"))
                    chip.setBackgroundColor(Color.parseColor("#222ECC71"))
                }
                updateActiveFilterRow()
            }
        }
    }

    private fun updateActiveFilterRow() {
        layoutActiveFilters.removeAllViews()

        if (activeFilters.isEmpty()) {
            scrollActiveFilters.visibility = android.view.View.GONE
            ResultCount.text = "All nearby people"
            return
        }

        scrollActiveFilters.visibility = android.view.View.VISIBLE
        ResultCount.text = "${activeFilters.size} filter${if (activeFilters.size > 1) "s" else ""} active"

        val dp = resources.displayMetrics.density

        activeFilters.forEach { label ->
            val pill = TextView(this)
            pill.text     = "$label  ⓧ"
            pill.textSize = 12f
            pill.setTypeface(null, Typeface.BOLD)
            pill.setTextColor(Color.parseColor("#2ECC71"))
            pill.setBackgroundColor(Color.parseColor("#222ECC71"))
            pill.setPadding(
                (10 * dp).toInt(), (6 * dp).toInt(),
                (10 * dp).toInt(), (6 * dp).toInt()
            )

            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = (8 * dp).toInt()
            pill.layoutParams = lp

            // tapping a pill removes that filter
            pill.setOnClickListener {
                activeFilters.remove(label)
                // also deselect the chip in the sheet
                val chipId = when (label) {
                    "Music"   -> R.id.chip_music
                    "Hiking"  -> R.id.chip_hiking
                    "Cooking" -> R.id.chip_cooking
                    "Gaming"  -> R.id.chip_gaming
                    "Reading" -> R.id.chip_reading
                    "Travel"  -> R.id.chip_travel
                    "Yoga"    -> R.id.chip_yoga
                    "Coffee"  -> R.id.chip_coffee
                    "Fitness" -> R.id.chip_fitness
                    else      -> null
                }
                chipId?.let { id ->
                    findViewById<TextView>(id).setTextColor(Color.parseColor("#8888A4"))
                    findViewById<TextView>(id).setBackgroundColor(Color.parseColor("#16161F"))
                }
                updateActiveFilterRow()
            }

            layoutActiveFilters.addView(pill)
        }
    }

    //get the current selected filters for profile matching
    fun getActiveFilters(): Set<String> = activeFilters.toSet()
}