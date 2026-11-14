
package com.example.levelfit

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RutinasActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rutinas)

        // 1. Identificar los elementos
        val tvTitulo = findViewById<TextView>(R.id.tvTituloRutinas)
        val tvDescripcion = findViewById<TextView>(R.id.tvDescripcionRutinas)

        val seccionPredeterminadas =
            findViewById<LinearLayout>(R.id.seccionPredeterminadas)

        val seccionPersonalizadas =
            findViewById<LinearLayout>(R.id.seccionPersonalizadas)

        val btnVolverMenu = findViewById<Button>(R.id.btnVolverMenu)

        // 2. Recibir el tipo de rutina
        val tipoRutina = intent.getStringExtra("tipoRutina")

        // 3. Mostrar el contenido correspondiente
        if (tipoRutina == "personalizadas") {

            tvTitulo.text = "Mis rutinas"

            tvDescripcion.text =
                "Crea y organiza tus propios entrenamientos."

            seccionPredeterminadas.visibility = View.GONE
            seccionPersonalizadas.visibility = View.VISIBLE

        } else {

            tvTitulo.text = "Rutinas predeterminadas"

            tvDescripcion.text =
                "Explora nuestras propuestas de entrenamiento."

            seccionPredeterminadas.visibility = View.VISIBLE
            seccionPersonalizadas.visibility = View.GONE
        }

        // 4. Regresar al menú principal
        btnVolverMenu.setOnClickListener {
            finish()
        }
    }
}
