package com.workoutlog.ui.screens.workouttype

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.ui.components.EmptyState
import com.workoutlog.ui.components.LoadingIndicator
import com.workoutlog.ui.theme.getWorkoutIcon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTypesScreen(
    viewModel: WorkoutTypesViewModel = hiltViewModel(),
    typeViewModel: AddEditWorkoutTypeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showTypeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun openAddSheet() {
        typeViewModel.setup(-1L)
        showTypeSheet = true
    }

    fun openEditSheet(typeId: Long) {
        typeViewModel.setup(typeId)
        showTypeSheet = true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openAddSheet() },
                containerColor = Color(0xFF5E9260)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add workout type")
            }
        }
    ) { padding ->
        if (state.isLoading) {
            LoadingIndicator()
            return@Scaffold
        }

        if (state.types.isEmpty()) {
            EmptyState(
                title = "No workout types",
                subtitle = "Tap + to create your first workout type",
                modifier = Modifier.padding(padding)
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
        ) {
            item {
                Text(
                    text = "Workout Types",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(state.types, key = { it.id }) { type ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openEditSheet(type.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(type.color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getWorkoutIcon(type.icon),
                                contentDescription = null,
                                tint = type.color,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = type.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 60.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    // Add/Edit sheet
    if (showTypeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTypeSheet = false },
            sheetState = sheetState,
            dragHandle = null,
            shape = RoundedCornerShape(0.dp),
            contentWindowInsets = { WindowInsets(0) }
        ) {
            AddEditWorkoutTypeSheet(
                viewModel = typeViewModel,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showTypeSheet = false
                    }
                }
            )
        }
    }
}
