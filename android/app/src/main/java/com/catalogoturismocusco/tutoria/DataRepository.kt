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
    val desercion: Int,
    val nombres: String = "",
    val apellidos: String = "",
    val isLocal: Boolean = false
) {
    val fullName: String get() = if (nombres.isEmpty() && apellidos.isEmpty()) "" else "$nombres $apellidos".trim()
}

data class ApiAlert(
    val id: String,
    val semestre: Int,
    val riesgoEstimado: Double,
    val estado: String
)

object DataRepository {
    var csvStudentRecords = listOf<StudentRecord>()
    var studentRecords = listOf<StudentRecord>()
    var currentApiAlerts = listOf<ApiAlert>()
    var dataLoaded = false

    fun loadData(context: Context) {
        if (!dataLoaded) {
            val recordsList = mutableListOf<StudentRecord>()
            try {
                val reader = BufferedReader(InputStreamReader(context.assets.open("historial_sintetico.csv")))
                reader.readLine()
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
            csvStudentRecords = recordsList
            dataLoaded = true
        }
        refreshMergedRecords(context)
    }
    
    fun refreshMergedRecords(context: Context) {
        val dbHelper = DatabaseHelper(context)
        val dbRecords = dbHelper.getAllStudents()
        val dbMap = dbRecords.associateBy { it.id to it.semestre }
        
        val mergedList = mutableListOf<StudentRecord>()
        for (csvRec in csvStudentRecords) {
            val dbRec = dbMap[csvRec.id to csvRec.semestre]
            if (dbRec != null) {
                mergedList.add(dbRec)
            } else {
                mergedList.add(csvRec)
            }
        }
        
        val csvKeys = csvStudentRecords.map { it.id to it.semestre }.toSet()
        for (dbRec in dbRecords) {
            if (!csvKeys.contains(dbRec.id to dbRec.semestre)) {
                mergedList.add(dbRec)
            }
        }
        studentRecords = mergedList
    }
}
