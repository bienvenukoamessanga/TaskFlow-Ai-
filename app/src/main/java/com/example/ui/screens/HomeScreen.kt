package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskFilterBar
import com.example.ui.components.TaskFlowStats
import com.example.ui.theme.AiSparkleGlow
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    allTasks: List<TaskItem>,
    filteredTasks: List<TaskItem>,
    categories: List<String>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedStatus: TaskStatus?,
    onStatusSelect: (TaskStatus?) -> Unit,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    selectedPriority: TaskPriority?,
    onPrioritySelect: (TaskPriority?) -> Unit,
    onAdvanceStatus: (TaskItem) -> Unit,
    onToggleSubtask: (TaskItem, String) -> Unit,
    onEditTask: (TaskItem) -> Unit,
    onDeleteTask: (TaskItem) -> Unit,
    onAiBreakdown: (TaskItem) -> Unit,
    onOpenAiPlanner: () -> Unit,
    onOpenAiMagic: () -> Unit,
    onAddNewTask: () -> Unit,
    onClearCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Top Bar
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(listOf(PrimaryIndigo, SecondaryCyan))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TaskAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "TaskFlow",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AiSparkleGlow.copy(alpha = 0.18f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = AiSparkleGlow,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "AI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AiSparkleGlow,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                actions = {
                    val hasCompleted = allTasks.any { it.status == TaskStatus.COMPLETED }
                    if (hasCompleted) {
                        IconButton(
                            onClick = onClearCompleted,
                            modifier = Modifier.testTag("clear_completed_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = "Nettoyer les tâches terminées",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Content List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Stats Card
                item {
                    TaskFlowStats(
                        tasks = allTasks,
                        onOpenAiPlanner = onOpenAiPlanner,
                        onOpenAiMagic = onOpenAiMagic
                    )
                }

                // Filter & Search Bar
                item {
                    TaskFilterBar(
                        searchQuery = searchQuery,
                        onSearchChange = onSearchChange,
                        selectedStatus = selectedStatus,
                        onStatusSelect = onStatusSelect,
                        selectedCategory = selectedCategory,
                        onCategorySelect = onCategorySelect,
                        selectedPriority = selectedPriority,
                        onPrioritySelect = onPrioritySelect,
                        categories = categories,
                        allTasks = allTasks
                    )
                }

                // Empty State or Tasks List
                if (filteredTasks.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TaskAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isNotBlank() || selectedStatus != null || selectedCategory != null)
                                    "Aucune tâche trouvée avec ces filtres"
                                else
                                    "Votre flux de tâches est complètement vide !",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Créez une tâche ou utilisez le créateur magique IA.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onOpenAiMagic,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Essayer le Créateur IA")
                            }
                        }
                    }
                } else {
                    items(
                        items = filteredTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onStatusAdvance = { onAdvanceStatus(task) },
                            onSubtaskToggle = { subId -> onToggleSubtask(task, subId) },
                            onEdit = { onEditTask(task) },
                            onDelete = { onDeleteTask(task) },
                            onAiBreakdown = { onAiBreakdown(task) }
                        )
                    }
                }
            }
        }

        // Floating Action Buttons (Add task + Magic AI)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Secondary AI Magic FAB
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AiSparkleGlow,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onOpenAiMagic() }
                    .testTag("ai_magic_fab")
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(listOf(PrimaryIndigo, AiSparkleGlow))
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Créateur Magique IA",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Créer avec IA",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Primary Add Task FAB
            FloatingActionButton(
                onClick = onAddNewTask,
                containerColor = PrimaryIndigo,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajouter une tâche manuelle",
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
