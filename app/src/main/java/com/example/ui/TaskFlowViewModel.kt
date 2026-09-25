package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.TaskRepository
import com.example.model.SubTask
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import com.example.network.DailyPlanResult
import com.example.network.GeminiApiClient
import com.example.network.MagicTaskResult
import com.example.network.TaskBreakdownResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class SheetType {
    object None : SheetType()
    data class AddTask(val defaultCategory: String = "Général") : SheetType()
    data class EditTask(val task: TaskItem) : SheetType()
    data class TaskDetail(val task: TaskItem) : SheetType()
    object AiMagic : SheetType()
    object AiPlanner : SheetType()
}

sealed class AiOperationState<out T> {
    object Idle : AiOperationState<Nothing>()
    object Loading : AiOperationState<Nothing>()
    data class Success<T>(val data: T) : AiOperationState<T>()
    data class Error(val message: String) : AiOperationState<Nothing>()
}

class TaskFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    private val geminiClient = GeminiApiClient()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TaskRepository(db.taskDao())
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    val allTasks: StateFlow<List<TaskItem>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<TaskStatus?>(null)
    val statusFilter = _statusFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter = _categoryFilter.asStateFlow()

    private val _priorityFilter = MutableStateFlow<TaskPriority?>(null)
    val priorityFilter = _priorityFilter.asStateFlow()

    private val _currentSheet = MutableStateFlow<SheetType>(SheetType.None)
    val currentSheet = _currentSheet.asStateFlow()

    private val _snackBarMessage = MutableStateFlow<String?>(null)
    val snackBarMessage = _snackBarMessage.asStateFlow()

    // AI States
    private val _aiBreakdownState = MutableStateFlow<AiOperationState<TaskBreakdownResult>>(AiOperationState.Idle)
    val aiBreakdownState = _aiBreakdownState.asStateFlow()

    private val _aiMagicState = MutableStateFlow<AiOperationState<MagicTaskResult>>(AiOperationState.Idle)
    val aiMagicState = _aiMagicState.asStateFlow()

    private val _aiPlannerState = MutableStateFlow<AiOperationState<DailyPlanResult>>(AiOperationState.Idle)
    val aiPlannerState = _aiPlannerState.asStateFlow()

    val filteredTasks: StateFlow<List<TaskItem>> = combine(
        allTasks,
        _searchQuery,
        _statusFilter,
        _categoryFilter,
        _priorityFilter
    ) { tasks, query, status, category, priority ->
        tasks.filter { task ->
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true) ||
                    task.category.contains(query, ignoreCase = true)

            val matchesStatus = status == null || task.status == status
            val matchesCategory = category == null || task.category.equals(category, ignoreCase = true)
            val matchesPriority = priority == null || task.priority == priority

            matchesQuery && matchesStatus && matchesCategory && matchesPriority
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = combine(allTasks) { tasksArray ->
        val defaultCats = listOf("Général", "Travail", "Personnel", "Études", "Santé", "Projets")
        val existing = tasksArray[0].map { it.category }.filter { it.isNotBlank() }
        (defaultCats + existing).distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: TaskStatus?) {
        _statusFilter.value = status
    }

    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    fun setPriorityFilter(priority: TaskPriority?) {
        _priorityFilter.value = priority
    }

    fun openSheet(sheet: SheetType) {
        _currentSheet.value = sheet
    }

    fun closeSheet() {
        _currentSheet.value = SheetType.None
    }

    fun clearSnackbar() {
        _snackBarMessage.value = null
    }

    fun showSnackbar(msg: String) {
        _snackBarMessage.value = msg
    }

    // Task CRUD
    fun saveTask(task: TaskItem) {
        viewModelScope.launch {
            if (task.id == 0L) {
                repository.insert(task)
                showSnackbar("Tâche créée avec succès !")
            } else {
                repository.update(task)
                showSnackbar("Tâche mise à jour !")
            }
            closeSheet()
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            repository.delete(task)
            showSnackbar("Tâche supprimée.")
            if (_currentSheet.value is SheetType.TaskDetail || _currentSheet.value is SheetType.EditTask) {
                closeSheet()
            }
        }
    }

    fun advanceStatus(task: TaskItem) {
        viewModelScope.launch {
            repository.advanceStatus(task)
        }
    }

    fun toggleSubtask(task: TaskItem, subtaskId: String) {
        viewModelScope.launch {
            repository.toggleSubtask(task, subtaskId)
        }
    }

    fun addSubtaskToTask(task: TaskItem, title: String) {
        if (title.isBlank()) return
        val newSubtask = SubTask(title = title.trim(), isDone = false)
        val updatedList = task.subtasks + newSubtask
        viewModelScope.launch {
            repository.update(task.copy(subtasks = updatedList))
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            repository.deleteCompleted()
            showSnackbar("Toutes les tâches terminées ont été nettoyées.")
        }
    }

    // AI Actions
    fun requestAiBreakdown(title: String, description: String) {
        viewModelScope.launch {
            _aiBreakdownState.value = AiOperationState.Loading
            val result = geminiClient.breakdownTask(title, description)
            result.fold(
                onSuccess = { breakdown ->
                    _aiBreakdownState.value = AiOperationState.Success(breakdown)
                },
                onFailure = { error ->
                    _aiBreakdownState.value = AiOperationState.Error(
                        error.message ?: "Impossible de contacter l'IA. Vérifiez la clé API."
                    )
                }
            )
        }
    }

    fun resetAiBreakdown() {
        _aiBreakdownState.value = AiOperationState.Idle
    }

    fun requestMagicTask(prompt: String) {
        viewModelScope.launch {
            _aiMagicState.value = AiOperationState.Loading
            val result = geminiClient.magicCreateTask(prompt)
            result.fold(
                onSuccess = { magic ->
                    _aiMagicState.value = AiOperationState.Success(magic)
                },
                onFailure = { error ->
                    _aiMagicState.value = AiOperationState.Error(
                        error.message ?: "Erreur de génération par l'IA."
                    )
                }
            )
        }
    }

    fun resetAiMagic() {
        _aiMagicState.value = AiOperationState.Idle
    }

    fun requestDailyPlan() {
        viewModelScope.launch {
            _aiPlannerState.value = AiOperationState.Loading
            val pending = allTasks.value.filter { it.status != TaskStatus.COMPLETED }
            val result = geminiClient.generateDailyFlowPlan(pending)
            result.fold(
                onSuccess = { plan ->
                    _aiPlannerState.value = AiOperationState.Success(plan)
                },
                onFailure = { error ->
                    _aiPlannerState.value = AiOperationState.Error(
                        error.message ?: "Échec du planificateur IA."
                    )
                }
            )
        }
    }

    fun resetDailyPlan() {
        _aiPlannerState.value = AiOperationState.Idle
    }
}
