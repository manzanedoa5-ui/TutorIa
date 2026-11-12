package com.catalogoturismocusco.tutoria

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

data class StudentRecord(
    val id: String,
    val semestre: Int,
    val promedioPrevio: Double,
    val creditosAprobadosPrevios: Int,
    val cursosRepetidos: Int,
    val cargaActual: Int,
    val desercion: Int
)

data class ApiAlert(
    val id: String,
    val semestre: Int,
    val riesgoEstimado: Double,
    val estado: String
)

object DataRepository {
    var studentRecords = listOf<StudentRecord>()
    var currentApiAlerts = listOf<ApiAlert>()
    var dataLoaded = false

    fun loadData(context: Context) {
        if (dataLoaded) return

        val recordsList = mutableListOf<StudentRecord>()
        try {
            val reader = BufferedReader(InputStreamReader(context.assets.open("historial_sintetico.csv")))
            reader.readLine() // skip header
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val tokens = line!!.split(",")
                if (tokens.size >= 7) {
                    recordsList.add(
                        StudentRecord(
                            id = tokens[0],
                            semestre = tokens[1].toIntOrNull() ?: 0,
                            promedioPrevio = tokens[2].toDoubleOrNull() ?: 0.0,
                            creditosAprobadosPrevios = tokens[3].toIntOrNull() ?: 0,
                            cursosRepetidos = tokens[4].toIntOrNull() ?: 0,
                            cargaActual = tokens[5].toIntOrNull() ?: 0,
                            desercion = tokens[6].toIntOrNull() ?: 0
                        )
                    )
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        studentRecords = recordsList
        dataLoaded = true
    }
}
