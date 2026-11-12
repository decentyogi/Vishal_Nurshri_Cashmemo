package com.vishalnursery.cashmemo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.DashboardStats
import com.vishalnursery.cashmemo.domain.model.MemoFilter
import com.vishalnursery.cashmemo.domain.model.round2
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.ui.components.KpiSummaryPill
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDashboardScreen(
    memos: List<CashMemo>,
    stats: DashboardStats,
    settings: BusinessSettings,
    searchQuery: String,
    selectedDate: String,
    activeFilter: MemoFilter,
    onSearchQueryChange: (String) -> Unit,
    onSelectedDateChange: (String) -> Unit,
    onFilterChange: (MemoFilter) -> Unit,
    onEditMemo: (CashMemo) -> Unit,
    onDuplicateMemo: (CashMemo) -> Unit,
    onPrintMemo: (CashMemo) -> Unit,
    onShareMemo: (CashMemo) -> Unit,
    onShareMemoJpeg: (CashMemo) -> Unit,
    onShareMemoPdf: (CashMemo) -> Unit,
    onDeleteMemo: (String) -> Unit,
    onExportCsv: () -> Unit,
    onExportJsonBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedDates by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        onSelectedDateChange(sdf.format(Date(millis)))
                    }
                    showDatePicker = false
                }) {
                    Text("Filter Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    pendingDeleteId?.let { targetId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete Cash Memo?") },
            text = { Text("This removes the memo from your offline database.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMemo(targetId)
                    pendingDeleteId = null
                }) {
                    Text("Delete", color = NurseryColors.DueRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // KPI Dashboard Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiSummaryPill(
                label = "Total Sales",
                value = IndianCurrencyFormatter.formatInr(stats.totalSales, settings.currencySymbol, false),
                accentColor = NurseryColors.Maroon,
                backgroundColor = NurseryColors.MaroonLight,
                modifier = Modifier.weight(1f)
            )
            KpiSummaryPill(
                label = "Collected",
                value = IndianCurrencyFormatter.formatInr(stats.totalCollected, settings.currencySymbol, false),
                accentColor = NurseryColors.BotanicalGreen,
                backgroundColor = NurseryColors.BotanicalGreenLight,
                modifier = Modifier.weight(1f)
            )
            KpiSummaryPill(
                label = "Carry Forward",
                value = IndianCurrencyFormatter.formatInr(stats.totalPendingDues, settings.currencySymbol, false),
                accentColor = NurseryColors.DueRed,
                backgroundColor = NurseryColors.DueRedContainer,
                modifier = Modifier.weight(1f)
            )
        }

        // Search Bar + Calendar Date Picker
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Search")
                        }
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Filter by Calendar Date",
                            tint = if (selectedDate.isNotBlank()) NurseryColors.Maroon else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            placeholder = {
                Text(
                    text = "Search memo #, customer, mobile, date...",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (selectedDate.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = true,
                    onClick = { onSelectedDateChange("") },
                    label = { Text("Date: $selectedDate") },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "Clear Date Filter", modifier = Modifier.size(16.dp))
                    }
                )
                TextButton(onClick = { showDatePicker = true }) {
                    Text("Change Date")
                }
            }
        }

        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemoFilter.entries.forEach { filter ->
                FilterChip(
                    selected = activeFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        }

        // Export Actions Row (equal width, never squished)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onExportCsv,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Export CSV",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            OutlinedButton(
                onClick = onExportJsonBackup,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Backup JSON",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (memos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No saved cash memos match your filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val groupedEntries = remember(memos) {
                memos.groupBy { it.date.ifBlank { "Undated" } }
                    .entries
                    .sortedByDescending { entry -> entry.value.maxOfOrNull { it.createdAt } ?: 0L }
                    .toList()
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(groupedEntries, key = { it.key }) { (dateKey, dayMemos) ->
                    val defaultExpanded = selectedDate.isNotBlank() ||
                        searchQuery.isNotBlank() ||
                        groupedEntries.firstOrNull()?.key == dateKey
                    val isExpanded = expandedDates[dateKey] ?: defaultExpanded
                    val daySales = round2(dayMemos.sumOf { it.totalPayable })
                    val dayCollected = round2(dayMemos.sumOf { it.paidAmount })
                    val dayDue = round2(dayMemos.sumOf { it.balanceDue })

                    Card(
                        colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                        border = BorderStroke(1.dp, NurseryColors.Maroon.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedDates = expandedDates.toMutableMap().apply {
                                            put(dateKey, !isExpanded)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Sales on $dateKey",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = NurseryColors.Maroon
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = NurseryColors.MaroonLight
                                        ) {
                                            Text(
                                                text = "${dayMemos.size} ${if (dayMemos.size == 1) "Bill" else "Bills"}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = NurseryColors.Maroon,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Sales: ${IndianCurrencyFormatter.formatInr(daySales, settings.currencySymbol, false)}  •  " +
                                            "Paid: ${IndianCurrencyFormatter.formatInr(dayCollected, settings.currencySymbol, false)}  •  " +
                                            "Due: ${IndianCurrencyFormatter.formatInr(dayDue, settings.currencySymbol, false)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse Date" else "Expand Date",
                                    tint = NurseryColors.Maroon
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    dayMemos.forEach { memo ->
                                        MemoHistoryCard(
                                            memo = memo,
                                            currencySymbol = settings.currencySymbol,
                                            onEdit = { onEditMemo(memo) },
                                            onDuplicate = { onDuplicateMemo(memo) },
                                            onPrint = { onPrintMemo(memo) },
                                            onShare = { onShareMemo(memo) },
                                            onShareJpeg = { onShareMemoJpeg(memo) },
                                            onSharePdf = { onShareMemoPdf(memo) },
                                            onDelete = { pendingDeleteId = memo.id }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoHistoryCard(
    memo: CashMemo,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onShareJpeg: () -> Unit,
    onSharePdf: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
        border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${memo.memoNumber}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = NurseryColors.Maroon
                        )
                        Text(
                            text = memo.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (memo.hasPendingDue) NurseryColors.DueRedContainer else NurseryColors.BotanicalGreenLight
                        ) {
                            Text(
                                text = if (memo.hasPendingDue) {
                                    "Due ${IndianCurrencyFormatter.formatInr(memo.balanceDue, currencySymbol)}"
                                } else "Paid",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (memo.hasPendingDue) NurseryColors.DueRed else NurseryColors.BotanicalGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = memo.customerName.ifBlank { "Walk-in / Cash Sale" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (memo.customerMobile.isNotBlank()) {
                        Text(
                            text = "Mob: ${memo.customerMobile}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = IndianCurrencyFormatter.formatInr(memo.totalPayable, currencySymbol),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (memo.isSynced) "Synced" else "Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (memo.isSynced) NurseryColors.BotanicalGreen else Color.Gray
                    )
                }
            }

            Text(
                text = memo.items.joinToString(", ") { "${it.description} (${it.quantity})" },
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Memo")
                }
                IconButton(onClick = onDuplicate) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Memo")
                }
                IconButton(onClick = onPrint) {
                    Icon(Icons.Default.Print, contentDescription = "Print Memo")
                }
                IconButton(onClick = onShareJpeg) {
                    Icon(Icons.Default.Image, contentDescription = "Send As JPEG", tint = NurseryColors.BotanicalGreen)
                }
                IconButton(onClick = onSharePdf) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Send As PDF", tint = NurseryColors.Maroon)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share Text Summary")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Memo", tint = NurseryColors.DueRed)
                }
            }
        }
    }
}
