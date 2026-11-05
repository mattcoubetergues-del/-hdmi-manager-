package com.example.hdmimanager

import android.app.Activity
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "Gestionnaire d'écrans (HDMI)"
            textSize = 22f
        }
        layout.addView(title)

        val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val displays = dm.displays

        val info = StringBuilder()
        info.append("Écrans détectés : ${displays.size}\n\n")

        for (d in displays) {
            info.append("Écran #${d.displayId}\n")
            info.append("  Nom : ${d.name}\n")
            val flags = d.flags
            info.append("  Présentation : ${if (flags and Display.FLAG_PRESENTATION != 0) "Oui" else "Non"}\n")
            info.append("  Sécurisé : ${if (flags and Display.FLAG_SECURE != 0) "Oui" else "Non"}\n")
            val m = d.realMetrics ?: android.util.DisplayMetrics()
            info.append("  Résolution : ${m.widthPixels} x ${m.heightPixels}\n\n")
        }

        if (displays.size <= 1) {
            info.append("Aucun écran externe détecté.\n")
            info.append("Branche un câble HDMI (ou active Cast/DisplayPort USB-C), puis rouvre l'app.")
        }

        val text = TextView(this).apply {
            text = info.toString()
            textSize = 16f
            setPadding(0, 32, 0, 0)
        }
        layout.addView(text)
        setContentView(layout)
    }
}
