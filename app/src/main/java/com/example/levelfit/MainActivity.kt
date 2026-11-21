package com.example.levelfit

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private var modoRegistro = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(R.id.main)) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            vista.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        modoRegistro = savedInstanceState?.getBoolean("modoRegistro") ?: false
        actualizarFormulario()
        findViewById<View>(R.id.btnMostrarRegistro).setOnClickListener {
            modoRegistro = !modoRegistro
            actualizarFormulario()
        }
        findViewById<View>(R.id.btnIniciarSesion).setOnClickListener {
            if (auth.currentUser == null) autenticar() else cargarPerfil()
        }
        if (auth.currentUser != null) cargarPerfil()
    }

    private fun autenticar() {
        val correo = findViewById<EditText>(R.id.etCorreo)
        val clave = findViewById<EditText>(R.id.etClave)
        val nombre = findViewById<EditText>(R.id.etNombre)
        val confirmar = findViewById<EditText>(R.id.etConfirmarClave)
        val email = correo.text.toString().trim()
        val password = clave.text.toString()
        val nombreNuevo = nombre.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            correo.error = getString(R.string.ent_err_correo)
            return
        }
        if (password.isEmpty() || (modoRegistro && password.length < 6)) {
            clave.error = getString(if (modoRegistro) R.string.ent_err_seis else R.string.ent_err_clave)
            return
        }
        if (modoRegistro && nombreNuevo.isBlank()) {
            nombre.error = getString(R.string.ent_err_nombre_persona)
            return
        }
        if (modoRegistro && password != confirmar.text.toString()) {
            confirmar.error = getString(R.string.ent_err_confirmacion)
            return
        }

        botonesActivos(false)
        val registro = modoRegistro
        val tarea = if (registro) auth.createUserWithEmailAndPassword(email, password)
        else auth.signInWithEmailAndPassword(email, password)
        tarea.addOnCompleteListener { resultado ->
            if (isFinishing || isDestroyed) return@addOnCompleteListener
            if (!resultado.isSuccessful) {
                errorDeAcceso(if (registro) R.string.ent_err_registro else R.string.ent_err_acceso)
            } else if (registro) {
                guardarPerfil(nombreNuevo)
            } else {
                cargarPerfil()
            }
        }
    }

    private fun cargarPerfil() {
        val usuario = auth.currentUser ?: return
        botonesActivos(false)
        val perfil = FirebaseDatabase.getInstance(getString(R.string.ent_url_base)).reference
            .child("usuarios").child(usuario.uid).child("perfil")
        perfil.child("nombre").get().addOnCompleteListener { resultado ->
            if (isFinishing || isDestroyed) return@addOnCompleteListener
            if (!resultado.isSuccessful) {
                errorDeAcceso(R.string.ent_err_lectura)
                return@addOnCompleteListener
            }
            val nombre = resultado.result.getValue(String::class.java)
            if (!nombre.isNullOrBlank()) {
                abrirMenu(nombre)
            } else {
                // Una cuenta creada antes de un corte de conexión puede carecer de perfil.
                val campo = EditText(this)
                AlertDialog.Builder(this)
                    .setTitle(R.string.ent_nombre_perfil)
                    .setView(campo)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ent_confirmar) { _, _ ->
                        val elegido = campo.text.toString().trim()
                        if (elegido.isBlank()) errorDeAcceso(R.string.ent_err_nombre_persona)
                        else guardarPerfil(elegido)
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        auth.signOut()
                        botonesActivos(true)
                    }.show()
            }
        }
    }

    private fun guardarPerfil(nombre: String) {
        val usuario = auth.currentUser ?: return
        FirebaseDatabase.getInstance(getString(R.string.ent_url_base)).reference.child("usuarios").child(usuario.uid)
            .child("perfil").setValue(mapOf("nombre" to nombre, "correo" to usuario.email))
            .addOnCompleteListener {
                if (isFinishing || isDestroyed) return@addOnCompleteListener
                if (it.isSuccessful) abrirMenu(nombre) else errorDeAcceso(R.string.ent_err_perfil)
            }
    }

    private fun abrirMenu(nombre: String) {
        startActivity(Intent(this, MenuPrincipalActivity::class.java).putExtra("nombre", nombre))
        finish()
    }

    private fun actualizarFormulario() {
        val visible = if (modoRegistro) View.VISIBLE else View.GONE
        findViewById<View>(R.id.contenedorNombre).visibility = visible
        findViewById<View>(R.id.contenedorConfirmacion).visibility = visible
        findViewById<TextView>(R.id.tvTituloFormulario)
            .setText(if (modoRegistro) R.string.lf_registro else R.string.lf_acceso)
        findViewById<MaterialButton>(R.id.btnIniciarSesion)
            .setText(if (modoRegistro) R.string.lf_registrar else R.string.lf_entrar)
        findViewById<MaterialButton>(R.id.btnMostrarRegistro)
            .setText(if (modoRegistro) R.string.lf_volver_acceso else R.string.lf_crear_cuenta)
    }

    private fun botonesActivos(activos: Boolean) {
        findViewById<View>(R.id.btnIniciarSesion).isEnabled = activos
        findViewById<View>(R.id.btnMostrarRegistro).isEnabled = activos
    }

    private fun errorDeAcceso(texto: Int) {
        botonesActivos(true)
        Toast.makeText(this, texto, Toast.LENGTH_LONG).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("modoRegistro", modoRegistro)
        super.onSaveInstanceState(outState)
    }
}