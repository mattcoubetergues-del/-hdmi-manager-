package com.example.hdmimanager

import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private lateinit var displayManager: DisplayManager
    private lateinit var displayAdapter: DisplayAdapter
    private lateinit var presentationButton: MaterialButton
    private var activePresentation: DemoPresentation? = null

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = refreshDisplays()
        override fun onDisplayRemoved(displayId: Int) = refreshDisplays()
        override fun onDisplayChanged(displayId: Int) = refreshDisplays()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displayList = findViewById<RecyclerView>(R.id.display_list)
        displayAdapter = DisplayAdapter()
        displayList.layoutManager = LinearLayoutManager(this)
        displayList.adapter = displayAdapter
        presentationButton = findViewById(R.id.btn_presentation)
        presentationButton.setOnClickListener { launchPresentation() }
    }

    override fun onStart() {
        super.onStart()
        displayManager.registerDisplayListener(displayListener, null)
        refreshDisplays()
    }

    override fun onStop() {
        displayManager.unregisterDisplayListener(displayListener)
        super.onStop()
    }

    override fun onDestroy() {
        activePresentation?.dismiss()
        activePresentation = null
        super.onDestroy()
    }

    private fun refreshDisplays() {
        if (!::displayAdapter.isInitialized) return
        val presentationIds = displayManager
            .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
            .map { it.displayId }
            .toSet()
        val displays = displayManager.displays.sortedBy { it.displayId }.map { display ->
            val metrics = android.util.DisplayMetrics()
            display.getRealMetrics(metrics)
            val role = when {
                display.displayId == Display.DEFAULT_DISPLAY -> getString(R.string.primary_display)
                presentationIds.contains(display.displayId) -> getString(R.string.presentation_display)
                else -> getString(R.string.other_display)
            }
            val type = when (display.type) {
                Display.TYPE_BUILT_IN -> getString(R.string.type_builtin)
                Display.TYPE_HDMI -> getString(R.string.type_hdmi)
                Display.TYPE_WIFI -> getString(R.string.type_wifi)
                Display.TYPE_OVERLAY -> getString(R.string.type_overlay)
                Display.TYPE_VIRTUAL -> getString(R.string.type_virtual)
                else -> getString(R.string.type_unknown, display.type)
            }
            getString(
                R.string.display_details,
                display.displayId,
                display.name,
                role,
                type,
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi
            )
        }
        displayAdapter.submitList(displays.ifEmpty { listOf(getString(R.string.no_displays)) })
        presentationButton.isEnabled = firstPresentationDisplay() != null
    }

    private fun launchPresentation() {
        val display = firstPresentationDisplay()
        if (display == null) {
            Toast.makeText(this, R.string.no_external_display, Toast.LENGTH_SHORT).show()
            refreshDisplays()
            return
        }
        activePresentation?.dismiss()
        activePresentation = DemoPresentation(this, display).also { presentation ->
            presentation.setOnDismissListener { activePresentation = null }
            presentation.show()
        }
    }

    private fun firstPresentationDisplay(): Display? = displayManager
        .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        .firstOrNull()

    private class DisplayAdapter : RecyclerView.Adapter<DisplayAdapter.DisplayViewHolder>() {
        private val items = mutableListOf<String>()

        fun submitList(newItems: List<String>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayViewHolder {
            val textView = TextView(parent.context).apply {
                textSize = 16f
                setTextColor(resources.getColor(R.color.text_primary, null))
                setPadding(16, 14, 16, 14)
                background = resources.getDrawable(R.drawable.display_row_background, null)
            }
            val params = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
            textView.layoutParams = params
            return DisplayViewHolder(textView)
        }

        override fun onBindViewHolder(holder: DisplayViewHolder, position: Int) {
            holder.label.text = items[position]
        }

        override fun getItemCount(): Int = items.size

        class DisplayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val label: TextView = view as TextView
        }
    }

    private class DemoPresentation(context: Context, display: Display) : Presentation(context, display) {
        private val hostContext = context
        private val externalDisplayName = display.name

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val message = TextView(hostContext).apply {
                text = hostContext.getString(R.string.presentation_message, externalDisplayName)
                textSize = 28f
                gravity = Gravity.CENTER
                setTextColor(resources.getColor(R.color.presentation_text, null))
                setBackgroundColor(resources.getColor(R.color.presentation_background, null))
                setPadding(32, 32, 32, 32)
            }
            setContentView(message)
        }
    }
}
