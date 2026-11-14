package com.example.levelfit

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import android.content.Intent


class MainActivity : AppCompatActivity() {

    // Indica cuál de los dos formularios se está mostrando.
    private var modoRegistro = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Evita que el contenido quede debajo de las barras del sistema.
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            vista.setPadding(
                barras.left,
                barras.top,
                barras.right,
                barras.bottom
            )

            insets
        }

        // Recupera el formulario seleccionado si se recrea la pantalla.
        modoRegistro = savedInstanceState?.getBoolean("modoRegistro") ?: false
        actualizarFormulario()

        findViewById<MaterialButton>(
            R.id.btnMostrarRegistro
        ).setOnClickListener {
            modoRegistro = !modoRegistro
            actualizarFormulario()
        }
        // Provisional: permite probar el menú sin Firebase.
        findViewById<MaterialButton>(
            R.id.btnIniciarSesion
        ).setOnClickListener {
            if (!modoRegistro) {
                val intent = Intent(
                    this,
                    MenuPrincipalActivity::class.java
                )

                startActivity(intent)
            }
        }
    }

    private fun actualizarFormulario() {
        val titulo = findViewById<TextView>(R.id.tvTituloFormulario)
        val nombre = findViewById<View>(R.id.contenedorNombre)
        val confirmacion = findViewById<View>(R.id.contenedorConfirmacion)
        val botonPrincipal = findViewById<MaterialButton>(R.id.btnIniciarSesion)
        val botonCambio = findViewById<MaterialButton>(R.id.btnMostrarRegistro)

        if (modoRegistro) {
            nombre.visibility = View.VISIBLE
            confirmacion.visibility = View.VISIBLE
            titulo.setText(R.string.lf_registro)
            botonPrincipal.setText(R.string.lf_registrar)
            botonCambio.setText(R.string.lf_volver_acceso)
        } else {
            nombre.visibility = View.GONE
            confirmacion.visibility = View.GONE
            titulo.setText(R.string.lf_acceso)
            botonPrincipal.setText(R.string.lf_entrar)
            botonCambio.setText(R.string.lf_crear_cuenta)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("modoRegistro", modoRegistro)
        super.onSaveInstanceState(outState)
    }
}
