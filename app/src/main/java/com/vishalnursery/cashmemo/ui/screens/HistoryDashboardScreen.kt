package com.vishalnursery.cashmemo.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.ui.components.KpiSummaryPill
import com.vishalnursery.cashmemo.ui.theme.NurseryColors

@Composable
fun HistoryDashboardScreen(
    memos: List<CashMemo>,
    stats: DashboardStats,
    settings: BusinessSettings,
    searchQuery: String,
    activeFilter: MemoFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (MemoFilter) -> Unit,
    onEditMemo: (CashMemo) -> Unit,
    onDuplicateMemo: (CashMemo) -> Unit,
    onPrintMemo: (CashMemo) -> Unit,
    onShareMemo: (CashMemo) -> Unit,
    onDeleteMemo: (String) -> Unit,
    onExportCsv: () -> Unit,
    onExportJsonBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

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

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = {
                Text(
                    text = "Search memo #, customer, mobile...",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(memos, key = { it.id }) { memo ->
                    MemoHistoryCard(
                        memo = memo,
                        currencySymbol = settings.currencySymbol,
                        onEdit = { onEditMemo(memo) },
                        onDuplicate = { onDuplicateMemo(memo) },
                        onPrint = { onPrintMemo(memo) },
                        onShare = { onShareMemo(memo) },
                        onDelete = { pendingDeleteId = memo.id }
                    )
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
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Share Memo")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Memo", tint = NurseryColors.DueRed)
                }
            }
        }
    }
}
