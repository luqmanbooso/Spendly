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
import android.provider.Settings
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
    private var selectedTab = 0
    private var startDate: Long = 0
    private var endDate: Long = System.currentTimeMillis()

    private var customStartDate: Long = 0
    private var customEndDate: Long = 0

    private val STORAGE_PERMISSION_CODE = 101
    private val WRITE_PERMISSION_CODE = 102

    private var lastExportType: String = ""

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

        startDate = getStartDateForPeriod(selectedPeriod)
        updateContent()

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
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        fadeIn.duration = 500
        binding.tabLayout.startAnimation(fadeIn)

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
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTab = tab.position

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

        binding.chipCustom.setOnClickListener {
            showDateRangePicker()
        }

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

    private fun updateContent() {
        when (selectedTab) {
            0 -> showSpendingAnalysis()
            1 -> showReports()
        }
    }

    private fun showSpendingAnalysis() {
        binding.analysisSectionTitle.text = "Category Breakdown"
        binding.lineChart.visibility = View.GONE
        binding.pieChart.visibility = View.VISIBLE

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDateStr = dateFormat.format(Date(startDate))
        val endDateStr = dateFormat.format(Date(endDate))
        binding.tvDateRange.text = "$startDateStr - $endDateStr"

        val expensesByCategory = transactionRepository.getExpensesByCategory(startDate, endDate)
        val totalExpense = expensesByCategory.values.sum()

        val currencySymbol = prefsManager.getCurrencySymbol()
        binding.tvTotalAmount.text = CurrencyFormatter.formatAmount(totalExpense, currencySymbol)

        if (expensesByCategory.isEmpty() || totalExpense == 0.0) {
            showEmptyState()
            return
        }

        showDataState()

        setupPieChart(binding.pieChart)

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

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

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDateStr = dateFormat.format(Date(startDate))
        val endDateStr = dateFormat.format(Date(endDate))
        binding.tvDateRange.text = "$startDateStr - $endDateStr"

        val data = when (selectedPeriod) {
            "This Month", "Last Month" -> transactionRepository.getWeeklyData(4)
            "Last 3 Months" -> transactionRepository.getMonthlyData(3)
            "This Year" -> transactionRepository.getMonthlyData(12)
            "Custom" -> {
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

        setupLineChart(binding.lineChart)

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

        val currencySymbol = prefsManager.getCurrencySymbol()
        binding.tvIncomeAmount.text = CurrencyFormatter.formatAmount(totalIncome, currencySymbol)
        binding.tvExpenseAmount.text = CurrencyFormatter.formatAmount(totalExpense, currencySymbol)
        binding.tvBalanceAmount.text =
            CurrencyFormatter.formatAmount(totalIncome - totalExpense, currencySymbol)
        binding.tvTotalAmount.text = CurrencyFormatter.formatAmount(totalIncome, currencySymbol)

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

            val xAxis = xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.textColor = ContextCompat.getColor(context, R.color.text_secondary)
            xAxis.textSize = 12f
            xAxis.setDrawAxisLine(true)
            xAxis.axisLineColor = Color.parseColor("#E0E0E0")
            xAxis.axisLineWidth = 1f

            val leftAxis = axisLeft
            leftAxis.setDrawGridLines(true)
            leftAxis.gridColor = Color.parseColor("#E0E0E0")
            leftAxis.gridLineWidth = 0.5f
            leftAxis.textColor = ContextCompat.getColor(context, R.color.text_secondary)
            leftAxis.textSize = 12f

            axisRight.isEnabled = false

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
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
            }

            "Custom" -> {
                return customEndDate
            }

            else -> {
                return System.currentTimeMillis()
            }
        }

        return calendar.timeInMillis
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}