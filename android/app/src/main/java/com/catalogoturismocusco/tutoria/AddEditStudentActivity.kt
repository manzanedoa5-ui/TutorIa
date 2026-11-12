package com.catalogoturismocusco.tutoria

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Locale

class AddEditStudentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_student)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etId = findViewById<EditText>(R.id.etId)
        val etNombres = findViewById<EditText>(R.id.etNombres)
        val etApellidos = findViewById<EditText>(R.id.etApellidos)
        val etSemestre = findViewById<EditText>(R.id.etSemestre)
        val etPromedio = findViewById<EditText>(R.id.etPromedio)
        val etCreditos = findViewById<EditText>(R.id.etCreditos)
        val etRepetidos = findViewById<EditText>(R.id.etRepetidos)
        val etCarga = findViewById<EditText>(R.id.etCarga)
        val btnSave = findViewById<Button>(R.id.btnSave)
        
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val editId = intent.getStringExtra("student_id")
        val editSemestre = intent.getIntExtra("semestre", -1)
        var isEdit = false

        if (editId != null && editSemestre != -1) {
            isEdit = true
            val record = DataRepository.studentRecords.find { it.id == editId && it.semestre == editSemestre }
            if (record != null) {
                etId.setText(record.id.replace("SIM-", "AL-"))
                etId.isEnabled = false
                etSemestre.setText(record.semestre.toString())
                etSemestre.isEnabled = false
                etNombres.setText(record.nombres)
                etApellidos.setText(record.apellidos)
                etPromedio.setText(record.promedioPrevio.toString())
                etCreditos.setText(record.creditosAprobadosPrevios.toString())
                etRepetidos.setText(record.cursosRepetidos.toString())
                etCarga.setText(record.cargaActual.toString())
                findViewById<TextView>(R.id.tvTitle).text = "Editar Estudiante"
            }
        }

        btnSave.setOnClickListener {
            val idStr = etId.text.toString().trim()
            val nombres = etNombres.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val semestreStr = etSemestre.text.toString().trim()
            val promedioStr = etPromedio.text.toString().trim()
            val creditosStr = etCreditos.text.toString().trim()
            val repetidosStr = etRepetidos.text.toString().trim()
            val cargaStr = etCarga.text.toString().trim()

            if (idStr.isEmpty() || nombres.isEmpty() || apellidos.isEmpty() || semestreStr.isEmpty() || 
                promedioStr.isEmpty() || creditosStr.isEmpty() || repetidosStr.isEmpty() || cargaStr.isEmpty()) {
                Toast.makeText(this, "Complete todos los campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val internalId = idStr.replace("AL-", "SIM-").uppercase(Locale.getDefault())
            val semestre = semestreStr.toIntOrNull()
            val promedio = promedioStr.toDoubleOrNull()
            val creditos = creditosStr.toIntOrNull()
            val repetidos = repetidosStr.toIntOrNull()
            val carga = cargaStr.toIntOrNull()

            if (semestre == null || semestre <= 0) {
                Toast.makeText(this, "Periodo inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (promedio == null || promedio < 0.0 || promedio > 20.0) {
                Toast.makeText(this, "Promedio debe estar entre 0 y 20", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (creditos == null || creditos < 0 || repetidos == null || repetidos < 0 || carga == null || carga < 0) {
                Toast.makeText(this, "Valores numéricos inválidos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dbHelper = DatabaseHelper(this)
            
            if (!isEdit) {
                val existing = dbHelper.getStudent(internalId, semestre)
                if (existing != null) {
                    Toast.makeText(this, "El estudiante ya existe en este periodo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val csvExisting = DataRepository.csvStudentRecords.find { it.id == internalId && it.semestre == semestre }
                if (csvExisting != null) {
                    Toast.makeText(this, "El código ya pertenece a un estudiante sintético en este periodo", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            val record = StudentRecord(
                id = internalId,
                semestre = semestre,
                nombres = nombres,
                apellidos = apellidos,
                promedioPrevio = promedio,
                creditosAprobadosPrevios = creditos,
                cursosRepetidos = repetidos,
                cargaActual = carga,
                desercion = 0,
                isLocal = true
            )

            if (dbHelper.insertOrUpdateStudent(record)) {
                Toast.makeText(this, "Estudiante guardado", Toast.LENGTH_SHORT).show()
                DataRepository.refreshMergedRecords(this)
                finish()
            } else {
                Toast.makeText(this, "Error al guardar en la base de datos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
