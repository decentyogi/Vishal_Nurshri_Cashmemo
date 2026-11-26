package com.vishalnursery.cashmemo.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.GeneralNote
import com.vishalnursery.cashmemo.domain.model.TodoFilter
import com.vishalnursery.cashmemo.domain.model.TodoPriority
import com.vishalnursery.cashmemo.domain.model.TodoTask
import com.vishalnursery.cashmemo.domain.util.DateUtils
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import com.vishalnursery.cashmemo.ui.viewmodel.NotesUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(
    notesState: NotesUiState,
    customers: List<CustomerProfile>,
    pendingOrders: List<CashMemo>,
    onSearchQueryChange: (String) -> Unit,
    onTodoFilterChange: (TodoFilter) -> Unit,
    onSaveNote: (GeneralNote) -> Unit,
    onTogglePinNote: (GeneralNote) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSaveTodo: (TodoTask) -> Unit,
    onToggleTodoDone: (TodoTask) -> Unit,
    onDeleteTodo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val coroutineScope = rememberCoroutineScope()

    var editingNote by remember { mutableStateOf<GeneralNote?>(null) }
    var isNoteEditorOpen by remember { mutableStateOf(false) }

    var editingTodo by remember { mutableStateOf<TodoTask?>(null) }
    var isTodoEditorOpen by remember { mutableStateOf(false) }

    var pendingDeleteNote by remember { mutableStateOf<GeneralNote?>(null) }
    var pendingDeleteTodo by remember { mutableStateOf<TodoTask?>(null) }

    if (isNoteEditorOpen) {
        NoteEditorDialog(
            initial = editingNote,
            customers = customers,
            onDismiss = {
                isNoteEditorOpen = false
                editingNote = null
            },
            onSave = { saved ->
                onSaveNote(saved)
                isNoteEditorOpen = false
                editingNote = null
            }
        )
    }

    if (isTodoEditorOpen) {
        TodoEditorDialog(
            initial = editingTodo,
            customers = customers,
            pendingOrders = pendingOrders,
            onDismiss = {
                isTodoEditorOpen = false
                editingTodo = null
            },
            onSave = { saved ->
                onSaveTodo(saved)
                isTodoEditorOpen = false
                editingTodo = null
            }
        )
    }

    pendingDeleteNote?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDeleteNote = null },
            title = { Text("Delete Note?") },
            text = { Text("Remove \"${note.title.ifBlank { "Untitled Note" }}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteNote(note.id)
                    pendingDeleteNote = null
                }) {
                    Text("Delete", color = NurseryColors.DueRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteNote = null }) { Text("Cancel") }
            }
        )
    }

    pendingDeleteTodo?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTodo = null },
            title = { Text("Delete Task?") },
            text = { Text("Remove \"${task.title}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTodo(task.id)
                    pendingDeleteTodo = null
                }) {
                    Text("Delete", color = NurseryColors.DueRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTodo = null }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Sticky header <= 90dp matching History
        Surface(
            color = NurseryColors.PaperSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = NurseryColors.PaperSurface,
                    contentColor = NurseryColors.Maroon,
                    modifier = Modifier.height(42.dp),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = NurseryColors.Maroon
                        )
                    }
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                        modifier = Modifier.height(42.dp),
                        text = {
                            Text(
                                text = "General Notes (${notesState.totalNotesCount})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                        modifier = Modifier.height(42.dp),
                        text = {
                            Text(
                                text = "To-Do (${notesState.pendingTodosCount})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = NurseryColors.CreamBackground,
                    border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .height(40.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = NurseryColors.MutedInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (notesState.searchQuery.isEmpty()) {
                                Text(
                                    text = if (pagerState.currentPage == 0) "Search notes or customer..." else "Search tasks or date...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NurseryColors.MutedInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = notesState.searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = NurseryColors.InkDark),
                                cursorBrush = SolidColor(NurseryColors.Maroon),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (notesState.searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear Search",
                                    modifier = Modifier.size(16.dp),
                                    tint = NurseryColors.MutedInk
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = NurseryColors.OutlineSoft)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    if (notesState.notes.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No general notes yet. Tap + to create one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = NurseryColors.MutedInk
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notesState.notes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = {
                                        editingNote = note
                                        isNoteEditorOpen = true
                                    },
                                    onTogglePin = { onTogglePinNote(note) },
                                    onDelete = { pendingDeleteNote = note }
                                )
                            }
                        }
                    }
                } else {
                    val todayDay = DateUtils.todayEpochDay()
                    val overdueTasks = remember(notesState.todos, todayDay) {
                        notesState.todos.filter { !it.isDone && DateUtils.isOverdue(it.dueDate, todayDay) }
                    }
                    val todayTasks = remember(notesState.todos, todayDay) {
                        notesState.todos.filter { !it.isDone && DateUtils.isToday(it.dueDate, todayDay) }
                    }
                    val tomorrowTasks = remember(notesState.todos, todayDay) {
                        notesState.todos.filter { !it.isDone && DateUtils.isTomorrow(it.dueDate, todayDay) }
                    }
                    val upcomingTasks = remember(notesState.todos, todayDay) {
                        notesState.todos.filter {
                            !it.isDone &&
                                it.dueDate.isNotBlank() &&
                                !DateUtils.isOverdue(it.dueDate, todayDay) &&
                                !DateUtils.isToday(it.dueDate, todayDay) &&
                                !DateUtils.isTomorrow(it.dueDate, todayDay)
                        }
                    }
                    val noDueTasks = remember(notesState.todos) {
                        notesState.todos.filter { !it.isDone && it.dueDate.isBlank() }
                    }
                    val doneTasks = remember(notesState.todos) {
                        notesState.todos.filter { it.isDone }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "todo_filters") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TodoFilter.entries.forEach { filter ->
                                    val label = if (filter == TodoFilter.OVERDUE && notesState.overdueTodosCount > 0) {
                                        "${filter.label} (${notesState.overdueTodosCount})"
                                    } else {
                                        filter.label
                                    }
                                    FilterChip(
                                        selected = notesState.todoFilter == filter,
                                        onClick = { onTodoFilterChange(filter) },
                                        modifier = Modifier.height(32.dp),
                                        label = {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        if (notesState.todos.isEmpty()) {
                            item(key = "todo_empty") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No tasks match your filter. Tap + to add one.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = NurseryColors.MutedInk
                                    )
                                }
                            }
                        } else {
                            fun addSection(keyPrefix: String, title: String, color: Color, tasks: List<TodoTask>) {
                                if (tasks.isEmpty()) return
                                item(key = "${keyPrefix}_hdr") {
                                    Text(
                                        text = "$title (${tasks.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = color,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                                items(tasks, key = { it.id }) { task ->
                                    TodoCard(
                                        task = task,
                                        isOverdue = !task.isDone && DateUtils.isOverdue(task.dueDate, todayDay),
                                        onToggleDone = { onToggleTodoDone(task) },
                                        onClick = {
                                            editingTodo = task
                                            isTodoEditorOpen = true
                                        },
                                        onDelete = { pendingDeleteTodo = task }
                                    )
                                }
                            }

                            addSection("overdue", "Overdue", NurseryColors.DueRed, overdueTasks)
                            addSection("today", "Today", NurseryColors.BotanicalGreen, todayTasks)
                            addSection("tomorrow", "Tomorrow", NurseryColors.WarningHeader, tomorrowTasks)
                            addSection("upcoming", "Upcoming", NurseryColors.Maroon, upcomingTasks)
                            addSection("nodue", "No Due Date", NurseryColors.MutedInk, noDueTasks)
                            addSection("done", "Completed", NurseryColors.MutedInk, doneTasks)
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    if (pagerState.currentPage == 0) {
                        editingNote = null
                        isNoteEditorOpen = true
                    } else {
                        editingTodo = null
                        isTodoEditorOpen = true
                    }
                },
                containerColor = NurseryColors.Maroon,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (pagerState.currentPage == 0) "New Note" else "New Task"
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: GeneralNote,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned) NurseryColors.MaroonLight.copy(alpha = 0.25f) else NurseryColors.PaperSurface
        ),
        border = BorderStroke(1.dp, if (note.isPinned) NurseryColors.Maroon.copy(alpha = 0.45f) else NurseryColors.OutlineSoft),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = NurseryColors.InkDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                            tint = if (note.isPinned) NurseryColors.Maroon else NurseryColors.MutedInk,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(if (note.isPinned) 0f else 45f)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = NurseryColors.DueRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (note.customerName.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NurseryColors.BotanicalGreenLight
                ) {
                    Text(
                        text = note.customerName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = NurseryColors.BotanicalGreen,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (note.body.isNotBlank()) {
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TodoCard(
    task: TodoTask,
    isOverdue: Boolean,
    onToggleDone: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityBg = when (task.priority) {
        TodoPriority.HIGH -> NurseryColors.DueRedContainer
        TodoPriority.MEDIUM -> NurseryColors.WarningSoft
        TodoPriority.LOW -> NurseryColors.BotanicalGreenLight
    }
    val priorityColor = when (task.priority) {
        TodoPriority.HIGH -> NurseryColors.DueRed
        TodoPriority.MEDIUM -> NurseryColors.WarningHeader
        TodoPriority.LOW -> NurseryColors.BotanicalGreen
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
        border = BorderStroke(1.dp, if (isOverdue) NurseryColors.DueRed.copy(alpha = 0.45f) else NurseryColors.OutlineSoft),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onToggleDone() },
                colors = CheckboxDefaults.colors(checkedColor = NurseryColors.BotanicalGreen)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (task.isDone) NurseryColors.MutedInk else NurseryColors.InkDark,
                    textDecoration = if (task.isDone) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityBg
                    ) {
                        Text(
                            text = task.priority.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                    if (task.dueDate.isNotBlank()) {
                        Text(
                            text = if (isOverdue) "Overdue: ${task.dueDate}" else task.dueDate,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                            color = if (isOverdue) NurseryColors.DueRed else NurseryColors.MutedInk,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    if (task.customerName.isNotBlank()) {
                        Text(
                            text = "• ${task.customerName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = NurseryColors.BotanicalGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = NurseryColors.DueRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun NoteEditorDialog(
    initial: GeneralNote?,
    customers: List<CustomerProfile>,
    onDismiss: () -> Unit,
    onSave: (GeneralNote) -> Unit
) {
    val origTitle = initial?.title ?: ""
    val origBody = initial?.body ?: ""
    val origCustomerId = initial?.customerId ?: ""
    val origCustomerName = initial?.customerName ?: ""
    val origPinned = initial?.isPinned ?: false

    var title by remember(initial) { mutableStateOf(origTitle) }
    var body by remember(initial) { mutableStateOf(origBody) }
    var customerId by remember(initial) { mutableStateOf(origCustomerId) }
    var customerName by remember(initial) { mutableStateOf(origCustomerName) }
    var isPinned by remember(initial) { mutableStateOf(origPinned) }
    var showCustomerSuggestions by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDirty = title != origTitle || body != origBody || customerName != origCustomerName || isPinned != origPinned
    val canSave = title.isNotBlank() || body.isNotBlank()

    fun requestClose() {
        if (isDirty) showDiscardDialog = true else onDismiss()
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved edits in this note.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    onDismiss()
                }) {
                    Text("Discard", color = NurseryColors.DueRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Keep Editing") }
            }
        )
    }

    val customerMatches = remember(customerName, customers) {
        val q = customerName.trim().lowercase()
        if (q.isBlank()) emptyList()
        else customers.filter {
            it.name.lowercase().contains(q) || it.phoneNumber.lowercase().contains(q)
        }.take(5)
    }

    Dialog(
        onDismissRequest = ::requestClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler { requestClose() }
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            color = NurseryColors.CreamBackground
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Surface(
                    color = NurseryColors.PaperSurface,
                    border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = ::requestClose) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                            Text(
                                text = if (initial == null) "New Note" else "Edit Note",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val prefix = "${DateUtils.todayDdMmYyyy()} — "
                                    body = if (body.isBlank()) prefix else "${body.trimEnd()}\n$prefix"
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("+ Date line", fontSize = 11.sp, maxLines = 1, softWrap = false)
                            }
                            IconButton(
                                onClick = { isPinned = !isPinned },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = if (isPinned) "Unpin" else "Pin",
                                    tint = if (isPinned) NurseryColors.Maroon else NurseryColors.MutedInk,
                                    modifier = Modifier.rotate(if (isPinned) 0f else 45f)
                                )
                            }
                            Button(
                                onClick = {
                                    val base = initial ?: GeneralNote()
                                    onSave(
                                        base.copy(
                                            title = title,
                                            body = body,
                                            customerId = customerId,
                                            customerName = customerName,
                                            isPinned = isPinned
                                        )
                                    )
                                },
                                enabled = canSave,
                                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = {
                            customerName = it
                            customerId = ""
                            showCustomerSuggestions = true
                        },
                        label = { Text("Customer (optional)") },
                        singleLine = true,
                        trailingIcon = {
                            if (customerName.isNotBlank()) {
                                IconButton(onClick = {
                                    customerName = ""
                                    customerId = ""
                                    showCustomerSuggestions = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Customer", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (showCustomerSuggestions && customerMatches.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NurseryColors.PaperSurface,
                            border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(4.dp)) {
                                customerMatches.forEach { cust ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                customerId = cust.id
                                                customerName = cust.name
                                                showCustomerSuggestions = false
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cust.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        if (cust.phoneNumber.isNotBlank()) {
                                            Text(cust.phoneNumber, fontSize = 12.sp, color = NurseryColors.MutedInk)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Note details") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TodoEditorDialog(
    initial: TodoTask?,
    customers: List<CustomerProfile>,
    pendingOrders: List<CashMemo>,
    onDismiss: () -> Unit,
    onSave: (TodoTask) -> Unit
) {
    var title by remember(initial) { mutableStateOf(initial?.title ?: "") }
    var dueDate by remember(initial) { mutableStateOf(initial?.dueDate ?: "") }
    var priority by remember(initial) { mutableStateOf(initial?.priority ?: TodoPriority.MEDIUM) }
    var customerId by remember(initial) { mutableStateOf(initial?.customerId ?: "") }
    var customerName by remember(initial) { mutableStateOf(initial?.customerName ?: "") }
    var showCustomerSuggestions by remember(initial) { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.parseDdMmYyyyUtcMillis(dueDate) ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDate = DateUtils.formatUtcMillisDdMmYyyy(millis)
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val customerMatches = remember(customerName, customers) {
        val q = customerName.trim().lowercase()
        if (q.isBlank()) emptyList()
        else customers.filter {
            it.name.lowercase().contains(q) || it.phoneNumber.lowercase().contains(q)
        }.take(4)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Task" else "Edit Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task description *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Priority", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NurseryColors.MutedInk)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TodoPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label, maxLines = 1, softWrap = false, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    color = NurseryColors.PaperSurface,
                    border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Due Date (optional)", style = MaterialTheme.typography.labelSmall, color = NurseryColors.MutedInk)
                            Text(
                                text = dueDate.ifBlank { "Tap to pick date" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (dueDate.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (dueDate.isNotBlank()) NurseryColors.InkDark else NurseryColors.MutedInk
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (dueDate.isNotBlank()) {
                                IconButton(onClick = { dueDate = "" }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear due date", modifier = Modifier.size(16.dp))
                                }
                            }
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick due date", tint = NurseryColors.Maroon, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = {
                        customerName = it
                        customerId = ""
                        showCustomerSuggestions = true
                    },
                    label = { Text("Customer (optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (showCustomerSuggestions && customerMatches.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NurseryColors.PaperSurface,
                        border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            customerMatches.forEach { cust ->
                                Text(
                                    text = cust.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            customerId = cust.id
                                            customerName = cust.name
                                            showCustomerSuggestions = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                if (initial == null && pendingOrders.isNotEmpty()) {
                    Text(
                        text = "Quick Reminder from Pending Order",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NurseryColors.Maroon
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        pendingOrders.take(8).forEach { order ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val cName = order.customerName.trim()
                                    title = if (cName.isNotBlank()) {
                                        "Follow up Order #${order.memoNumber} ($cName)"
                                    } else {
                                        "Follow up Order #${order.memoNumber}"
                                    }
                                    customerName = cName
                                    dueDate = order.dueDate
                                    priority = TodoPriority.HIGH
                                },
                                label = {
                                    Text(
                                        text = "#${order.memoNumber} ${order.customerName.ifBlank { "Order" }}",
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val base = initial ?: TodoTask()
                    onSave(
                        base.copy(
                            title = title.trim(),
                            dueDate = dueDate.trim(),
                            priority = priority,
                            customerId = customerId,
                            customerName = customerName.trim()
                        )
                    )
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon)
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
