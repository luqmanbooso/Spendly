package com.example.spendly

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendly.databinding.ActivityAnalysisBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalysisBinding
    private lateinit var prefsManager: PrefsManager
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var bottomNavHelper: BottomNavHelper
    private lateinit var backupHelper: BackupHelper

    private var selectedPeriod = "This Month"
    private var selectedTab = 0 // 0: Spending Analysis, 1: Reports
    private var startDate: Long = 0
    private var endDate: Long = System.currentTimeMillis()

    // For custom date range
    private var customStartDate: Long = 0
    private var customEndDate: Long = 0

    // Storage permission request code
    private val STORAGE_PERMISSION_CODE = 101
    private val WRITE_PERMISSION_CODE = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PrefsManager(this)
        transactionRepository = TransactionRepository(this)
        bottomNavHelper = BottomNavHelper(this, binding.root)
        backupHelper = BackupHelper(this)

        setupBottomNav()
        setupToolbar()
        setupTabLayout()
        setupPeriodSelection()
        setupExportButtons()

        // Initialize with default view
        startDate = getStartDateForPeriod(selectedPeriod)
        updateContent()

        // Apply animations
        playAnimations()
    }

    private fun setupBottomNav() {
        bottomNavHelper.setupBottomNav(NavSection.ANALYSIS)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Analysis & Reports"
        supportActionBar?.elevation = 0f
    }

    private fun playAnimations() {
        // Apply entrance animations to UI elements
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeIn.duration = 500
        binding.tabLayout.startAnimation(fadeIn)

        // Staggered animations for each element
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        slideUp.duration = 600
        slideUp.interpolator = OvershootInterpolator(0.8f)

        binding.root.postDelayed({
            binding.periodChipGroup.startAnimation(fadeIn)
        }, 100)

        binding.root.postDelayed({
            binding.chartCard.startAnimation(slideUp)
        }, 300)

        binding.root.postDelayed({
            binding.summaryCard.startAnimation(slideUp)
        }, 500)

        binding.root.postDelayed({
            binding.exportCard.startAnimation(slideUp)
        }, 700)
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTab = tab.position

                // Animate content change
                binding.dataContainer.animate()
                    .alpha(0f)
                    .setDuration(150)
                    .withEndAction {
                        updateContent()
                        binding.dataContainer.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                    }
                    .start()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupPeriodSelection() {
        binding.chipThisMonth.setOnClickListener {
            selectedPeriod = "This Month"
            updateChipSelection(binding.chipThisMonth)
            startDate = getStartDateForPeriod(selectedPeriod)
            endDate = System.currentTimeMillis()
            updateContent()
        }

        binding.chipLastMonth.setOnClickListener {
            selectedPeriod = "Last Month"
            updateChipSelection(binding.chipLastMonth)
            startDate = getStartDateForPeriod(selectedPeriod)
            endDate = getEndDateForPeriod(selectedPeriod)
            updateContent()
        }

        binding.chipLast3Months.setOnClickListener {
            selectedPeriod = "Last 3 Months"
            updateChipSelection(binding.chipLast3Months)
            startDate = getStartDateForPeriod(selectedPeriod)
            endDate = System.currentTimeMillis()
            updateContent()
        }

        binding.chipThisYear.setOnClickListener {
            selectedPeriod = "This Year"
            updateChipSelection(binding.chipThisYear)
            startDate = getStartDateForPeriod(selectedPeriod)
            endDate = System.currentTimeMillis()
            updateContent()
        }

        // Add custom date range selection
        binding.chipCustom.setOnClickListener {
            showDateRangePicker()
        }

        // Set default selection
        updateChipSelection(binding.chipThisMonth)
    }

    private fun showDateRangePicker() {
        val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .setSelection(androidx.core.util.Pair(
                customStartDate.takeIf { it > 0 }
                    ?: System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
                customEndDate.takeIf { it > 0 } ?: System.currentTimeMillis()
            ))
            .build()

        dateRangePicker.addOnPositiveButtonClickListener { dateRange ->
            customStartDate = dateRange.first
            customEndDate = dateRange.second
            selectedPeriod = "Custom"
            updateChipSelection(binding.chipCustom)
            startDate = customStartDate
            endDate = customEndDate
            updateContent()
        }

        dateRangePicker.show(supportFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun updateChipSelection(selectedChip: View) {
        binding.chipThisMonth.isChecked = selectedChip == binding.chipThisMonth
        binding.chipLastMonth.isChecked = selectedChip == binding.chipLastMonth
        binding.chipLast3Months.isChecked = selectedChip == binding.chipLast3Months
        binding.chipThisYear.isChecked = selectedChip == binding.chipThisYear
        binding.chipCustom.isChecked = selectedChip == binding.chipCustom
    }

    private fun setupExportButtons() {
        binding.btnExportCsv.setOnClickListener {
            if (checkStoragePermission()) {
                exportTransactionsToCSV()
            } else {
                requestStoragePermission()
            }
        }

        binding.btnExportPdf.setOnClickListener {
            if (checkStoragePermission()) {
                exportTransactionsToPDF()
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun updateContent() {
        when (selectedTab) {
            0 -> showSpendingAnalysis() // Spending Analysis tab
            1 -> showReports() // Reports tab
        }
    }

    private fun showSpendingAnalysis() {
        binding.analysisSectionTitle.text = "Category Breakdown"
        binding.lineChart.visibility = View.GONE
        binding.pieChart.visibility = View.VISIBLE

        // Get date range for header
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDateStr = dateFormat.format(Date(startDate))
        val endDateStr = dateFormat.format(Date(endDate))
        binding.tvDateRange.text = "$startDateStr - $endDateStr"

        // Get expense data by category
        val expensesByCategory = transactionRepository.getExpensesByCategory(startDate, endDate)
        val totalExpense = expensesByCategory.values.sum()

        // Format total expense
        val currencySymbol = prefsManager.getCurrencySymbol()
        binding.tvTotalAmount.text = CurrencyFormatter.formatAmount(totalExpense, currencySymbol)

        if (expensesByCategory.isEmpty() || totalExpense == 0.0) {
            showEmptyState()
            return
        }

        showDataState()

        // Create enhanced pie chart
        setupPieChart(binding.pieChart)

        // Create pie chart data with improved visuals
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        // Sort categories by amount for better visual hierarchy
        val sortedCategories = expensesByCategory.entries.sortedByDescending { it.value }

        sortedCategories.forEach { (category, amount) ->
            val percentage = if (totalExpense > 0) (amount / totalExpense * 100).toFloat() else 0f
            entries.add(PieEntry(percentage, category))
            colors.add(getCategoryColor(category))
        }

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 14f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter()
            valueLineColor = Color.WHITE
            valueLinePart1Length = 0.3f
            valueLinePart2Length = 0.4f
            valueLineWidth = 1f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            sliceSpace = 3f
            selectionShift = 5f
            iconsOffset = MPPointF(0f, 20f)
        }

        val pieData = PieData(dataSet)

        binding.pieChart.apply {
            data = pieData
            animateY(1400, Easing.EaseInOutQuad)
            invalidate()
        }

        // Update category expenses list with improved adapter
        val categoryExpenses = sortedCategories.map { (category, amount) ->
            CategoryExpense(
                category = category,
                amount = amount,
                percentage = if (totalExpense > 0) (amount / totalExpense * 100).toFloat() else 0f
            )
        }

        binding.rvCategoryExpenses.layoutManager = LinearLayoutManager(this)
        val adapter = CategoryExpenseAdapter(this, categoryExpenses, currencySymbol)
        binding.rvCategoryExpenses.adapter = adapter

        // Update summary section with animations
        updateFinancialSummary(startDate, endDate)
    }

    private fun setupPieChart(pieChart: PieChart) {
        pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setExtraOffsets(5f, 10f, 5f, 5f)
            dragDecelerationFrictionCoef = 0.95f

            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setTransparentCircleColor(Color.WHITE)
            setTransparentCircleAlpha(110)

            setDrawCenterText(true)
            centerText = "Expenses"
            setCenterTextSize(16f)
            setCenterTextColor(ContextCompat.getColor(context, R.color.text_primary))

            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            // Legend styling
            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 7f
                yEntrySpace = 0f
                yOffset = 10f
                textSize = 11f
                textColor = ContextCompat.getColor(context, R.color.text_primary)
            }

            setEntryLabelColor(Color.WHITE)
            setEntryLabelTextSize(12f)
        }
    }

    private fun showReports() {
        binding.analysisSectionTitle.text = "Income vs Expense Trend"
        binding.pieChart.visibility = View.GONE
        binding.lineChart.visibility = View.VISIBLE

        // Get date range for header
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDateStr = dateFormat.format(Date(startDate))
        val endDateStr = dateFormat.format(Date(endDate))
        binding.tvDateRange.text = "$startDateStr - $endDateStr"

        // Get enhanced data for line chart based on period
        val data = when (selectedPeriod) {
            "This Month", "Last Month" -> transactionRepository.getWeeklyData(4)
            "Last 3 Months" -> transactionRepository.getMonthlyData(3)
            "This Year" -> transactionRepository.getMonthlyData(12)
            "Custom" -> {
                // For custom dates, determine appropriate interval
                val daysDiff = (endDate - startDate) / (24 * 60 * 60 * 1000)
                when {
                    daysDiff <= 31 -> transactionRepository.getWeeklyData(4, startDate, endDate)
                    daysDiff <= 90 -> transactionRepository.getMonthlyData(3, startDate, endDate)
                    else -> transactionRepository.getMonthlyData(12, startDate, endDate)
                }
            }

            else -> transactionRepository.getMonthlyData(6)
        }

        if (data.isEmpty()) {
            showEmptyState()
            return
        }

        showDataState()

        // Set up enhanced line chart
        setupLineChart(binding.lineChart)

        // Create line chart data
        val incomeEntries = ArrayList<Entry>()
        val expenseEntries = ArrayList<Entry>()
        val labels = ArrayList<String>()

        var totalIncome = 0.0
        var totalExpense = 0.0

        data.forEachIndexed { index, periodData ->
            when (periodData) {
                is TransactionRepository.MonthlyData -> {
                    incomeEntries.add(Entry(index.toFloat(), periodData.income.toFloat()))
                    expenseEntries.add(Entry(index.toFloat(), periodData.expense.toFloat()))
                    labels.add(periodData.month)
                    totalIncome += periodData.income
                    totalExpense += periodData.expense
                }

                is TransactionRepository.WeeklyData -> {
                    incomeEntries.add(Entry(index.toFloat(), periodData.income.toFloat()))
                    expenseEntries.add(Entry(index.toFloat(), periodData.expense.toFloat()))
                    labels.add(periodData.week)
                    totalIncome += periodData.income
                    totalExpense += periodData.expense
                }
            }
        }

        val incomeDataSet = LineDataSet(incomeEntries, "Income").apply {
            color = ContextCompat.getColor(this@AnalysisActivity, R.color.success)
            lineWidth = 3f
            setCircleColor(ContextCompat.getColor(this@AnalysisActivity, R.color.success))
            circleRadius = 5f
            circleHoleRadius = 2.5f
            setDrawCircleHole(true)
            valueTextSize = 10f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable = ContextCompat.getDrawable(
                this@AnalysisActivity,
                R.drawable.line_chart_gradient_green
            )
        }

        val expenseDataSet = LineDataSet(expenseEntries, "Expense").apply {
            color = ContextCompat.getColor(this@AnalysisActivity, R.color.expense)
            lineWidth = 3f
            setCircleColor(ContextCompat.getColor(this@AnalysisActivity, R.color.expense))
            circleRadius = 5f
            circleHoleRadius = 2.5f
            setDrawCircleHole(true)
            valueTextSize = 10f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillDrawable =
                ContextCompat.getDrawable(this@AnalysisActivity, R.drawable.line_chart_gradient_red)
        }

        val dataSets = ArrayList<ILineDataSet>()
        dataSets.add(incomeDataSet)
        dataSets.add(expenseDataSet)

        val lineData = LineData(dataSets)

        binding.lineChart.apply {
            this.data = lineData
            animateX(1500, Easing.EaseInOutQuart)
        }

        // Update summary section
        val currencySymbol = prefsManager.getCurrencySymbol()
        binding.tvIncomeAmount.text = CurrencyFormatter.formatAmount(totalIncome, currencySymbol)
        binding.tvExpenseAmount.text = CurrencyFormatter.formatAmount(totalExpense, currencySymbol)
        binding.tvBalanceAmount.text =
            CurrencyFormatter.formatAmount(totalIncome - totalExpense, currencySymbol)
        binding.tvTotalAmount.text = CurrencyFormatter.formatAmount(totalIncome, currencySymbol)

        // Update Financial Summary
        updateFinancialSummary(startDate, endDate)
    }

    private fun setupLineChart(lineChart: LineChart) {
        lineChart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.WHITE)

            // X Axis styling
            val xAxis = xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = ContextCompat.getColor(context, R.color.text_secondary)
            xAxis.textSize = 12f
            xAxis.setDrawAxisLine(true)
            xAxis.axisLineColor = Color.parseColor("#E0E0E0")
            xAxis.axisLineWidth = 1f

            // Left Y Axis styling
            val leftAxis = axisLeft
            leftAxis.setDrawGridLines(true)
            leftAxis.gridColor = Color.parseColor("#E0E0E0")
            leftAxis.gridLineWidth = 0.5f
            leftAxis.textColor = ContextCompat.getColor(context, R.color.text_secondary)
            leftAxis.textSize = 12f

            // Disable right Y axis
            axisRight.isEnabled = false

            // Legend styling
            legend.apply {
                form = Legend.LegendForm.LINE
                textSize = 12f
                textColor = ContextCompat.getColor(context, R.color.text_primary)
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }
        }
    }

    private fun updateFinancialSummary(startDate: Long, endDate: Long) {
        val transactions = transactionRepository.getTransactionsForPeriod(startDate, endDate)

        var totalIncome = 0.0
        var totalExpense = 0.0

        for (transaction in transactions) {
            if (transaction.type == TransactionType.INCOME) {
                totalIncome += transaction.amount
            } else {
                totalExpense += transaction.amount
            }
        }

        val balance = totalIncome - totalExpense
        val currencySymbol = prefsManager.getCurrencySymbol()

        // Animate changing text values
        animateTextChange(
            binding.tvIncomeAmount,
            CurrencyFormatter.formatAmount(totalIncome, currencySymbol)
        )
        animateTextChange(
            binding.tvExpenseAmount,
            CurrencyFormatter.formatAmount(totalExpense, currencySymbol)
        )
        animateTextChange(
            binding.tvBalanceAmount,
            CurrencyFormatter.formatAmount(balance, currencySymbol)
        )

        // Set balance text color based on value
        binding.tvBalanceAmount.setTextColor(
            ContextCompat.getColor(
                this,
                if (balance >= 0) R.color.success else R.color.expense
            )
        )
    }

    private fun animateTextChange(textView: TextView, newText: String) {
        textView.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                textView.text = newText
                textView.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
            .start()
    }

    private fun showEmptyState() {
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.dataContainer.visibility = View.GONE

        // Update the empty state depending on the tab
        val emptyStateMessage =
            binding.emptyStateContainer.findViewById<TextView>(R.id.tvEmptyMessage)
        when (selectedTab) {
            0 -> emptyStateMessage.text = "No expense data for this period"
            1 -> emptyStateMessage.text = "No transaction data for this period"
        }
    }

    private fun showDataState() {
        binding.emptyStateContainer.visibility = View.GONE
        binding.dataContainer.visibility = View.VISIBLE
    }

    private fun getCategoryColor(category: String): Int {
        return when (category.lowercase()) {
            "food" -> ContextCompat.getColor(this, R.color.category_food)
            "transport" -> ContextCompat.getColor(this, R.color.category_transport)
            "bills" -> ContextCompat.getColor(this, R.color.category_bills)
            "entertainment" -> ContextCompat.getColor(this, R.color.category_entertainment)
            "shopping" -> ContextCompat.getColor(this, R.color.category_shopping)
            "health" -> ContextCompat.getColor(this, R.color.category_health)
            "education" -> ContextCompat.getColor(this, R.color.category_education)
            else -> ContextCompat.getColor(this, R.color.category_other)
        }
    }

    private fun getStartDateForPeriod(period: String): Long {
        val calendar = Calendar.getInstance()

        when (period) {
            "This Month" -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }

            "Last Month" -> {
                calendar.add(Calendar.MONTH, -1)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }

            "Last 3 Months" -> {
                calendar.add(Calendar.MONTH, -3)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }

            "This Year" -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }

            "Custom" -> {
                return customStartDate
            }
        }

        return calendar.timeInMillis
    }

    private fun getEndDateForPeriod(period: String): Long {
        val calendar = Calendar.getInstance()

        when (period) {
            "Last Month" -> {
                // Set to current month day 1
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                // Then subtract 1 millisecond to get end of previous month
                calendar.add(Calendar.MILLISECOND, -1)
            }

            "Custom" -> {
                return customEndDate
            }

            else -> {
                // Use current time for other periods
                return System.currentTimeMillis()
            }
        }

        return calendar.timeInMillis
    }

    // CSV Export Implementation
    private fun exportTransactionsToCSV() {
        val startDate = this.startDate
        val endDate = this.endDate

        val transactions = transactionRepository.getTransactionsForPeriod(startDate, endDate)

        if (transactions.isEmpty()) {
            showSnackbar("No transactions to export")
            return
        }

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
            val timestamp = dateFormat.format(Date())

            // Get appropriate directory
            val csvFolder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File(getExternalFilesDir(null), "Spendly/Reports")
            } else {
                File(Environment.getExternalStorageDirectory(), "Spendly/Reports")
            }

            if (!csvFolder.exists()) {
                csvFolder.mkdirs()
            }

            val fileName = "spendly_transactions_${timestamp}.csv"
            val csvFile = File(csvFolder, fileName)

            val success = exportTransactionsToCSV(transactions, csvFile)

            if (success) {
                // Share the CSV file
                val fileUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",
                    csvFile
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    type = "text/csv"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                showSuccessDialog(
                    "CSV Export Successful",
                    "Your transactions have been exported to CSV format.",
                    fileUri
                )
            } else {
                showSnackbar("Failed to export CSV. Please try again.")
            }
        } catch (e: Exception) {
            showSnackbar("Export failed: ${e.localizedMessage}")
            Log.e("AnalysisActivity", "CSV export error", e)
        }
    }

    // PDF Export Implementation
    private fun exportTransactionsToPDF() {
        val startDate = this.startDate
        val endDate = this.endDate

        val transactions = transactionRepository.getTransactionsForPeriod(startDate, endDate)

        if (transactions.isEmpty()) {
            showSnackbar("No transactions to export")
            return
        }

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
            val timestamp = dateFormat.format(Date())

            // Get appropriate directory
            val pdfFolder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                File(getExternalFilesDir(null), "Spendly/Reports")
            } else {
                File(Environment.getExternalStorageDirectory(), "Spendly/Reports")
            }

            if (!pdfFolder.exists()) {
                pdfFolder.mkdirs()
            }

            val fileName = "spendly_report_${timestamp}.pdf"
            val pdfFile = File(pdfFolder, fileName)

            // Create PDF document
            val document = PdfDocument()

            // Page configuration
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Generate PDF content
            generatePdfContent(canvas, paint, transactions)

            document.finishPage(page)

            // Save the document
            val fos = FileOutputStream(pdfFile)
            document.writeTo(fos)
            document.close()
            fos.close()

            // Share the PDF file
            val fileUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                pdfFile
            )

            showSuccessDialog(
                "PDF Report Generated",
                "Your financial report has been created as a PDF.",
                fileUri
            )

        } catch (e: Exception) {
            showSnackbar("PDF export failed: ${e.localizedMessage}")
            Log.e("AnalysisActivity", "PDF export error", e)
        }
    }

    private fun generatePdfContent(canvas: Canvas, paint: Paint, transactions: List<Transaction>) {
        // Set up text properties
        paint.color = Color.BLACK
        paint.textSize = 12f

        // Constants for positioning
        val startX = 40f
        val startY = 80f
        val lineHeight = 20f

        // Add header
        paint.textSize = 24f
        canvas.drawText("Spendly Financial Report", startX, startY, paint)

        // Add date range
        paint.textSize = 14f
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val dateRange =
            "${dateFormat.format(Date(startDate))} - ${dateFormat.format(Date(endDate))}"
        canvas.drawText("Period: $dateRange", startX, startY + 30f, paint)

        // Add user info
        val userName = "User: ${getUserName()}"
        canvas.drawText(userName, startX, startY + 50f, paint)

        // Add current date
        val currentDate = "Generated: ${dateFormat.format(Date())}"
        canvas.drawText(currentDate, startX, startY + 70f, paint)

        // Add summary
        var totalIncome = 0.0
        var totalExpense = 0.0

        transactions.forEach {
            if (it.type == TransactionType.INCOME) totalIncome += it.amount
            else totalExpense += it.amount
        }

        val balance = totalIncome - totalExpense
        val currencySymbol = prefsManager.getCurrencySymbol()

        paint.textSize = 16f
        paint.color = Color.parseColor("#1976D2") // Blue
        canvas.drawText("Summary", startX, startY + 110f, paint)

        // Draw summary table
        paint.textSize = 14f
        paint.color = Color.BLACK
        canvas.drawText(
            "Total Income: ${CurrencyFormatter.formatAmount(totalIncome, currencySymbol)}",
            startX, startY + 140f, paint
        )
        canvas.drawText(
            "Total Expense: ${CurrencyFormatter.formatAmount(totalExpense, currencySymbol)}",
            startX, startY + 160f, paint
        )

        paint.color =
            if (balance >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336") // Green or Red
        canvas.drawText(
            "Balance: ${CurrencyFormatter.formatAmount(balance, currencySymbol)}",
            startX, startY + 180f, paint
        )

        // Draw transactions table header
        paint.color = Color.parseColor("#1976D2")
        canvas.drawText("Transactions", startX, startY + 220f, paint)

        paint.color = Color.BLACK
        canvas.drawText("Date", startX, startY + 240f, paint)
        canvas.drawText("Category", startX + 100f, startY + 240f, paint)
        canvas.drawText("Description", startX + 200f, startY + 240f, paint)
        canvas.drawText("Amount", startX + 400f, startY + 240f, paint)

        // Draw line under headers
        paint.strokeWidth = 1f
        canvas.drawLine(startX, startY + 250f, startX + 500f, startY + 250f, paint)

        // Draw transactions
        paint.color = Color.BLACK
        var y = startY + 270f

        val transactionDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        for (transaction in transactions.take(25)) { // Limit to first 25 transactions
            val dateStr = transactionDateFormat.format(Date(transaction.date))
            canvas.drawText(dateStr, startX, y, paint)
            canvas.drawText(transaction.category, startX + 100f, y, paint)

            // Truncate description if too long
            val description = if (transaction.title.length > 25) transaction.title.substring(
                0,
                22
            ) + "..." else transaction.title
            canvas.drawText(description, startX + 200f, y, paint)

            // Draw amount with appropriate color
            paint.color = if (transaction.type == TransactionType.INCOME)
                Color.parseColor("#4CAF50") else Color.parseColor("#F44336")

            val amountStr = if (transaction.type == TransactionType.INCOME)
                "+${CurrencyFormatter.formatAmount(transaction.amount, currencySymbol)}"
            else
                "-${CurrencyFormatter.formatAmount(transaction.amount, currencySymbol)}"

            canvas.drawText(amountStr, startX + 400f, y, paint)

            paint.color = Color.BLACK
            y += lineHeight
        }

        // Add footer if there are more transactions
        if (transactions.size > 25) {
            paint.textSize = 12f
            paint.color = Color.parseColor("#757575")
            canvas.drawText(
                "... and ${transactions.size - 25} more transactions",
                startX, y + 20f, paint
            )
        }

        // Add footer
        paint.textSize = 12f
        paint.color = Color.parseColor("#757575")
        canvas.drawText(
            "Generated by Spendly - Personal Finance Manager",
            startX, 800f, paint
        )
    }

    private fun getUserName(): String {
        return "luqmanbooso" // Get actual username if available
    }

    private fun showSuccessDialog(title: String, message: String, fileUri: Uri) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Share") { _, _ ->
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    type = if (title.contains("CSV")) "text/csv" else "application/pdf"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share File"))
            }
            .setNeutralButton("View") { _, _ ->
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = fileUri
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(viewIntent)
            }
            .setNegativeButton("Close", null)
            .show()
    }


    private fun checkStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        } else {
            val write = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            val read = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            return write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, STORAGE_PERMISSION_CODE)
            } catch (e: Exception) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, STORAGE_PERMISSION_CODE)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                WRITE_PERMISSION_CODE
            )
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

}