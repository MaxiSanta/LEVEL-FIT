
package com.example.levelfit

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class HistorialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)

        // Identificar el botón para regresar
        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenu)

        // Cerrar Historial y regresar al Menú Principal
        btnVolverMenu.setOnClickListener {
            finish()
        }
    }
}
