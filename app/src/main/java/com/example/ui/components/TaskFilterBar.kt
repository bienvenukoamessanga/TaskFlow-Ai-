package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.model.TaskStatus

@Composable
fun TaskFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedStatus: TaskStatus?,
    onStatusSelect: (TaskStatus?) -> Unit,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
    selectedPriority: TaskPriority?,
    onPrioritySelect: (TaskPriority?) -> Unit,
    categories: List<String>,
    allTasks: List<TaskItem>,
    modifier: Modifier = Modifier
) {
    val scrollStateCategory = rememberScrollState()
    val scrollStateStatus = rememberScrollState()

    Column(modifier = modifier.fillMaxWidth()) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_tasks_input"),
            placeholder = { Text("Rechercher une tâche, un mot-clé...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Rechercher",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Effacer la recherche"
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollStateStatus),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedStatus == null,
                onClick = { onStatusSelect(null) },
                label = { Text("Toutes (${allTasks.size})") },
                modifier = Modifier.testTag("filter_status_all"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )

            val todoCount = allTasks.count { it.status == TaskStatus.TODO }
            FilterChip(
                selected = selectedStatus == TaskStatus.TODO,
                onClick = { onStatusSelect(if (selectedStatus == TaskStatus.TODO) null else TaskStatus.TODO) },
                label = { Text("À faire ($todoCount)") },
                modifier = Modifier.testTag("filter_status_todo")
            )

            val inProgressCount = allTasks.count { it.status == TaskStatus.IN_PROGRESS }
            FilterChip(
                selected = selectedStatus == TaskStatus.IN_PROGRESS,
                onClick = { onStatusSelect(if (selectedStatus == TaskStatus.IN_PROGRESS) null else TaskStatus.IN_PROGRESS) },
                label = { Text("En cours ($inProgressCount)") },
                modifier = Modifier.testTag("filter_status_in_progress")
            )

            val completedCount = allTasks.count { it.status == TaskStatus.COMPLETED }
            FilterChip(
                selected = selectedStatus == TaskStatus.COMPLETED,
                onClick = { onStatusSelect(if (selectedStatus == TaskStatus.COMPLETED) null else TaskStatus.COMPLETED) },
                label = { Text("Terminées ($completedCount)") },
                modifier = Modifier.testTag("filter_status_completed")
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollStateCategory),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelect(null) },
                label = { Text("Toutes catégories") }
            )

            categories.forEach { cat ->
                val catCount = allTasks.count { it.category.equals(cat, ignoreCase = true) }
                FilterChip(
                    selected = selectedCategory.equals(cat, ignoreCase = true),
                    onClick = {
                        onCategorySelect(if (selectedCategory.equals(cat, ignoreCase = true)) null else cat)
                    },
                    label = { Text("$cat ($catCount)") }
                )
            }
        }
    }
}
