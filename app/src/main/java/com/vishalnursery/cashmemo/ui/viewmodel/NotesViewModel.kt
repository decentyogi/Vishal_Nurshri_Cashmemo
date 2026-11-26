package com.vishalnursery.cashmemo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vishalnursery.cashmemo.data.repository.NotesRepository
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.GeneralNote
import com.vishalnursery.cashmemo.domain.model.TodoFilter
import com.vishalnursery.cashmemo.domain.model.TodoPriority
import com.vishalnursery.cashmemo.domain.model.TodoTask
import com.vishalnursery.cashmemo.domain.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<GeneralNote> = emptyList(),
    val todos: List<TodoTask> = emptyList(),
    val totalNotesCount: Int = 0,
    val pendingTodosCount: Int = 0,
    val overdueTodosCount: Int = 0,
    val searchQuery: String = "",
    val todoFilter: TodoFilter = TodoFilter.ALL
)

class NotesViewModel(
    private val repository: NotesRepository
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val todoFilterFlow = MutableStateFlow(TodoFilter.ALL)

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.notesFlow,
                repository.todosFlow,
                searchQueryFlow,
                todoFilterFlow
            ) { allNotes, allTodos, query, filter ->
                val q = query.trim().lowercase()
                val todayDay = DateUtils.todayEpochDay()

                val filteredNotes = if (q.isEmpty()) {
                    allNotes
                } else {
                    allNotes.filter {
                        it.title.lowercase().contains(q) ||
                            it.body.lowercase().contains(q) ||
                            it.customerName.lowercase().contains(q)
                    }
                }.sortedWith(
                    compareByDescending<GeneralNote> { it.isPinned }
                        .thenByDescending { it.updatedAt }
                )

                val filteredTodos = allTodos.filter { task ->
                    val matchesFilter = when (filter) {
                        TodoFilter.ALL -> true
                        TodoFilter.PENDING -> !task.isDone
                        TodoFilter.OVERDUE -> !task.isDone && DateUtils.isOverdue(task.dueDate, todayDay)
                        TodoFilter.DONE -> task.isDone
                    }
                    val matchesQuery = q.isEmpty() ||
                        task.title.lowercase().contains(q) ||
                        task.customerName.lowercase().contains(q) ||
                        task.dueDate.lowercase().contains(q)
                    matchesFilter && matchesQuery
                }.sortedWith(
                    compareBy<TodoTask> { it.isDone }
                        .thenBy { if (!it.isDone) DateUtils.dateSortKey(it.dueDate) else 0L }
                        .thenByDescending {
                            if (!it.isDone) when (it.priority) {
                                TodoPriority.HIGH -> 3
                                TodoPriority.MEDIUM -> 2
                                TodoPriority.LOW -> 1
                            } else 0
                        }
                        .thenByDescending { if (it.isDone) it.doneAt else it.createdAt }
                )

                NotesUiState(
                    notes = filteredNotes,
                    todos = filteredTodos,
                    totalNotesCount = allNotes.size,
                    pendingTodosCount = allTodos.count { !it.isDone },
                    overdueTodosCount = allTodos.count { !it.isDone && DateUtils.isOverdue(it.dueDate, todayDay) },
                    searchQuery = query,
                    todoFilter = filter
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun updateSearchQuery(query: String) {
        searchQueryFlow.value = query
    }

    fun updateTodoFilter(filter: TodoFilter) {
        todoFilterFlow.value = filter
    }

    fun saveNote(note: GeneralNote) {
        viewModelScope.launch {
            repository.saveNote(note)
                .onSuccess { _messages.emit("Note saved") }
                .onFailure { _messages.emit("Save failed: ${it.localizedMessage}") }
        }
    }

    fun togglePinNote(note: GeneralNote) {
        viewModelScope.launch {
            repository.togglePinNote(note)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
                .onSuccess { _messages.emit("Note deleted") }
        }
    }

    fun saveTodo(todo: TodoTask) {
        viewModelScope.launch {
            repository.saveTodo(todo)
                .onSuccess { _messages.emit("Task saved") }
                .onFailure { _messages.emit("Save failed: ${it.localizedMessage}") }
        }
    }

    fun toggleTodoDone(todo: TodoTask) {
        viewModelScope.launch {
            repository.toggleTodoDone(todo)
        }
    }

    fun deleteTodo(id: String) {
        viewModelScope.launch {
            repository.deleteTodo(id)
                .onSuccess { _messages.emit("Task deleted") }
        }
    }

    fun createOrderReminder(order: CashMemo) {
        val label = buildString {
            append("Follow up Order #${order.memoNumber}")
            if (order.customerName.isNotBlank()) append(" (${order.customerName})")
        }
        saveTodo(
            TodoTask(
                title = label,
                dueDate = order.dueDate,
                priority = TodoPriority.HIGH,
                customerName = order.customerName
            )
        )
    }

    class Factory(
        private val repository: NotesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotesViewModel(repository) as T
        }
    }
}
