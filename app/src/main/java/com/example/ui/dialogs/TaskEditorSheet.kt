package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.SubTask
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import com.example.ui.AiOperationState
import com.example.ui.theme.AiSparkleGlow
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.PriorityUrgent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    initialTask: TaskItem?,
    defaultCategory: String,
    availableCategories: List<String>,
    aiBreakdownState: AiOperationState<com.example.network.TaskBreakdownResult>,
    onRequestAiBreakdown: (title: String, description: String) -> Unit,
    onResetAiBreakdown: () -> Unit,
    onSave: (TaskItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initialTask?.title ?: "") }
    var description by remember { mutableStateOf(initialTask?.description ?: "") }
    var selectedCategory by remember { mutableStateOf(initialTask?.category ?: defaultCategory) }
    var selectedPriority by remember { mutableStateOf(initialTask?.priority ?: TaskPriority.MEDIUM) }
    var estimatedMinutes by remember { mutableIntStateOf(initialTask?.estimatedMinutes ?: 30) }
    var aiTips by remember { mutableStateOf(initialTask?.aiTips ?: "") }
    var newSubtaskInput by remember { mutableStateOf("") }
    var newCategoryInput by remember { mutableStateOf("") }
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    val subtasks = remember {
        mutableStateListOf<SubTask>().apply {
            if (initialTask != null) {
                addAll(initialTask.subtasks)
            }
        }
    }

    // React to AI breakdown success
    LaunchedEffect(aiBreakdownState) {
        if (aiBreakdownState is AiOperationState.Success) {
            val result = aiBreakdownState.data
            subtasks.clear()
            subtasks.addAll(result.subtasks)
            if (result.tips.isNotBlank()) {
                aiTips = result.tips
            }
            if (result.suggestedPriority != null) {
                selectedPriority = result.suggestedPriority
            }
            if (result.suggestedDurationMinutes != null) {
                estimatedMinutes = result.suggestedDurationMinutes
            }
            onResetAiBreakdown()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (initialTask == null) "Nouvelle tâche" else "Modifier la tâche",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Titre de la tâche *") },
                placeholder = { Text("Ex: Finaliser le rapport de projet") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_editor_title_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Contexte") },
                placeholder = { Text("Ajoutez des détails ou objectifs spécifiques...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_editor_desc_input"),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // AI Breakdown Assistant Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = AiSparkleGlow.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AiSparkleGlow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Découpage intelligent par l'IA",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Générer les étapes et estimer le temps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (aiBreakdownState is AiOperationState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AiSparkleGlow,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onRequestAiBreakdown(title, description)
                                }
                            },
                            enabled = title.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AiSparkleGlow,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("trigger_ai_breakdown_btn")
                        ) {
                            Text("Découper", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (aiBreakdownState is AiOperationState.Error) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = aiBreakdownState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Selector
            Text(
                text = "Priorité",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { priority ->
                    val color = when (priority) {
                        TaskPriority.LOW -> PriorityLow
                        TaskPriority.MEDIUM -> PriorityMedium
                        TaskPriority.HIGH -> PriorityHigh
                        TaskPriority.URGENT -> PriorityUrgent
                    }
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick = { selectedPriority = priority },
                        label = { Text(priority.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color.copy(alpha = 0.2f),
                            selectedLabelColor = color
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector
            Text(
                text = "Catégorie",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory.equals(cat, ignoreCase = true),
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Estimated Duration
            Text(
                text = "Temps estimé : $estimatedMinutes min",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 45, 60, 90, 120).forEach { mins ->
                    FilterChip(
                        selected = estimatedMinutes == mins,
                        onClick = { estimatedMinutes = mins },
                        label = { Text("$mins min") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtasks Section
            Text(
                text = "Sous-tâches (${subtasks.size})",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            subtasks.forEachIndexed { index, sub ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = sub.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { subtasks.removeAt(index) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer sous-tâche",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Add manual subtask input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newSubtaskInput,
                    onValueChange = { newSubtaskInput = it },
                    placeholder = { Text("Ajouter une étape manuelle...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newSubtaskInput.isNotBlank()) {
                            subtasks.add(SubTask(title = newSubtaskInput.trim(), isDone = false))
                            newSubtaskInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Ajouter sous-tâche",
                        tint = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons: Save & Cancel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Annuler")
                }

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val taskToSave = (initialTask ?: TaskItem(title = title.trim())).copy(
                                title = title.trim(),
                                description = description.trim(),
                                category = selectedCategory,
                                priority = selectedPriority,
                                estimatedMinutes = estimatedMinutes,
                                aiTips = aiTips.ifBlank { null },
                                subtasks = subtasks.toList()
                            )
                            onSave(taskToSave)
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("task_editor_save_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        if (initialTask == null) "Créer la tâche" else "Enregistrer",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
