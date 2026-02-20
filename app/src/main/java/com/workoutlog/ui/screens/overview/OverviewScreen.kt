package com.workoutlog.ui.screens.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.ui.components.LoadingIndicator
import com.workoutlog.ui.components.StatCard
import com.workoutlog.ui.screens.home.WorkoutEntryItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun OverviewScreen(
    onAddEntry: (String?) -> Unit,
    onEditEntry: (Long) -> Unit,
    viewModel: OverviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onAddEntry(null) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add workout")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingIndicator()
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // Month selector
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
                    }
                    Text(
                        text = state.currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
                    }
                }
            }

            // Stats
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "Workouts",
                        value = "${state.totalWorkouts}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Rest Days",
                        value = "${state.totalRestDays}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Top Type",
                        value = state.mostCommonType?.name ?: "-",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Filter chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = state.selectedFilter == null,
                            onClick = { viewModel.setFilter(null) },
                            label = { Text("All") }
                        )
                    }
                    items(state.workoutTypes) { type ->
                        FilterChip(
                            selected = state.selectedFilter == type.id,
                            onClick = { viewModel.setFilter(type.id) },
                            label = { Text(type.name) }
                        )
                    }
                }
            }

            // Calendar grid
            item {
                MonthCalendar(
                    yearMonth = state.currentMonth,
                    entriesByDate = state.entriesByDate,
                    onDateClick = { date -> onAddEntry(date.toString()) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Entries list
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Workouts this month",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            val sortedEntries = state.entriesByDate.entries
                .sortedByDescending { it.key }
                .flatMap { it.value }

            items(sortedEntries, key = { it.id }) { entry ->
                WorkoutEntryItem(
                    entry = entry,
                    onClick = { onEditEntry(entry.id) },
                    onDelete = { viewModel.deleteEntry(entry) }
                )
            }
        }
    }
}

@Composable
fun MonthCalendar(
    yearMonth: YearMonth,
    entriesByDate: Map<LocalDate, List<com.workoutlog.domain.model.WorkoutEntry>>,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstDay = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startDayOfWeek = firstDay.dayOfWeek.value // 1=Monday
    val today = LocalDate.now()

    Column(modifier = modifier) {
        // Day of week headers
        Row(modifier = Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    text = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        // Calendar days
        val totalCells = startDayOfWeek - 1 + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - (startDayOfWeek - 1) + 1

                    if (dayNum in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayNum)
                        val hasEntries = entriesByDate.containsKey(date)
                        val isToday = date == today

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .then(
                                    if (isToday) Modifier.background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) else Modifier
                                )
                                .clickable { onDateClick(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$dayNum",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                if (hasEntries) {
                                    val entry = entriesByDate[date]?.firstOrNull()
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                entry?.workoutType?.color
                                                    ?: MaterialTheme.colorScheme.primary
                                            )
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
