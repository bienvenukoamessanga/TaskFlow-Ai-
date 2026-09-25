package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.TaskItem
import com.example.ui.SheetType
import com.example.ui.TaskFlowViewModel
import com.example.ui.dialogs.AiMagicSheet
import com.example.ui.dialogs.AiPlannerSheet
import com.example.ui.dialogs.TaskEditorSheet
import com.example.ui.screens.AiCopilotScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.AiSparkleGlow
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.TaskFlowAiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskFlowAiTheme {
                TaskFlowApp()
            }
        }
    }
}

@Composable
fun TaskFlowApp(viewModel: TaskFlowViewModel = viewModel()) {
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val priorityFilter by viewModel.priorityFilter.collectAsStateWithLifecycle()
    val currentSheet by viewModel.currentSheet.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackBarMessage.collectAsStateWithLifecycle()

    val aiBreakdownState by viewModel.aiBreakdownState.collectAsStateWithLifecycle()
    val aiMagicState by viewModel.aiMagicState.collectAsStateWithLifecycle()
    val aiPlannerState by viewModel.aiPlannerState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.TaskAlt else Icons.Outlined.TaskAlt,
                            contentDescription = "Flux"
                        )
                    },
                    label = { Text("Flux") },
                    modifier = Modifier.testTag("nav_tab_tasks")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Default.AutoAwesome else Icons.Outlined.AutoAwesome,
                            contentDescription = "Copilote IA"
                        )
                    },
                    label = { Text("Copilote IA") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AiSparkleGlow,
                        selectedTextColor = AiSparkleGlow
                    ),
                    modifier = Modifier.testTag("nav_tab_copilot")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Default.BarChart else Icons.Outlined.BarChart,
                            contentDescription = "Statistiques"
                        )
                    },
                    label = { Text("Stats") },
                    modifier = Modifier.testTag("nav_tab_stats")
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreen(
                allTasks = allTasks,
                filteredTasks = filteredTasks,
                categories = categories,
                searchQuery = searchQuery,
                onSearchChange = viewModel::setSearchQuery,
                selectedStatus = statusFilter,
                onStatusSelect = viewModel::setStatusFilter,
                selectedCategory = categoryFilter,
                onCategorySelect = viewModel::setCategoryFilter,
                selectedPriority = priorityFilter,
                onPrioritySelect = viewModel::setPriorityFilter,
                onAdvanceStatus = viewModel::advanceStatus,
                onToggleSubtask = viewModel::toggleSubtask,
                onEditTask = { task -> viewModel.openSheet(SheetType.EditTask(task)) },
                onDeleteTask = viewModel::deleteTask,
                onAiBreakdown = { task ->
                    viewModel.openSheet(SheetType.EditTask(task))
                    viewModel.requestAiBreakdown(task.title, task.description)
                },
                onOpenAiPlanner = { viewModel.openSheet(SheetType.AiPlanner) },
                onOpenAiMagic = { viewModel.openSheet(SheetType.AiMagic) },
                onAddNewTask = { viewModel.openSheet(SheetType.AddTask()) },
                onClearCompleted = viewModel::clearCompletedTasks,
                modifier = Modifier.padding(innerPadding)
            )
            1 -> AiCopilotScreen(
                tasks = allTasks,
                onLaunchPlanner = { viewModel.openSheet(SheetType.AiPlanner) },
                onLaunchMagicCreator = { viewModel.openSheet(SheetType.AiMagic) },
                modifier = Modifier.padding(innerPadding)
            )
            2 -> StatsScreen(
                tasks = allTasks,
                onClearCompleted = viewModel::clearCompletedTasks,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    // Modal Bottom Sheets
    when (val sheet = currentSheet) {
        is SheetType.AddTask -> {
            TaskEditorSheet(
                initialTask = null,
                defaultCategory = sheet.defaultCategory,
                availableCategories = categories,
                aiBreakdownState = aiBreakdownState,
                onRequestAiBreakdown = viewModel::requestAiBreakdown,
                onResetAiBreakdown = viewModel::resetAiBreakdown,
                onSave = viewModel::saveTask,
                onDismiss = viewModel::closeSheet
            )
        }
        is SheetType.EditTask -> {
            TaskEditorSheet(
                initialTask = sheet.task,
                defaultCategory = sheet.task.category,
                availableCategories = categories,
                aiBreakdownState = aiBreakdownState,
                onRequestAiBreakdown = viewModel::requestAiBreakdown,
                onResetAiBreakdown = viewModel::resetAiBreakdown,
                onSave = viewModel::saveTask,
                onDismiss = viewModel::closeSheet
            )
        }
        is SheetType.AiMagic -> {
            AiMagicSheet(
                aiMagicState = aiMagicState,
                onRequestMagicTask = viewModel::requestMagicTask,
                onResetMagicState = viewModel::resetAiMagic,
                onConfirmTask = { task ->
                    viewModel.saveTask(task)
                    viewModel.closeSheet()
                },
                onDismiss = viewModel::closeSheet
            )
        }
        is SheetType.AiPlanner -> {
            AiPlannerSheet(
                aiPlannerState = aiPlannerState,
                onRequestDailyPlan = viewModel::requestDailyPlan,
                onDismiss = viewModel::closeSheet
            )
        }
        else -> {}
    }
}
