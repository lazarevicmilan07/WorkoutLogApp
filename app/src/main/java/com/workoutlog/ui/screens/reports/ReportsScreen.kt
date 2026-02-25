package com.workoutlog.ui.screens.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.domain.model.DailyCountData
import com.workoutlog.domain.model.MonthlyCountData
import com.workoutlog.domain.model.WorkoutTypeCountData
import com.workoutlog.ui.components.EmptyState
import com.workoutlog.ui.components.LoadingIndicator
import com.workoutlog.ui.components.MonthYearPickerDialog
import com.workoutlog.ui.components.YearPickerDialog
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ReportsScreen(
    initialIsMonthly: Boolean = true,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPeriodPicker by remember { mutableStateOf(false) }

    // Set mode once on first composition
    LaunchedEffect(Unit) {
        viewModel.setMonthly(initialIsMonthly)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    // Swipe animation for period changes
    val dragOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    fun animatePrevious() {
        scope.launch {
            // Previous: slide out RIGHT, come in from LEFT
            dragOffset.animateTo(screenWidthPx, tween(150))
            viewModel.previousPeriod()
            dragOffset.snapTo(-screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }

    fun animateNext() {
        scope.launch {
            // Next: slide out LEFT, come in from RIGHT
            dragOffset.animateTo(-screenWidthPx, tween(150))
            viewModel.nextPeriod()
            dragOffset.snapTo(screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Period selector with swipe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { animatePrevious() }) {
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
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { showPeriodPicker = true }
                )
                IconButton(onClick = { animateNext() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                }
            }

            // Content with swipe gesture
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { scope.launch { dragOffset.stop() } },
                            onDragEnd = {
                                scope.launch {
                                    when {
                                        dragOffset.value > 100 -> {
                                            dragOffset.animateTo(screenWidthPx, tween(150))
                                            viewModel.previousPeriod()
                                            dragOffset.snapTo(-screenWidthPx)
                                            dragOffset.animateTo(0f, tween(200))
                                        }
                                        dragOffset.value < -100 -> {
                                            dragOffset.animateTo(-screenWidthPx, tween(150))
                                            viewModel.nextPeriod()
                                            dragOffset.snapTo(screenWidthPx)
                                            dragOffset.animateTo(0f, tween(200))
                                        }
                                        else -> dragOffset.animateTo(0f, tween(150))
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch { dragOffset.animateTo(0f, tween(150)) }
                            },
                            onHorizontalDrag = { _, delta ->
                                scope.launch { dragOffset.snapTo(dragOffset.value + delta) }
                            }
                        )
                    }
            ) {
                Box(modifier = Modifier.offset { IntOffset(dragOffset.value.roundToInt(), 0) }) {
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
    }

    // Period picker dialog
    if (showPeriodPicker) {
        if (state.isMonthly) {
            MonthYearPickerDialog(
                currentMonth = YearMonth.of(state.selectedYear, state.selectedMonth),
                onDismiss = { showPeriodPicker = false },
                onConfirm = { yearMonth ->
                    val currentYm = YearMonth.of(state.selectedYear, state.selectedMonth)
                    if (yearMonth != currentYm) {
                        val goingBack = yearMonth < currentYm
                        scope.launch {
                            dragOffset.animateTo(
                                if (goingBack) screenWidthPx else -screenWidthPx,
                                tween(150)
                            )
                            viewModel.goToPeriod(yearMonth.year, yearMonth.monthValue)
                            dragOffset.snapTo(if (goingBack) -screenWidthPx else screenWidthPx)
                            dragOffset.animateTo(0f, tween(200))
                        }
                    }
                    showPeriodPicker = false
                }
            )
        } else {
            YearPickerDialog(
                currentYear = state.selectedYear,
                onDismiss = { showPeriodPicker = false },
                onConfirm = { year ->
                    if (year != state.selectedYear) {
                        val goingBack = year < state.selectedYear
                        scope.launch {
                            dragOffset.animateTo(
                                if (goingBack) screenWidthPx else -screenWidthPx,
                                tween(150)
                            )
                            viewModel.setYear(year)
                            dragOffset.snapTo(if (goingBack) -screenWidthPx else screenWidthPx)
                            dragOffset.animateTo(0f, tween(200))
                        }
                    }
                    showPeriodPicker = false
                }
            )
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
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
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard("Duration", "${report.totalDuration}m", Modifier.weight(1f))
                SummaryCard("Calories", "${report.totalCalories}", Modifier.weight(1f))
            }
        }

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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
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
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val arcSize = Size(diameter, diameter)
        var startAngle = -90f
        data.forEach { item ->
            val sweep = (item.count / total) * 360f
            drawArc(
                color = item.workoutType.color,
                startAngle = startAngle,
                sweepAngle = sweep - 2f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 44f, cap = StrokeCap.Butt)
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
    val maxCount = data.maxOfOrNull { it.count } ?: 1

    Canvas(modifier = modifier) {
        val barWidth = size.width / daysInMonth
        val dataMap = data.associate { it.day to it.count }
        for (day in 1..daysInMonth) {
            val count = dataMap[day] ?: 0
            val barHeight = if (maxCount > 0) (count.toFloat() / maxCount) * (size.height - 20f) else 0f
            val x = (day - 1) * barWidth
            drawRect(
                color = surfaceVariant,
                topLeft = Offset(x + barWidth * 0.15f, 0f),
                size = Size(barWidth * 0.7f, size.height - 20f)
            )
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
