package com.catalogoturismocusco.tutoria

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.NumberFormat

class StudentActivity : AppCompatActivity() {

    private fun formatSemester(sem: Int): String {
        val year = 2019 + ((sem - 1) / 2)
        val term = if (sem % 2 == 0) "I" else "II"
        return "$year-$term"
    }

    private fun formatStudentId(internalId: String): String {
        return internalId.replace("SIM-", "AL-")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
        
        val btnEdit = findViewById<ImageButton>(R.id.btnEdit)

        val tvStudentId = findViewById<TextView>(R.id.tvStudentId)
        val tvAcademicInfo = findViewById<TextView>(R.id.tvAcademicInfo)
        val tvAlertInfo = findViewById<TextView>(R.id.tvAlertInfo)
        val llHistory = findViewById<LinearLayout>(R.id.llHistory)

        val studentId = intent.getStringExtra("student_id") ?: return
        val currentSemester = intent.getIntExtra("semestre", 0)

        tvStudentId.text = formatStudentId(studentId)

        val record = DataRepository.studentRecords.find { it.id == studentId && it.semestre == currentSemester }
        if (record != null) {
            val titleText = if (record.fullName.isNotEmpty()) {
                "${formatStudentId(studentId)}\n${record.fullName}"
            } else {
                formatStudentId(studentId)
            }
            tvStudentId.text = titleText
            
            tvAcademicInfo.text = "• Promedio previo: ${record.promedioPrevio}\n" +
                    "• Créditos aprobados previos: ${record.creditosAprobadosPrevios}\n" +
                    "• Cursos repetidos: ${record.cursosRepetidos}\n" +
                    "• Carga académica actual: ${record.cargaActual}"
                    
            btnEdit.setOnClickListener {
                val intent = Intent(this, AddEditStudentActivity::class.java)
                intent.putExtra("student_id", record.id)
                intent.putExtra("semestre", record.semestre)
                startActivity(intent)
            }
        }

        val alert = DataRepository.currentApiAlerts.find { it.id == studentId }
        if (alert != null) {
            val format = NumberFormat.getPercentInstance().apply {
                maximumFractionDigits = 1
            }
            val riesgoStr = format.format(alert.riesgoEstimado)
            tvAlertInfo.text = "Riesgo estimado: $riesgoStr\nEstado: ${alert.estado}"
            tvAlertInfo.setTextColor(Color.parseColor("#555555"))
        } else {
            if (record?.isLocal == true) {
                tvAlertInfo.text = "Sin evaluación de riesgo"
            } else {
                tvAlertInfo.text = "Sin alerta registrada en este periodo."
            }
            tvAlertInfo.setTextColor(Color.parseColor("#4CAF50"))
        }

        val historyRecords = DataRepository.studentRecords.filter { it.id == studentId }.sortedByDescending { it.semestre }
        for (hist in historyRecords) {
            val tv = TextView(this)
            tv.text = "• Periodo ${formatSemester(hist.semestre)}: Promedio ${hist.promedioPrevio}"
            tv.textSize = 14f
            tv.setTextColor(Color.parseColor("#555555"))
            tv.setPadding(0, 0, 0, 12)
            llHistory.addView(tv)
        }
    }
}