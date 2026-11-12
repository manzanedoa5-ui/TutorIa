package com.catalogoturismocusco.tutoria

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "tutoria.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE students (
                id TEXT,
                semestre INTEGER,
                nombres TEXT,
                apellidos TEXT,
                promedio_previo REAL,
                creditos_aprobados INTEGER,
                cursos_repetidos INTEGER,
                carga_actual INTEGER,
                PRIMARY KEY(id, semestre)
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun insertOrUpdateStudent(record: StudentRecord): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("id", record.id)
            put("semestre", record.semestre)
            put("nombres", record.nombres)
            put("apellidos", record.apellidos)
            put("promedio_previo", record.promedioPrevio)
            put("creditos_aprobados", record.creditosAprobadosPrevios)
            put("cursos_repetidos", record.cursosRepetidos)
            put("carga_actual", record.cargaActual)
        }
        val res = db.insertWithOnConflict("students", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return res != -1L
    }

    fun getAllStudents(): List<StudentRecord> {
        val list = mutableListOf<StudentRecord>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM students", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(
                    StudentRecord(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        semestre = cursor.getInt(cursor.getColumnIndexOrThrow("semestre")),
                        nombres = cursor.getString(cursor.getColumnIndexOrThrow("nombres")),
                        apellidos = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
                        promedioPrevio = cursor.getDouble(cursor.getColumnIndexOrThrow("promedio_previo")),
                        creditosAprobadosPrevios = cursor.getInt(cursor.getColumnIndexOrThrow("creditos_aprobados")),
                        cursosRepetidos = cursor.getInt(cursor.getColumnIndexOrThrow("cursos_repetidos")),
                        cargaActual = cursor.getInt(cursor.getColumnIndexOrThrow("carga_actual")),
                        desercion = 0,
                        isLocal = true
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }
    
    fun getStudent(id: String, semestre: Int): StudentRecord? {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM students WHERE id=? AND semestre=?", arrayOf(id, semestre.toString()))
        var record: StudentRecord? = null
        if (cursor.moveToFirst()) {
            record = StudentRecord(
                id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                semestre = cursor.getInt(cursor.getColumnIndexOrThrow("semestre")),
                nombres = cursor.getString(cursor.getColumnIndexOrThrow("nombres")),
                apellidos = cursor.getString(cursor.getColumnIndexOrThrow("apellidos")),
                promedioPrevio = cursor.getDouble(cursor.getColumnIndexOrThrow("promedio_previo")),
                creditosAprobadosPrevios = cursor.getInt(cursor.getColumnIndexOrThrow("creditos_aprobados")),
                cursosRepetidos = cursor.getInt(cursor.getColumnIndexOrThrow("cursos_repetidos")),
                cargaActual = cursor.getInt(cursor.getColumnIndexOrThrow("carga_actual")),
                desercion = 0,
                isLocal = true
            )
        }
        cursor.close()
        return record
    }
}
