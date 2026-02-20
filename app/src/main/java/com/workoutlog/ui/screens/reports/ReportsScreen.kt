package com.workoutlog.ui.screens.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.domain.model.DailyCountData
import com.workoutlog.domain.model.MonthlyCountData
import com.workoutlog.domain.model.WorkoutTypeCountData
import com.workoutlog.ui.components.EmptyState
import com.workoutlog.ui.components.LoadingIndicator
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh data when returning to this screen
    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ReportsEvent.ExportSuccess -> snackbarHostState.showSnackbar(event.message)
                is ReportsEvent.ExportError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab selector
            TabRow(
                selectedTabIndex = if (state.isMonthly) 0 else 1
            ) {
                Tab(
                    selected = state.isMonthly,
                    onClick = { viewModel.setMonthly(true) },
                    text = { Text("Monthly") }
                )
                Tab(
                    selected = !state.isMonthly,
                    onClick = { viewModel.setMonthly(false) },
                    text = { Text("Yearly") }
                )
            }

            // Period selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousPeriod() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                }
                Text(
                    text = if (state.isMonthly) {
                        YearMonth.of(state.selectedYear, state.selectedMonth)
                            .format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    } else {
                        "${state.selectedYear}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { viewModel.nextPeriod() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                }
            }

            // Export buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    TextButton(onClick = { viewModel.exportToExcel() }) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Excel")
                    }
                    TextButton(onClick = { viewModel.exportToPdf() }) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("PDF")
                    }
                }
            }

            if (state.isLoading) {
                LoadingIndicator()
            } else if (state.isMonthly) {
                MonthlyReportContent(state)
            } else {
                YearlyReportContent(state)
            }
        }
    }
}

@Composable
fun MonthlyReportContent(state: ReportsUiState) {
    val report = state.monthlyReport

    if (report == null || report.totalWorkouts == 0) {
        EmptyState(title = "No workouts this month", subtitle = "Start logging to see reports")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Summary stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard("Workouts", "${report.totalWorkouts}", Modifier.weight(1f))
                SummaryCard("Rest Days", "${report.totalRestDays}", Modifier.weight(1f))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard("Duration", "${report.totalDuration}m", Modifier.weight(1f))
                SummaryCard("Calories", "${report.totalCalories}", Modifier.weight(1f))
            }
        }

        // Pie chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Workout Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(16.dp))
                    PieChart(
                        data = report.workoutTypeCounts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    ChartLegend(data = report.workoutTypeCounts)
                }
            }
        }

        // Bar chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Workouts by Day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(16.dp))
                    DailyBarChart(
                        data = report.dailyCounts,
                        daysInMonth = YearMonth.of(report.year, report.month).lengthOfMonth(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun YearlyReportContent(state: ReportsUiState) {
    val report = state.yearlyReport

    if (report == null || report.totalWorkouts == 0) {
        EmptyState(title = "No workouts this year", subtitle = "Start logging to see reports")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard("Workouts", "${report.totalWorkouts}", Modifier.weight(1f))
                SummaryCard("Rest Days", "${report.totalRestDays}", Modifier.weight(1f))
            }
        }

        // Monthly bar chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Workouts per Month",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(16.dp))
                    MonthlyBarChart(
                        data = report.monthlyCounts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }

        // Pie chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Workout Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(16.dp))
                    PieChart(
                        data = report.workoutTypeCounts,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    ChartLegend(data = report.workoutTypeCounts)
                }
            }
        }
    }
}

@Composable
fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PieChart(
    data: List<WorkoutTypeCountData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val total = data.sumOf { it.count }.toFloat()

    Canvas(modifier = modifier) {
        val diameter = minOf(size.width, size.height) * 0.8f
        val topLeft = Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f
        )
        val arcSize = Size(diameter, diameter)
        var startAngle = -90f

        data.forEach { item ->
            val sweep = (item.count / total) * 360f
            drawArc(
                color = item.workoutType.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 40f, cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

@Composable
fun ChartLegend(data: List<WorkoutTypeCountData>) {
    val total = data.sumOf { it.count }.toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        data.forEach { item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(item.workoutType.color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.workoutType.name,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${item.count} (${((item.count / total) * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DailyBarChart(
    data: List<DailyCountData>,
    daysInMonth: Int,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val maxCount = data.maxOfOrNull { it.count } ?: 1

    Canvas(modifier = modifier) {
        val barWidth = size.width / daysInMonth
        val dataMap = data.associate { it.day to it.count }

        for (day in 1..daysInMonth) {
            val count = dataMap[day] ?: 0
            val barHeight = if (maxCount > 0) (count.toFloat() / maxCount) * (size.height - 20f) else 0f
            val x = (day - 1) * barWidth

            // Background bar
            drawRect(
                color = surfaceVariant,
                topLeft = Offset(x + barWidth * 0.15f, 0f),
                size = Size(barWidth * 0.7f, size.height - 20f)
            )

            // Data bar
            if (count > 0) {
                drawRect(
                    color = primary,
                    topLeft = Offset(x + barWidth * 0.15f, size.height - 20f - barHeight),
                    size = Size(barWidth * 0.7f, barHeight)
                )
            }
        }
    }
}

@Composable
fun MonthlyBarChart(
    data: List<MonthlyCountData>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val maxCount = data.maxOfOrNull { it.count } ?: 1
    val months = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

    Canvas(modifier = modifier) {
        val barWidth = size.width / 12f
        val chartHeight = size.height - 24f

        data.forEachIndexed { index, item ->
            val barHeight = if (maxCount > 0) (item.count.toFloat() / maxCount) * chartHeight else 0f
            val x = index * barWidth

            drawRect(
                color = surfaceVariant,
                topLeft = Offset(x + barWidth * 0.2f, 0f),
                size = Size(barWidth * 0.6f, chartHeight)
            )

            if (item.count > 0) {
                drawRect(
                    color = primary,
                    topLeft = Offset(x + barWidth * 0.2f, chartHeight - barHeight),
                    size = Size(barWidth * 0.6f, barHeight)
                )
            }

            drawContext.canvas.nativeCanvas.drawText(
                months[index],
                x + barWidth / 2f,
                size.height,
                android.graphics.Paint().apply {
                    color = onSurfaceVariant.hashCode()
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}
