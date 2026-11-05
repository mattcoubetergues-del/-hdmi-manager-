package com.example.hdmimanager

import android.app.Activity
import android.app.AlertDialog
import android.app.Presentation
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private var presentation: Presentation? = null
    private lateinit var status: TextView
    private lateinit var dm: DisplayManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dm = getSystemService(DISPLAY_SERVICE) as DisplayManager

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        val title = TextView(this).apply {
            text = "Gestionnaire d'écrans"
            textSize = 26f
        }
        status = TextView(this).apply { textSize = 16f }
        val pickBtn = Button(this).apply { text = "🖥️ Choisir l'écran du dashboard" }
        val closeBtn = Button(this).apply { text = "❌ Fermer le dashboard" }

        pickBtn.setOnClickListener { showDisplayPicker() }
        closeBtn.setOnClickListener { stopDashboard() }

        listOf(title, status, pickBtn, closeBtn).forEach { root.addView(it) }
        setContentView(root)
        refreshStatus()
    }

    private fun allDisplays(): List<Display> = dm.displays.toList()

    private fun refreshStatus() {
        val list = allDisplays()
        val sb = StringBuilder("Écrans détectés (${list.size}) :\n")
        list.forEach {
            sb.append("• [ID ${it.displayId}] ${it.name} — ")
            sb.append(if (it.displayId == Display.DEFAULT_DISPLAY) "écran du téléphone" else "externe")
            sb.append("\n")
        }
        status.text = sb.toString()
    }

    private fun showDisplayPicker() {
        val list = allDisplays()
        val names = list.map {
            val type = if (it.displayId == Display.DEFAULT_DISPLAY) "📱 Téléphone" else "🖥️ Externe"
            "$type — ${it.name} (ID ${it.displayId})"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Afficher le dashboard sur…")
            .setItems(names) { _, which -> startDashboard(list[which]) }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun startDashboard(display: Display) {
        stopDashboard()
        presentation = DashboardPresentation(this, display).also {
            try {
                it.show()
                status.text = "✅ Dashboard affiché sur : ${display.name} (ID ${it.display.displayId})"
            } catch (e: Exception) {
                presentation = null
                status.text = "⚠️ Impossible d'afficher sur cet écran : ${e.message}"
            }
        }
    }

    private fun stopDashboard() {
        presentation?.dismiss()
        presentation = null
        refreshStatus()
    }

    override fun onDestroy() {
        presentation?.dismiss()
        super.onDestroy()
    }
}
