package com.example.data

import com.example.model.SubTask
import com.example.model.Task
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTaskById(id: Long): Flow<Task?> = taskDao.getTaskById(id)

    suspend fun insert(task: Task): Long = taskDao.insertTask(task)

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun update(task: Task) = taskDao.updateTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun delete(task: Task) = taskDao.deleteTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)

    suspend fun deleteById(id: Long) = taskDao.deleteTaskById(id)

    suspend fun deleteCompleted() = taskDao.deleteCompletedTasks()

    suspend fun toggleSubtask(task: Task, subtaskId: String) {
        val updatedSubtasks = task.subtasks.map {
            if (it.id == subtaskId) it.copy(isDone = !it.isDone) else it
        }
        val allDone = updatedSubtasks.isNotEmpty() && updatedSubtasks.all { it.isDone }
        val newStatus = if (allDone) {
            TaskStatus.COMPLETED
        } else if (updatedSubtasks.any { it.isDone }) {
            TaskStatus.IN_PROGRESS
        } else {
            task.status
        }
        val isCompleted = newStatus == TaskStatus.COMPLETED
        taskDao.updateTask(task.copy(subtasks = updatedSubtasks, status = newStatus, isCompleted = isCompleted))
    }

    suspend fun advanceStatus(task: Task) {
        val nextStatus = when (task.status) {
            TaskStatus.TODO -> TaskStatus.IN_PROGRESS
            TaskStatus.IN_PROGRESS -> TaskStatus.COMPLETED
            TaskStatus.COMPLETED -> TaskStatus.TODO
        }
        val isCompleted = nextStatus == TaskStatus.COMPLETED
        val updatedSubtasks = if (nextStatus == TaskStatus.COMPLETED) {
            task.subtasks.map { it.copy(isDone = true) }
        } else if (nextStatus == TaskStatus.TODO && task.status == TaskStatus.COMPLETED) {
            task.subtasks.map { it.copy(isDone = false) }
        } else {
            task.subtasks
        }
        taskDao.updateTask(task.copy(status = nextStatus, isCompleted = isCompleted, subtasks = updatedSubtasks))
    }

    suspend fun seedInitialDataIfEmpty() {
        val count = taskDao.getTaskCount()
        if (count == 0) {
            val starterTasks = listOf(
                Task(
                    title = "Bienvenue sur TaskFlow Ai 🚀",
                    description = "Explorez la gestion intelligente de tâches avec assistance IA et flux automatisé.",
                    dueDate = System.currentTimeMillis() + 86400000L,
                    isCompleted = false,
                    status = TaskStatus.IN_PROGRESS,
                    priority = TaskPriority.HIGH,
                    category = "Général",
                    estimatedMinutes = 10,
                    aiTips = "Appuyez sur 'Générer avec l'IA' pour décomposer n'importe quel objectif complexe en étapes claires !",
                    subtasks = listOf(
                        SubTask(title = "Explorer le tableau de bord des flux", isDone = true),
                        SubTask(title = "Tester le générateur de tâche par prompt IA", isDone = false),
                        SubTask(title = "Consulter le Copilote IA pour planifier la journée", isDone = false)
                    )
                ),
                Task(
                    title = "Préparer la réunion d'équipe trimestrielle",
                    description = "Synthétiser les indicateurs clés et l'avancement des projets.",
                    dueDate = System.currentTimeMillis() + 172800000L,
                    isCompleted = false,
                    status = TaskStatus.TODO,
                    priority = TaskPriority.URGENT,
                    category = "Travail",
                    estimatedMinutes = 45,
                    aiTips = "Commencez par les 3 points clés à décider avant d'aborder les détails opérationnels.",
                    subtasks = listOf(
                        SubTask(title = "Rassembler les KPI de performance", isDone = false),
                        SubTask(title = "Créer les 5 diapositives de synthèse", isDone = false),
                        SubTask(title = "Envoyer l'ordre du jour aux participants", isDone = false)
                    )
                ),
                Task(
                    title = "Séance de renforcement musculaire et cardio",
                    description = "Session de 40 minutes pour maintenir l'énergie et la concentration.",
                    dueDate = null,
                    isCompleted = false,
                    status = TaskStatus.TODO,
                    priority = TaskPriority.MEDIUM,
                    category = "Santé",
                    estimatedMinutes = 40,
                    aiTips = "Hydratez-vous bien avant et commencez par 5 min d'échauffement articulaire.",
                    subtasks = listOf(
                        SubTask(title = "Échauffement 5 min", isDone = false),
                        SubTask(title = "Circuit training 30 min", isDone = false),
                        SubTask(title = "Étirements et retour au calme 5 min", isDone = false)
                    )
                ),
                Task(
                    title = "Lire 20 pages d'un livre d'architecture logicielle",
                    description = "Apprentissage continu sur les bonnes pratiques et la résilience.",
                    dueDate = null,
                    isCompleted = true,
                    status = TaskStatus.COMPLETED,
                    priority = TaskPriority.LOW,
                    category = "Études",
                    estimatedMinutes = 25,
                    aiTips = "Prenez 2 notes concrètes applicables directement dans vos projets.",
                    subtasks = listOf(
                        SubTask(title = "Lire les chapitres 3 et 4", isDone = true),
                        SubTask(title = "Noter les concepts essentiels", isDone = true)
                    )
                )
            )
            taskDao.insertTasks(starterTasks)
        }
    }
}
