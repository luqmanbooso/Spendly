package com.example.spendly

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.spendly.databinding.ActivityAnalysisBinding
import com.github.mikephil.charting.animation.Easing
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
import com.google.android.material.tabs.TabLayout
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
    }

    private fun playAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.tabLayout.startAnimation(fadeIn)
        binding.periodChipGroup.startAnimation(fadeIn)

        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        binding.chartCard.startAnimation(slideUp)

        binding.root.postDelayed({
            binding.summaryCard.startAnimation(slideUp)
        }, 200)

        binding.root.postDelayed({
            binding.exportCard.startAnimation(slideUp)
        }, 400)
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                selectedTab = tab.position
                updateContent()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupPeriodSelection() {
        binding.chipThisMonth.setOnClickListener {
            selectedPeriod = "This Month"
            updateChipSelection(binding.chipThisMonth)
            updateContent()
        }

        binding.chipLastMonth.setOnClickListener {
            selectedPeriod = "Last Month"
            updateChipSelection(binding.chipLastMonth)
            updateContent()
        }

        binding.chipLast3Months.setOnClickListener {
            selectedPeriod = "Last 3 Months"
            updateChipSelection(binding.chipLast3Months)
            updateContent()
        }

        binding.chipThisYear.setOnClickListener {
            selectedPeriod = "This Year"
            updateChipSelection(binding.chipThisYear)
            updateContent()
        }

        // Set default selection
        updateChipSelection(binding.chipThisMonth)
    }

    private fun updateChipSelection(selectedChip: View) {
        binding.chipThisMonth.isChecked = selectedChip == binding.chipThisMonth
        binding.chipLastMonth.isChecked = selectedChip == binding.chipLastMonth
        binding.chipLast3Months.isChecked = selectedChip == binding.chipLast3Months
        binding.chipThisYear.isChecked = selectedChip == binding.chipThisYear
    }

    private fun setupExportButtons() {
        binding.btnExportCsv.setOnClickListener {
            exportTransactionsToCSV()
        }

        binding.btnExportPdf.setOnClickListener {
            // For now, just show a toast
            Toast.makeText(this, "PDF Export coming soon!", Toast.LENGTH_SHORT).show()
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

        val startDate = getStartDateForPeriod(selectedPeriod)
        val endDate = System.currentTimeMillis()

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

        // Create pie chart data
        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        expensesByCategory.forEach { (category, amount) ->
            val percentage = if (totalExpense > 0) (amount / totalExpense * 100).toFloat() else 0f
            entries.add(PieEntry(percentage, category))
            colors.add(getCategoryColor(category))
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueFormatter = PercentFormatter()

        val pieData = PieData(dataSet)

        binding.pieChart.apply {
            data = pieData
            description.isEnabled = false
            setUsePercentValues(true)
            isDrawHoleEnabled = true
            setHoleColor(Color.WHITE)
            holeRadius = 58f
            transparentCircleRadius = 61f
            setDrawEntryLabels(false)
            legend.isEnabled = true
            legend.textSize = 12f
            legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }

        // Update category expenses list
        val categoryExpenses = expensesByCategory.map { (category, amount) ->
            CategoryExpense(
                category = category,
                amount = amount,
                percentage = if (totalExpense > 0) (amount / totalExpense * 100).toFloat() else 0f
            )
        }.sortedByDescending { it.amount }

        binding.rvCategoryExpenses.layoutManager = LinearLayoutManager(this)
        val adapter = CategoryExpenseAdapter(this, categoryExpenses, currencySymbol)
        binding.rvCategoryExpenses.adapter = adapter

        // Update summary section
        val totalIncome = transactionRepository.getTransactionsForPeriod(startDate, endDate)
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }

        binding.tvIncomeAmount.text = CurrencyFormatter.formatAmount(totalIncome, currencySymbol)
        binding.tvExpenseAmount.text = CurrencyFormatter.formatAmount(totalExpense, currencySymbol)
        binding.tvBalanceAmount.text = CurrencyFormatter.formatAmount(totalIncome - totalExpense, currencySymbol)
    }

    private fun showReports() {
        binding.analysisSectionTitle.text = "Income vs Expense Trend"
        binding.pieChart.visibility = View.GONE
        binding.lineChart.visibility = View.VISIBLE

        val startDate = getStartDateForPeriod(selectedPeriod)
        val endDate = System.currentTimeMillis()

        // Get date range for header
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDateStr = dateFormat.format(Date(startDate))
        val endDateStr = dateFormat.format(Date(endDate))
        binding.tvDateRange.text = "$startDateStr - $endDateStr"

        // Get data for line chart based on period
        val data = when (selectedPeriod) {
            "This Month", "Last Month" -> transactionRepository.getWeeklyData(4)
            "Last 3 Months" -> transactionRepository.getMonthlyData(3)
            "This Year" -> transactionRepository.getMonthlyData(12)
            else -> transactionRepository.getMonthlyData(6)
        }

        if (data.isEmpty()) {
            showEmptyState()
            return
        }

        showDataState()

        // Update line chart
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
            circleRadius = 4f
            setCircleColor(ContextCompat.getColor(this@AnalysisActivity, R.color.success))
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@AnalysisActivity, R.color.success)
            fillAlpha = 30
            valueTextSize = 10f
        }

        val expenseDataSet = LineDataSet(expenseEntries, "Expense").apply {
            color = ContextCompat.getColor(this@AnalysisActivity, R.color.expense)
            circleRadius = 4f
            setCircleColor(ContextCompat.getColor(this@AnalysisActivity, R.color.expense))
            lineWidth = 2f
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = ContextCompat.getColor(this@AnalysisActivity, R.color.expense)
            fillAlpha = 30
            valueTextSize = 10f
        }

        val dataSets = ArrayList<ILineDataSet>()
        dataSets.add(incomeDataSet)
        dataSets.add(expenseDataSet)

        val lineData = LineData(dataSets)

        binding.lineChart.apply {
            this.data = lineData
            description.isEnabled = false
            legend.isEnabled = true
            setTouchEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            val xAxis = xAxis
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.granularity = 1f
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)

            axisRight.isEnabled = false

            animateX(1500, Easing.EaseInOutQuad)
        }

        // Update summary section
        val currencySymbol = prefsManager.getCurrencySymbol()
        binding.tvIncomeAmount.text = CurrencyFormatter.formatAmount(totalIncome, currencySymbol)
        binding.tvExpenseAmount.text = CurrencyFormatter.formatAmount(totalExpense, currencySymbol)
        binding.tvBalanceAmount.text = CurrencyFormatter.formatAmount(totalIncome - totalExpense, currencySymbol)
        binding.tvTotalAmount.text = CurrencyFormatter.formatAmount(totalIncome, currencySymbol)
    }

    private fun showEmptyState() {
        binding.emptyStateContainer.visibility = View.VISIBLE
        binding.dataContainer.visibility = View.GONE
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
        }

        return calendar.timeInMillis
    }

    private fun exportTransactionsToCSV() {
        val startDate = getStartDateForPeriod(selectedPeriod)
        val endDate = System.currentTimeMillis()

        val transactions = transactionRepository.getTransactionsForPeriod(startDate, endDate)

        if (transactions.isEmpty()) {
            Toast.makeText(this, "No transactions to export", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val csvFile = backupHelper.exportTransactionsAsCSV(transactions)

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

            startActivity(Intent.createChooser(shareIntent, "Share CSV Report"))

        } catch (e: Exception) {
            Toast.makeText(this, "Failed to export: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}