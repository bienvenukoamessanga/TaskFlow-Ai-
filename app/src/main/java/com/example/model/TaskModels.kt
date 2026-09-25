package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class TaskStatus(val displayName: String) {
    TODO("À faire"),
    IN_PROGRESS("En cours"),
    COMPLETED("Terminé")
}

enum class TaskPriority(val displayName: String, val level: Int) {
    LOW("Basse", 1),
    MEDIUM("Moyenne", 2),
    HIGH("Élevée", 3),
    URGENT("Urgente", 4)
}

data class SubTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val isDone: Boolean = false
)

/**
 * Room database entity representing a task.
 * Contains fields for id, title, description, dueDate, and isCompleted (completion status).
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val status: TaskStatus = if (isCompleted) TaskStatus.COMPLETED else TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: String = "Général",
    val estimatedMinutes: Int = 30,
    val createdAt: Long = System.currentTimeMillis(),
    val aiTips: String? = null,
    val subtasks: List<SubTask> = emptyList()
) {
    val completionStatus: Boolean
        get() = isCompleted || status == TaskStatus.COMPLETED

    val completedSubtasksCount: Int
        get() = subtasks.count { it.isDone }

    val totalSubtasksCount: Int
        get() = subtasks.size

    val progressPercent: Float
        get() = if (subtasks.isEmpty()) {
            if (isCompleted || status == TaskStatus.COMPLETED) 1f else if (status == TaskStatus.IN_PROGRESS) 0.5f else 0f
        } else {
            completedSubtasksCount.toFloat() / totalSubtasksCount.toFloat()
        }
}

typealias TaskItem = Task
