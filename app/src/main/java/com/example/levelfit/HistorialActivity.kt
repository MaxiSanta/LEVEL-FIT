package com.example.levelfit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import java.text.DateFormat
import java.util.Date

class HistorialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historial)
        findViewById<View>(R.id.btnVolverMenu).setOnClickListener {
            startActivity(Intent(this, MenuPrincipalActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            finish()
        }
        findViewById<View>(R.id.btnRecargar).setOnClickListener { cargarHistorial() }
    }

    override fun onStart() {
        super.onStart()
        cargarHistorial()
    }

    private fun cargarHistorial() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val texto = findViewById<TextView>(R.id.tvHistorial)
        val reintentar = findViewById<View>(R.id.btnRecargar)
        texto.setText(R.string.ent_cargando)
        reintentar.visibility = View.GONE
        FirebaseDatabase.getInstance(getString(R.string.ent_url_base)).reference.child("usuarios").child(uid)
            .child("entrenamientos").get().addOnCompleteListener(this) { tarea ->
                if (!tarea.isSuccessful) {
                    texto.setText(R.string.ent_err_lectura)
                    reintentar.visibility = View.VISIBLE
                    return@addOnCompleteListener
                }
                val sesiones = tarea.result.children.sortedByDescending { numero(it, "fecha").toLong() }
                val resumen = sesiones.joinToString("\n\n") { mostrarSesion(it) }
                texto.text = resumen.ifBlank { getString(R.string.ent_historial_vacio) }
            }
    }

    private fun numero(dato: DataSnapshot, campo: String): Double =
        (dato.child(campo).value as? Number)?.toDouble() ?: 0.0

    private fun mostrarSesion(sesion: DataSnapshot): String {
        val fecha = Date(numero(sesion, "fecha").toLong())
        val encabezado = getString(R.string.ent_fecha_fila,
            sesion.child("nombreRutina").getValue(String::class.java).orEmpty(),
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(fecha))
        val ejercicios = sesion.child("ejercicios").children.joinToString("\n") { e ->
            getString(R.string.ent_fila, e.child("nombre").getValue(String::class.java).orEmpty(),
                numero(e, "pesoKg"), numero(e, "series").toInt(),
                numero(e, "repeticiones").toInt(), numero(e, "volumen"))
        }
        return listOf(encabezado, ejercicios, getString(R.string.ent_total,
            numero(sesion, "volumenTotal"))).joinToString("\n")
    }
}