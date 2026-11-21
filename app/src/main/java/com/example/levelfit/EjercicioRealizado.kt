package com.example.levelfit

import java.io.Serializable

data class EjercicioRealizado(
    val nombre: String,
    val pesoKg: Double,
    val series: Int,
    val repeticiones: Int
) : Serializable {
    fun volumen(): Double = pesoKg * series * repeticiones

    fun paraFirebase(): Map<String, Any> = mapOf(
        "nombre" to nombre,
        "pesoKg" to pesoKg,
        "series" to series,
        "repeticiones" to repeticiones,
        "volumen" to volumen()
    )
}