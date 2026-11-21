package com.example.levelfit

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class MenuPrincipalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_principal)

        val nombre = intent.getStringExtra("nombre")
        if (!nombre.isNullOrBlank()) {
            findViewById<TextView>(R.id.tvSaludo).text = getString(R.string.ent_saludo, nombre)
        }

        // 1. Identificar los botones del menú
        val btnVerRutinas = findViewById<MaterialButton>(R.id.btnVerRutinas)
        val btnMisRutinas = findViewById<MaterialButton>(R.id.btnMisRutinas)
        val btnVerHistorial = findViewById<MaterialButton>(R.id.btnVerHistorial)
        val btnCerrarSesion = findViewById<MaterialButton>(R.id.btnCerrarSesion)

        // 2. Abrir rutinas predeterminadas
        btnVerRutinas.setOnClickListener {
            val intent = Intent(this, RutinasActivity::class.java)
            intent.putExtra("tipoRutina", "predeterminadas")
            startActivity(intent)
        }

        // 3. Abrir rutinas personalizadas
        btnMisRutinas.setOnClickListener {
            val intent = Intent(this, RutinasActivity::class.java)
            intent.putExtra("tipoRutina", "personalizadas")
            startActivity(intent)
        }

        // 4. Abrir historial
        btnVerHistorial.setOnClickListener {
            val intent = Intent(this, HistorialActivity::class.java)
            startActivity(intent)
        }

        // 5. Terminar la sesión de Firebase y volver al acceso.
        btnCerrarSesion.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val acceso = Intent(this, MainActivity::class.java)
            acceso.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(acceso)
        }

        // 6. Adaptar la pantalla a las barras del sistema
        val vistaPrincipal = findViewById<android.view.View>(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(vistaPrincipal) { v, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }
}