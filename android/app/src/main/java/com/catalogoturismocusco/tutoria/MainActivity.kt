package com.catalogoturismocusco.tutoria

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var spinnerSemester: Spinner
    private lateinit var cbAlertsOnly: CheckBox
    private lateinit var listView: ListView
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotalStudents: TextView
    private lateinit var tvGlobalAvg: TextView
    private lateinit var tvAtRiskCount: TextView
    private lateinit var btnRefresh: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var llError: LinearLayout
    private lateinit var btnRetry: Button

    private var currentSemester: Int = 0
    private var allSemesters = listOf<Int>()
    
    private var filteredList = mutableListOf<StudentRecord>()
    private lateinit var adapter: StudentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        DataRepository.loadData(this)

        etSearch = findViewById(R.id.etSearch)
        spinnerSemester = findViewById(R.id.spinnerSemester)
        cbAlertsOnly = findViewById(R.id.cbAlertsOnly)
        listView = findViewById(R.id.listView)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvTotalStudents = findViewById(R.id.tvTotalStudents)
        tvGlobalAvg = findViewById(R.id.tvGlobalAvg)
        tvAtRiskCount = findViewById(R.id.tvAtRiskCount)
        btnRefresh = findViewById(R.id.btnRefresh)
        progressBar = findViewById(R.id.progressBar)
        llError = findViewById(R.id.llError)
        btnRetry = findViewById(R.id.btnRetry)

        if (DataRepository.studentRecords.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            return
        }

        allSemesters = DataRepository.studentRecords.map { it.semestre }.distinct().sortedDescending()
        if (allSemesters.isNotEmpty()) {
            currentSemester = allSemesters.first()
        }

        updateSpinner()

        adapter = StudentAdapter(filteredList)
        listView.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        cbAlertsOnly.setOnCheckedChangeListener { _, _ -> applyFilters() }
        btnRefresh.setOnClickListener { fetchApiData(currentSemester) }
        btnRetry.setOnClickListener { fetchApiData(currentSemester) }

        listView.setOnItemClickListener { _, _, position, _ ->
            val record = filteredList[position]
            val intent = Intent(this, StudentActivity::class.java)
            intent.putExtra("student_id", record.id)
            intent.putExtra("semestre", currentSemester)
            startActivity(intent)
        }

        // Fetch initial data instead of applyFilters directly
        fetchApiData(currentSemester)
    }

    private fun fetchApiData(semester: Int) {
        progressBar.visibility = View.VISIBLE
        listView.visibility = View.GONE
        llError.visibility = View.GONE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val alerts = withContext(Dispatchers.IO) {
                    val host = when (android.os.Build.HARDWARE) {
                        "ranchu", "goldfish" -> "10.0.2.2"
                        else -> "127.0.0.1" // Celular físico: adb reverse tcp:8000 tcp:8000
                    }
                    val url = URL("http://$host:8000/review-queue?semestre=$semester")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonArray = JSONArray(jsonString)
                        val list = mutableListOf<ApiAlert>()
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            list.add(ApiAlert(
                                id = obj.getString("id"),
                                semestre = obj.getInt("semestre"),
                                riesgoEstimado = obj.getDouble("riesgo_estimado"),
                                estado = obj.getString("estado")
                            ))
                        }
                        list
                    } else {
                        throw Exception("HTTP error ${connection.responseCode}")
                    }
                }
                DataRepository.currentApiAlerts = alerts
                progressBar.visibility = View.GONE
                listView.visibility = View.VISIBLE
                applyFilters()
            } catch (e: Exception) {
                e.printStackTrace()
                progressBar.visibility = View.GONE
                llError.visibility = View.VISIBLE
                DataRepository.currentApiAlerts = emptyList()
                applyFilters() // apply filters will use empty list
            }
        }
    }

    private fun formatSemester(sem: Int): String {
        val year = 2019 + ((sem - 1) / 2)
        val term = if (sem % 2 == 0) "I" else "II"
        return "$year-$term"
    }

    private fun updateSpinner() {
        val top4 = allSemesters.take(4)
        val spinnerItems = top4.map { formatSemester(it) }.toMutableList()
        
        if (currentSemester !in top4) {
            spinnerItems.add(formatSemester(currentSemester))
        }
        spinnerItems.add("Ver anteriores...")

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerItems)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        spinnerSemester.onItemSelectedListener = null
        spinnerSemester.adapter = spinnerAdapter
        
        val selectionIndex = if (currentSemester in top4) {
            top4.indexOf(currentSemester)
        } else {
            4 
        }
        spinnerSemester.setSelection(selectionIndex)
        
        spinnerSemester.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedText = spinnerItems[position]
                if (selectedText == "Ver anteriores...") {
                    showOlderSemestersDialog()
                } else {
                    val matchedSem = allSemesters.find { formatSemester(it) == selectedText }
                    if (matchedSem != null && matchedSem != currentSemester) {
                        currentSemester = matchedSem
                        fetchApiData(currentSemester)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showOlderSemestersDialog() {
        val olderSemesters = allSemesters.drop(4)
        val options = olderSemesters.map { formatSemester(it) }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Seleccionar periodo")
            .setItems(options) { _, which ->
                currentSemester = olderSemesters[which]
                updateSpinner()
                fetchApiData(currentSemester)
            }
            .setOnCancelListener {
                updateSpinner()
            }
            .show()
    }

    private fun formatStudentId(internalId: String): String {
        return internalId.replace("SIM-", "AL-")
    }

    private fun applyFilters() {
        val rawQuery = etSearch.text.toString().trim().lowercase()
        val query = rawQuery.replace("al-", "sim-")
        val onlyAlerts = cbAlertsOnly.isChecked

        val semesterRecords = DataRepository.studentRecords.filter { it.semestre == currentSemester }
        val alertsForSemester = DataRepository.currentApiAlerts.map { it.id }.toSet()

        val total = semesterRecords.size
        val atRisk = alertsForSemester.size
        val avg = if (total > 0) semesterRecords.map { it.promedioPrevio }.average() else 0.0
        
        tvTotalStudents.text = total.toString()
        tvAtRiskCount.text = atRisk.toString()
        tvGlobalAvg.text = String.format(Locale.getDefault(), "%.2f", avg)

        filteredList.clear()
        filteredList.addAll(semesterRecords.filter { record ->
            val matchQuery = record.id.lowercase().contains(query)
            val matchAlert = !onlyAlerts || alertsForSemester.contains(record.id)
            matchQuery && matchAlert
        })

        adapter.notifyDataSetChanged()

        if (filteredList.isEmpty() && llError.visibility != View.VISIBLE && progressBar.visibility != View.VISIBLE) {
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
        }
    }

    inner class StudentAdapter(private val dataSource: List<StudentRecord>) : BaseAdapter() {
        override fun getCount(): Int = dataSource.size
        override fun getItem(position: Int): Any = dataSource[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(this@MainActivity).inflate(R.layout.item_student, parent, false)
            val tvCode = view.findViewById<TextView>(R.id.tvCode)
            val tvInfo = view.findViewById<TextView>(R.id.tvInfo)
            val tvAlertBadge = view.findViewById<TextView>(R.id.tvAlertBadge)
            
            val record = dataSource[position]
            tvCode.text = formatStudentId(record.id)
            tvInfo.text = "Promedio previo: ${record.promedioPrevio} | Créditos aprobados: ${record.creditosAprobadosPrevios}"
            
            val alertsForSemester = DataRepository.currentApiAlerts.map { it.id }.toSet()
            if (alertsForSemester.contains(record.id)) {
                tvAlertBadge.visibility = View.VISIBLE
            } else {
                tvAlertBadge.visibility = View.GONE
            }
            
            return view
        }
    }
}