package com.vishalnursery.cashmemo.data.repository

import com.vishalnursery.cashmemo.data.local.CashmemoDatabase
import com.vishalnursery.cashmemo.domain.model.GeneralNote
import com.vishalnursery.cashmemo.domain.model.TodoTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NotesRepository(database: CashmemoDatabase) {
    private val noteDao = database.noteDao()
    private val todoDao = database.todoDao()

    val notesFlow: Flow<List<GeneralNote>> =
        noteDao.observeAllNotes().map { list -> list.map { it.toDomain() } }

    val todosFlow: Flow<List<TodoTask>> =
        todoDao.observeAllTodos().map { list -> list.map { it.toDomain() } }

    suspend fun saveNote(note: GeneralNote): Result<GeneralNote> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val clean = note.copy(
                title = note.title.trim(),
                body = note.body.trimEnd(),
                customerName = note.customerName.trim(),
                updatedAt = now
            )
            noteDao.upsertNote(clean.toEntity())
            clean
        }
    }

    suspend fun togglePinNote(note: GeneralNote): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            noteDao.upsertNote(
                note.copy(
                    isPinned = !note.isPinned,
                    updatedAt = System.currentTimeMillis()
                ).toEntity()
            )
        }
    }

    suspend fun deleteNote(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { noteDao.deleteNoteById(id) }
    }

    suspend fun saveTodo(todo: TodoTask): Result<TodoTask> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val clean = todo.copy(
                title = todo.title.trim(),
                dueDate = todo.dueDate.trim(),
                customerName = todo.customerName.trim(),
                updatedAt = now
            )
            todoDao.upsertTodo(clean.toEntity())
            clean
        }
    }

    suspend fun toggleTodoDone(todo: TodoTask): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val now = System.currentTimeMillis()
            val nextDone = !todo.isDone
            todoDao.upsertTodo(
                todo.copy(
                    isDone = nextDone,
                    doneAt = if (nextDone) now else 0L,
                    updatedAt = now
                ).toEntity()
            )
        }
    }

    suspend fun deleteTodo(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { todoDao.deleteTodoById(id) }
    }
}
