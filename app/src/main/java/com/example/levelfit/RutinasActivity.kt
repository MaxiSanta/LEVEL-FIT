package com.example.levelfit

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class RutinasActivity : AppCompatActivity() {
    private var botonSeleccionado = View.NO_ID
    private var detalleAbierto: TextView? = null
    private var nombreRutina = ""
    private var ejercicios = emptyArray<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rutinas)
        val propias = intent.getStringExtra("tipoRutina") == "personalizadas"
        val seccion = findViewById<View>(R.id.seccionPredeterminadas)
        val titulo = findViewById<TextView>(R.id.tvTituloRutinas)
        val comenzar = findViewById<Button>(R.id.btnComenzarEntrenamiento)

        seccion.visibility = if (propias) View.GONE else View.VISIBLE
        titulo.setText(if (propias) R.string.ent_propias else R.string.ent_pred)
        if (propias) {
            nombreRutina = getString(R.string.ent_libre)
            findViewById<View>(R.id.tvCatalogoPropio).visibility = View.VISIBLE
            comenzar.setText(R.string.ent_abrir_libre)
            comenzar.isEnabled = true
        } else {
            conectarTarjeta(R.id.btnCuerpoCompleto, R.id.tvDetalleCuerpoCompleto, R.array.ent_lista_cuerpo)
            conectarTarjeta(R.id.btnTrenSuperior, R.id.tvDetalleSuperior, R.array.ent_lista_superior)
            conectarTarjeta(R.id.btnTrenInferior, R.id.tvDetalleInferior, R.array.ent_lista_inferior)
            conectarTarjeta(R.id.btnCore, R.id.tvDetalleCore, R.array.ent_lista_core)
            val anterior = savedInstanceState?.getInt("boton", View.NO_ID) ?: View.NO_ID
            if (anterior != View.NO_ID) findViewById<View>(anterior).performClick()
        }

        comenzar.setOnClickListener {
            val destino = Intent(this, EntrenamientoActivity::class.java)
            destino.putExtra("nombreRutina", nombreRutina)
            destino.putExtra("ejercicios", ejercicios)
            startActivity(destino)
        }
        findViewById<View>(R.id.btnVolverMenu).setOnClickListener { finish() }
    }

    private fun conectarTarjeta(botonId: Int, detalleId: Int, listaId: Int) {
        val boton = findViewById<Button>(botonId)
        val detalle = findViewById<TextView>(detalleId)
        val lista = resources.getStringArray(listaId)
        detalle.text = lista.joinToString("\n") { "• $it" }
        boton.setOnClickListener {
            val cerrar = botonSeleccionado == botonId
            detalleAbierto?.visibility = View.GONE
            botonSeleccionado = if (cerrar) View.NO_ID else botonId
            detalleAbierto = if (cerrar) null else detalle
            if (!cerrar) {
                detalle.visibility = View.VISIBLE
                nombreRutina = boton.text.toString()
                ejercicios = lista
            }
            findViewById<View>(R.id.btnComenzarEntrenamiento).isEnabled = !cerrar
        }
    }

    override fun onStart() {
        super.onStart()
        if (intent.getStringExtra("tipoRutina") != "personalizadas") return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val catalogo = findViewById<TextView>(R.id.tvCatalogoPropio)
        catalogo.setText(R.string.ent_cargando)
        FirebaseDatabase.getInstance(getString(R.string.ent_url_base)).reference.child("usuarios").child(uid)
            .child("ejerciciosPropios").get().addOnCompleteListener(this) { tarea ->
                if (!tarea.isSuccessful) {
                    catalogo.setText(R.string.ent_err_lectura)
                    return@addOnCompleteListener
                }
                val nombres = tarea.result.children.mapNotNull { it.getValue(String::class.java) }.sorted()
                catalogo.text = if (nombres.isEmpty()) getString(R.string.ent_personales_vacio)
                else getString(R.string.ent_ej_personales) + "\n" + nombres.joinToString("\n") { "• $it" }
            }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("boton", botonSeleccionado)
        super.onSaveInstanceState(outState)
    }
}