package com.example.hdmimanager

import android.app.Activity
import android.app.Presentation
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.os.Bundle
import android.view.Display
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private var presentation: Presentation? = null

    private val listener = object : DisplayListener {
        override fun onDisplayAdded(id: Int) = showDashboard()
        override fun onDisplayRemoved(id: Int) = showDashboard()
        override fun onDisplayChanged(id: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        layout.addView(TextView(this).apply {
            text = "Gestionnaire d'écrans\nLe dashboard s'affiche sur l'écran externe.\n"(0, 0) + ""
            textSize = 16f
        })
        setContentView(layout)
        (getSystemService(DISPLAY_SERVICE) as DisplayManager)
            .registerDisplayListener(listener, null)
        showDashboard()
    }

    private fun showDashboard() {
        presentation?.dismiss()
        presentation = null
        val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val external = dm.displays.firstOrNull {
            it.displayId != Display.DEFAULT_DISPLAY &&
            it.flags and Display.FLAG_PRESENTATION != 0
        }
        if (external != null) {
            presentation = DashboardPresentation(this, external).also {
                try { it.show() } catch (e: Exception) { presentation = null }
            }
        }
    }

    override fun onDestroy() {
        presentation?.dismiss()
        super.onDestroy()
    }
}
