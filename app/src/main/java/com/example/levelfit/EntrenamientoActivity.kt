package com.example.levelfit

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale
import java.util.UUID

class EntrenamientoActivity : AppCompatActivity() {
    private lateinit var usuario: DatabaseReference
    private lateinit var idSesion: String
    private lateinit var nombreRutina: String
    private var fecha = System.currentTimeMillis()
    private val registros = arrayListOf<EjercicioRealizado>()
    private var guardando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_entrenamiento)
        usuario = FirebaseDatabase.getInstance(getString(R.string.ent_url_base)).reference.child("usuarios").child(uid)
        idSesion = savedInstanceState?.getString("idSesion") ?: UUID.randomUUID().toString()
        fecha = savedInstanceState?.getLong("fecha") ?: fecha
        nombreRutina = intent.getStringExtra("nombreRutina") ?: getString(R.string.ent_libre)
        findViewById<TextView>(R.id.tvNombreRutina).text = nombreRutina
        @Suppress("DEPRECATION") // También funciona en Android 8 (API 26).
        val anteriores = savedInstanceState?.getSerializable("registros") as? ArrayList<*>
        if (anteriores != null) registros.addAll(anteriores.filterIsInstance<EjercicioRealizado>())

        val peso = findViewById<EditText>(R.id.etPeso)
        peso.keyListener = DigitsKeyListener.getInstance("0123456789,.")
        peso.setRawInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        if (savedInstanceState == null) findViewById<EditText>(R.id.etSeries).setText("1")

        findViewById<View>(R.id.btnElegir).setOnClickListener { elegirEjercicio() }
        findViewById<View>(R.id.btnNombrePropio).setOnClickListener { guardarNombrePropio() }
        findViewById<View>(R.id.btnAgregarFila).setOnClickListener { agregarRegistro() }
        findViewById<View>(R.id.btnQuitar).setOnClickListener {
            if (registros.isNotEmpty()) registros.removeAt(registros.lastIndex)
            mostrarResumen()
        }
        findViewById<View>(R.id.btnGuardarSesion).setOnClickListener { guardarSesion() }
        findViewById<View>(R.id.btnVolverRutinas).setOnClickListener { salir() }
        onBackPressedDispatcher.addCallback(this) { salir() }
        mostrarResumen()
        if (savedInstanceState?.getBoolean("guardando") == true) guardarSesion()
    }

    private fun catalogoBase(): List<String> {
        val listas = listOf(R.array.ent_lista_cuerpo, R.array.ent_lista_superior,
            R.array.ent_lista_inferior, R.array.ent_lista_core)
        return listas.flatMap { resources.getStringArray(it).toList() }.distinct()
    }

    private fun elegirEjercicio() {
        val deRutina = intent.getStringArrayExtra("ejercicios").orEmpty().toList()
        val sugeridos = deRutina.ifEmpty { catalogoBase() }
        val boton = findViewById<Button>(R.id.btnElegir)
        boton.isEnabled = false
        usuario.child("ejerciciosPropios").get().addOnCompleteListener { tarea ->
            if (isFinishing || isDestroyed) return@addOnCompleteListener
            boton.isEnabled = !guardando
            val propios = if (tarea.isSuccessful) {
                tarea.result.children.mapNotNull { it.getValue(String::class.java) }.sorted()
            } else {
                aviso(R.string.ent_err_lectura)
                emptyList()
            }
            val nombres = (sugeridos + propios).distinctBy { it.lowercase(Locale.ROOT) }.toTypedArray()
            if (!guardando) AlertDialog.Builder(this)
                .setTitle(R.string.ent_elegir)
                .setItems(nombres) { _, indice -> campo(R.id.etEjercicio).setText(nombres[indice]) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private fun campo(id: Int): EditText = findViewById(id)

    private fun leerNombre(): String? {
        val entrada = campo(R.id.etEjercicio)
        val nombre = entrada.text.toString().trim()
        if (nombre.isNotBlank()) return nombre
        entrada.error = getString(R.string.ent_err_nombre)
        return null
    }

    // Un mismo nombre conserva su identificador si se vuelve a guardar.
    private fun idEjercicio(nombre: String): String =
        UUID.nameUUIDFromBytes(nombre.lowercase(Locale.ROOT).toByteArray(Charsets.UTF_8)).toString()

    private fun guardarNombrePropio() {
        val nombre = leerNombre() ?: return
        if (catalogoBase().any { it.equals(nombre, ignoreCase = true) }) {
            aviso(R.string.ent_ya_base)
            return
        }
        usuario.child("ejerciciosPropios").child(idEjercicio(nombre)).setValue(nombre)
            .addOnCompleteListener(this) {
                if (it.isSuccessful && campo(R.id.etEjercicio).text.toString().trim() == nombre &&
                    campo(R.id.etPeso).text.isBlank() && campo(R.id.etRepeticiones).text.isBlank()) {
                    campo(R.id.etEjercicio).text.clear()
                }
                aviso(if (it.isSuccessful) R.string.ent_guardado_nombre else R.string.ent_err_guardar)
            }
    }

    private fun leerEntero(id: Int): Int? {
        val entrada = campo(id)
        val valor = entrada.text.toString().trim().toIntOrNull()
        if (valor != null && valor > 0) return valor
        entrada.error = getString(R.string.ent_err_entero)
        return null
    }

    private fun agregarRegistro() {
        val nombre = leerNombre() ?: return
        val entradaPeso = campo(R.id.etPeso)
        val kilos = entradaPeso.text.toString().trim().replace(',', '.').toDoubleOrNull()
        if (kilos == null || !kilos.isFinite() || kilos < 0) {
            entradaPeso.error = getString(R.string.ent_err_peso)
            return
        }
        val series = leerEntero(R.id.etSeries) ?: return
        val repeticiones = leerEntero(R.id.etRepeticiones) ?: return
        registros.add(EjercicioRealizado(nombre, kilos, series, repeticiones))
        campo(R.id.etEjercicio).text.clear()
        entradaPeso.text.clear()
        campo(R.id.etSeries).setText("1")
        campo(R.id.etRepeticiones).text.clear()
        mostrarResumen()
    }

    private fun mostrarResumen() {
        val resumen = registros.joinToString("\n\n") { e ->
            getString(R.string.ent_fila, e.nombre, e.pesoKg, e.series, e.repeticiones, e.volumen())
        }
        findViewById<TextView>(R.id.tvFilas).text = resumen.ifEmpty { getString(R.string.ent_sin_filas) }
        val total = registros.sumOf { it.volumen() }
        findViewById<TextView>(R.id.tvTotal).text = getString(R.string.ent_total, total)
        findViewById<View>(R.id.btnQuitar).isEnabled = registros.isNotEmpty() && !guardando
        val guardar = findViewById<Button>(R.id.btnGuardarSesion)
        guardar.isEnabled = registros.isNotEmpty() && !guardando
        guardar.setText(if (guardando) R.string.ent_estado_guardando else R.string.ent_guardar_sesion)
    }

    private fun guardarSesion() {
        if (guardando || registros.isEmpty()) return
        if (hayFilaPendiente()) {
            aviso(R.string.ent_pendiente)
            return
        }
        guardando = true
        habilitarFormulario(false)
        mostrarResumen()
        val cambios = mutableMapOf<String, Any>()
        cambios["entrenamientos/$idSesion"] = mapOf(
            "nombreRutina" to nombreRutina,
            "fecha" to fecha,
            "ejercicios" to registros.map { it.paraFirebase() },
            "volumenTotal" to registros.sumOf { it.volumen() }
        )
        val base = catalogoBase()
        for (e in registros) {
            if (base.none { it.equals(e.nombre, ignoreCase = true) }) {
                cambios["ejerciciosPropios/${idEjercicio(e.nombre)}"] = e.nombre
            }
        }
        // Una escritura guarda la sesión y sus ejercicios nuevos con el mismo resultado.
        usuario.updateChildren(cambios).addOnCompleteListener { tarea ->
            if (isFinishing || isDestroyed) return@addOnCompleteListener
            guardando = false
            if (tarea.isSuccessful) {
                aviso(R.string.ent_guardado_sesion)
                startActivity(Intent(this, HistorialActivity::class.java))
                finish()
            } else {
                habilitarFormulario(true)
                mostrarResumen()
                aviso(R.string.ent_err_guardar)
            }
        }
    }

    private fun habilitarFormulario(activo: Boolean) {
        val ids = listOf(R.id.etEjercicio, R.id.etPeso, R.id.etSeries, R.id.etRepeticiones,
            R.id.btnElegir, R.id.btnNombrePropio, R.id.btnAgregarFila)
        ids.forEach { findViewById<View>(it).isEnabled = activo }
    }

    private fun hayFilaPendiente(): Boolean =
        listOf(R.id.etEjercicio, R.id.etPeso, R.id.etRepeticiones).any {
            campo(it).text.isNotBlank()
        } || campo(R.id.etSeries).text.toString().let { it.isNotBlank() && it != "1" }

    private fun salir() {
        if (guardando) { aviso(R.string.ent_estado_guardando); return }
        if (registros.isEmpty() && !hayFilaPendiente()) { finish(); return }
        AlertDialog.Builder(this).setMessage(R.string.ent_descartar)
            .setNegativeButton(R.string.ent_seguir, null)
            .setPositiveButton(R.string.ent_salir) { _, _ -> finish() }.show()
    }

    private fun aviso(id: Int) { Toast.makeText(this, id, Toast.LENGTH_LONG).show() }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable("registros", ArrayList(registros))
        outState.putString("idSesion", idSesion)
        outState.putLong("fecha", fecha)
        outState.putBoolean("guardando", guardando)
        super.onSaveInstanceState(outState)
    }
}