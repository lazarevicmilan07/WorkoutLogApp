package com.workoutlog.ui.screens.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.domain.model.MonthlyCountData
import com.workoutlog.domain.model.WorkoutTypeCountData
import com.workoutlog.ui.components.DashStatCard
import com.workoutlog.ui.components.EmptyState
import com.workoutlog.ui.components.LoadingIndicator
import com.workoutlog.ui.components.MonthYearPickerDialog
import com.workoutlog.ui.components.YearPickerDialog
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun ReportsScreen(
    initialIsMonthly: Boolean = true,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPeriodPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.setMonthly(initialIsMonthly)
    }

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    val dragOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    fun animatePrevious() {
        scope.launch {
            dragOffset.animateTo(screenWidthPx, tween(150))
            viewModel.previousPeriod()
            dragOffset.snapTo(-screenWidthPx)
            dragOffset.animateTo(0f, tween(200))
        }
    }

    fun animateNext() {
        scope.launch {
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
            // Period selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { animatePrevious() }) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showPeriodPicker = true },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.isMonthly)
                            YearMonth.of(state.selectedYear, state.selectedMonth)
                                .format(DateTimeFormatter.ofPattern("MMMM"))
                        else
                            "${state.selectedYear}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (state.isMonthly) "${state.selectedYear}" else "Annual",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { animateNext() }) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.primary
                    )
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

// ─────────────────────────────────────────────────────────
// Monthly content
// ─────────────────────────────────────────────────────────

@Composable
fun MonthlyReportContent(state: ReportsUiState) {
    val report = state.monthlyReport

    if (report == null || report.totalWorkouts == 0) {
        EmptyState(title = "No workouts this month", subtitle = "Start logging to see reports")
        return
    }

    val ym = YearMonth.of(report.year, report.month)
    val currentYm = YearMonth.now()
    val daysElapsed = when {
        ym > currentYm -> 0
        ym == currentYm -> LocalDate.now().dayOfMonth
        else -> ym.lengthOfMonth()
    }
    val workoutCount = report.totalWorkouts - report.totalRestDays
    val workoutPercentage = if (daysElapsed > 0) workoutCount * 100 / daysElapsed else 0

    val favouriteWorkout = report.workoutTypeCounts
        .filter { !it.workoutType.isRestDay }
        .maxByOrNull { it.count }
        ?.workoutType?.name ?: "—"
    val purpleAccent = Color(0xFF8B5CF6)
    val amberAccent = MaterialTheme.colorScheme.tertiary

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 2×2 dashboard-style stat cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashStatCard(
                        icon = {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = Color(0xFF4CAF6A),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        accentColor = Color(0xFF4CAF6A),
                        label = "Workouts",
                        value = "$workoutCount / $daysElapsed",
                        modifier = Modifier.weight(1f)
                    )
                    DashStatCard(
                        icon = {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = null,
                                tint = Color(0xFF5B8DEE),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        accentColor = Color(0xFF5B8DEE),
                        label = "Consistency",
                        value = "$workoutPercentage%",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashStatCard(
                        icon = {
                            Icon(
                                Icons.Default.Hotel,
                                contentDescription = null,
                                tint = purpleAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        accentColor = purpleAccent,
                        label = "Rest Days",
                        value = "${report.totalRestDays}",
                        modifier = Modifier.weight(1f)
                    )
                    DashStatCard(
                        icon = {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = amberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        accentColor = amberAccent,
                        label = "Favourite",
                        value = favouriteWorkout,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Workout distribution — donut + legend side by side
        if (report.workoutTypeCounts.isNotEmpty()) {
            item {
                StatsCard(title = "Distribution") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(
                            data = report.workoutTypeCounts,
                            modifier = Modifier.size(140.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        DistributionLegend(
                            data = report.workoutTypeCounts,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Yearly content
// ─────────────────────────────────────────────────────────

@Composable
fun YearlyReportContent(state: ReportsUiState) {
    val report = state.yearlyReport

    if (report == null || report.totalWorkouts == 0) {
        EmptyState(title = "No workouts this year", subtitle = "Start logging to see reports")
        return
    }

    val activeMonths = report.monthlyCounts.count { it.count > 0 }
    val avgPerMonth = if (activeMonths > 0) (report.totalWorkouts.toFloat() / activeMonths).roundToInt() else 0
    val bestMonthData = report.monthlyCounts.maxByOrNull { it.count }
    val bestMonthName = if (bestMonthData != null && bestMonthData.count > 0) {
        Month.of(bestMonthData.month).getDisplayName(JTextStyle.SHORT, Locale.getDefault())
    } else "—"

    val blueAccent = MaterialTheme.colorScheme.primary
    val purpleAccent = Color(0xFF8B5CF6)
    val amberAccent = MaterialTheme.colorScheme.tertiary
    val greenAccent = Color(0xFF10B981)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 2×2 metric tiles
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Workouts",
                        value = "${report.totalWorkouts}",
                        icon = Icons.Default.FitnessCenter,
                        accentColor = blueAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Rest Days",
                        value = "${report.totalRestDays}",
                        icon = Icons.Default.Hotel,
                        accentColor = purpleAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        label = "Best Month",
                        value = bestMonthName,
                        icon = Icons.Default.EmojiEvents,
                        accentColor = amberAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        label = "Avg / Month",
                        value = if (avgPerMonth > 0) "$avgPerMonth" else "—",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        accentColor = greenAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Monthly activity bar chart
        item {
            StatsCard(title = "Monthly Activity") {
                MonthlyBarChart(
                    data = report.monthlyCounts,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
        }

        // Workout distribution
        if (report.workoutTypeCounts.isNotEmpty()) {
            item {
                StatsCard(title = "Distribution") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DonutChart(
                            data = report.workoutTypeCounts,
                            modifier = Modifier.size(130.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        DistributionLegend(
                            data = report.workoutTypeCounts,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// Shared composables
// ─────────────────────────────────────────────────────────

@Composable
fun MetricTile(
    label: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
fun DonutChart(
    data: List<WorkoutTypeCountData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val total = data.sumOf { it.count }
    var selectedIndex by remember(data) { mutableStateOf(-1) }
    val strokeWidth = 70f

    Box(
        modifier = modifier
            .clip(CircleShape)
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val canvasPx = minOf(size.width, size.height).toFloat()
                    val diameter = canvasPx - strokeWidth - 4f
                    val outerRadius = diameter / 2f + strokeWidth / 2f
                    val innerRadius = diameter / 2f - strokeWidth / 2f
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val dx = offset.x - cx
                    val dy = offset.y - cy
                    val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                    when {
                        dist < innerRadius -> selectedIndex = -1
                        dist <= outerRadius -> {
                            var angle = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat() + 90f
                            if (angle < 0f) angle += 360f
                            var startAngle = 0f
                            var hit = -1
                            data.forEachIndexed { index, item ->
                                val sweep = (item.count / total.toFloat()) * 360f
                                if (angle >= startAngle && angle < startAngle + sweep) hit = index
                                startAngle += sweep
                            }
                            selectedIndex = if (hit == selectedIndex) -1 else hit
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = minOf(size.width, size.height) - strokeWidth - 4f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            var startAngle = -90f
            data.forEachIndexed { index, item ->
                val sweep = (item.count / total.toFloat()) * 360f
                val alpha = if (selectedIndex == -1 || selectedIndex == index) 1f else 0.2f
                drawArc(
                    color = item.workoutType.color.copy(alpha = alpha),
                    startAngle = startAngle,
                    sweepAngle = sweep - 1.5f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
        if (selectedIndex == -1) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$total",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val selected = data[selectedIndex]
            val pct = (selected.count / total.toFloat() * 100).toInt()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${selected.count}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = selected.workoutType.color
                )
                Text(
                    text = "($pct%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = selected.workoutType.color
                )
            }
        }
    }
}

@Composable
fun DistributionLegend(
    data: List<WorkoutTypeCountData>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.count }.toFloat()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        data.forEach { item ->
            val fraction = item.count / total
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(item.workoutType.color)
                    )
                    Text(
                        text = item.workoutType.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${item.count} (${(fraction * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Mini progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(item.workoutType.color.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(item.workoutType.color)
                    )
                }
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
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxCount = data.maxOfOrNull { it.count } ?: 1
    val monthLabels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
    val dataMap = data.associate { it.month to it.count }

    Canvas(modifier = modifier) {
        val barWidth = size.width / 12f
        val labelHeight = 24f
        val chartHeight = size.height - labelHeight
        val cornerRadius = CornerRadius(5f, 5f)
        val paint = android.graphics.Paint().apply {
            textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            color = android.graphics.Color.argb(
                (labelColor.alpha * 255).toInt(),
                (labelColor.red * 255).toInt(),
                (labelColor.green * 255).toInt(),
                (labelColor.blue * 255).toInt()
            )
        }

        for (index in 0..11) {
            val month = index + 1
            val count = dataMap[month] ?: 0
            val x = index * barWidth
            val barLeft = x + barWidth * 0.2f
            val barW = barWidth * 0.6f

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(barLeft, 0f),
                size = Size(barW, chartHeight),
                cornerRadius = cornerRadius
            )

            if (count > 0) {
                val barHeight = (count.toFloat() / maxCount) * chartHeight
                drawRoundRect(
                    color = primary,
                    topLeft = Offset(barLeft, chartHeight - barHeight),
                    size = Size(barW, barHeight),
                    cornerRadius = cornerRadius
                )
            }

            drawContext.canvas.nativeCanvas.drawText(
                monthLabels[index],
                x + barWidth / 2f,
                size.height,
                paint
            )
        }
    }
}

