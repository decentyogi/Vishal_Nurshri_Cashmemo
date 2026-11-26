package com.vishalnursery.cashmemo.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.DashboardStats
import com.vishalnursery.cashmemo.domain.model.MemoFilter
import com.vishalnursery.cashmemo.domain.model.OrderFilter
import com.vishalnursery.cashmemo.domain.model.OrderStats
import com.vishalnursery.cashmemo.domain.model.OrderStatus
import com.vishalnursery.cashmemo.domain.model.round2
import com.vishalnursery.cashmemo.domain.util.DateUtils
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.ui.components.KpiSummaryPill
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryDashboardScreen(
    memos: List<CashMemo>,
    orders: List<CashMemo>,
    stats: DashboardStats,
    orderStats: OrderStats,
    settings: BusinessSettings,
    historyTab: Int,
    searchQuery: String,
    selectedDate: String,
    activeFilter: MemoFilter,
    activeOrderFilter: OrderFilter,
    onHistoryTabChange: (Int) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectedDateChange: (String) -> Unit,
    onFilterChange: (MemoFilter) -> Unit,
    onOrderFilterChange: (OrderFilter) -> Unit,
    onDeliverOrder: (CashMemo) -> Unit,
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
    var pendingDeleteMemo by remember { mutableStateOf<CashMemo?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedDates by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = historyTab.coerceIn(0, 1)) { 2 }

    LaunchedEffect(historyTab) {
        val target = historyTab.coerceIn(0, 1)
        if (pagerState.currentPage != target && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != historyTab) {
            onHistoryTabChange(pagerState.settledPage)
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.parseDdMmYyyyUtcMillis(selectedDate) ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        onSelectedDateChange(DateUtils.formatUtcMillisDdMmYyyy(millis))
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

    pendingDeleteMemo?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteMemo = null },
            title = { Text(if (target.isOrder) "Delete Order Memo?" else "Delete Cash Memo?") },
            text = {
                Text(
                    if (target.isOrder) {
                        "This removes Order #${target.memoNumber} from your offline database."
                    } else {
                        "This removes Cash Memo #${target.memoNumber} from your offline database."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMemo(target.id)
                    pendingDeleteMemo = null
                }) {
                    Text("Delete", color = NurseryColors.DueRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMemo = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        // --- STICKY HEADER (<= 90dp total: 42dp TabRow + 44dp compact Search bar) ---
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
                                text = "Sales (${stats.totalMemos})",
                                fontSize = 13.sp,
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
                                text = "Orders (${orderStats.totalOrders})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }

                // Compact Search Bar (40dp inside 4dp vertical padding = 48dp; total sticky <= 90dp)
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
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (pagerState.currentPage == 0) "Search bills, customer, item..." else "Search orders, customer, item...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NurseryColors.MutedInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(color = NurseryColors.InkDark),
                                cursorBrush = SolidColor(NurseryColors.Maroon),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotBlank()) {
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
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Filter by Calendar Date",
                                modifier = Modifier.size(18.dp),
                                tint = if (selectedDate.isNotBlank()) NurseryColors.Maroon else NurseryColors.MutedInk
                            )
                        }
                    }
                }
                HorizontalDivider(color = NurseryColors.OutlineSoft)
            }
        }

        // --- SCROLLABLE PAGER CONTENT ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            if (page == 0) {
                val groupedEntries = remember(memos) {
                    memos.groupBy { it.date.ifBlank { "Undated" } }
                        .entries
                        .sortedWith(
                            compareByDescending<Map.Entry<String, List<CashMemo>>> {
                                DateUtils.parseDdMmYyyyEpochDay(it.key) ?: Long.MIN_VALUE
                            }.thenByDescending { entry ->
                                entry.value.maxOfOrNull { it.createdAt } ?: 0L
                            }
                        )
                        .toList()
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "sales_kpis") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                    }

                    if (selectedDate.isNotBlank()) {
                        item(key = "sales_date_chip") {
                            SelectedDateChipRow(
                                selectedDate = selectedDate,
                                onClear = { onSelectedDateChange("") },
                                onChange = { showDatePicker = true }
                            )
                        }
                    }

                    item(key = "sales_filters") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemoFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = activeFilter == filter,
                                    onClick = { onFilterChange(filter) },
                                    modifier = Modifier.height(32.dp),
                                    label = {
                                        Text(
                                            text = filter.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                    }

                    item(key = "sales_exports") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onExportCsv,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Export CSV",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            OutlinedButton(
                                onClick = onExportJsonBackup,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Backup JSON",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (memos.isEmpty()) {
                        item(key = "sales_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No saved cash memos match your filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(groupedEntries, key = { it.key }) { (dateKey, dayMemos) ->
                            val defaultExpanded = selectedDate.isNotBlank() || searchQuery.isNotBlank()
                            val isExpanded = expandedDates[dateKey] ?: defaultExpanded
                            val daySales = round2(dayMemos.sumOf { it.grandTotal })
                            val dayCollected = round2(dayMemos.sumOf { it.effectivePaid })
                            val dayDue = round2(dayMemos.sumOf { it.effectiveDue })

                            Card(
                                colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                                border = BorderStroke(1.dp, NurseryColors.Maroon.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                expandedDates = expandedDates.toMutableMap().apply {
                                                    put(dateKey, !isExpanded)
                                                }
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
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
                                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                                    onDelete = { pendingDeleteMemo = memo }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "orders_kpis") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KpiSummaryPill(
                                label = "Pending Orders",
                                value = orderStats.pendingCount.toString(),
                                accentColor = NurseryColors.Maroon,
                                backgroundColor = NurseryColors.MaroonLight,
                                modifier = Modifier.weight(1f)
                            )
                            KpiSummaryPill(
                                label = "Advance",
                                value = IndianCurrencyFormatter.formatInr(orderStats.advanceCollected, settings.currencySymbol, false),
                                accentColor = NurseryColors.BotanicalGreen,
                                backgroundColor = NurseryColors.BotanicalGreenLight,
                                modifier = Modifier.weight(1f)
                            )
                            KpiSummaryPill(
                                label = "Balance Due",
                                value = IndianCurrencyFormatter.formatInr(orderStats.pendingBalance, settings.currencySymbol, false),
                                accentColor = NurseryColors.DueRed,
                                backgroundColor = NurseryColors.DueRedContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (selectedDate.isNotBlank()) {
                        item(key = "orders_date_chip") {
                            SelectedDateChipRow(
                                selectedDate = selectedDate,
                                onClear = { onSelectedDateChange("") },
                                onChange = { showDatePicker = true }
                            )
                        }
                    }

                    item(key = "orders_filters") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OrderFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = activeOrderFilter == filter,
                                    onClick = { onOrderFilterChange(filter) },
                                    modifier = Modifier.height(32.dp),
                                    label = {
                                        Text(
                                            text = filter.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (orders.isEmpty()) {
                        item(key = "orders_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No saved order memos match your filter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(orders, key = { it.id }) { order ->
                            OrderHistoryCard(
                                order = order,
                                currencySymbol = settings.currencySymbol,
                                onDeliver = { onDeliverOrder(order) },
                                onEdit = { onEditMemo(order) },
                                onDuplicate = { onDuplicateMemo(order) },
                                onPrint = { onPrintMemo(order) },
                                onShareJpeg = { onShareMemoJpeg(order) },
                                onSharePdf = { onShareMemoPdf(order) },
                                onDelete = { pendingDeleteMemo = order }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDateChipRow(
    selectedDate: String,
    onClear: () -> Unit,
    onChange: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FilterChip(
            selected = true,
            onClick = onClear,
            modifier = Modifier.height(30.dp),
            label = { Text("Date: $selectedDate", style = MaterialTheme.typography.labelSmall) },
            trailingIcon = {
                Icon(Icons.Default.Close, contentDescription = "Clear Date Filter", modifier = Modifier.size(14.dp))
            }
        )
        TextButton(
            onClick = onChange,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(30.dp)
        ) {
            Text("Change Date", style = MaterialTheme.typography.labelSmall)
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
        border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${memo.memoNumber}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = NurseryColors.Maroon,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = memo.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (memo.hasEffectivePendingDue) NurseryColors.DueRedContainer else NurseryColors.BotanicalGreenLight
                    ) {
                        Text(
                            text = if (memo.hasEffectivePendingDue) {
                                "Due ${IndianCurrencyFormatter.formatInr(memo.effectiveDue, currencySymbol, false)}"
                            } else "Paid",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (memo.hasEffectivePendingDue) NurseryColors.DueRed else NurseryColors.BotanicalGreen,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = IndianCurrencyFormatter.formatInr(memo.totalPayable, currencySymbol, false),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val partyLine = buildString {
                    append(memo.customerName.ifBlank { "Walk-in / Cash Sale" })
                    if (memo.customerMobile.isNotBlank()) append(" • ${memo.customerMobile}")
                }
                Text(
                    text = partyLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (memo.isSynced) "Synced" else "Offline",
                    fontSize = 10.sp,
                    color = if (memo.isSynced) NurseryColors.BotanicalGreen else Color.Gray,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Text(
                text = memo.items.joinToString(", ") { "${it.description} (${it.quantity})" },
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Memo", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDuplicate, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Memo", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onPrint, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Print, contentDescription = "Print Memo", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onShareJpeg, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Image, contentDescription = "Send As JPEG", tint = NurseryColors.BotanicalGreen, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onSharePdf, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Send As PDF", tint = NurseryColors.Maroon, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share Text Summary", modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Memo", tint = NurseryColors.DueRed, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryCard(
    order: CashMemo,
    currencySymbol: String,
    onDeliver: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onPrint: () -> Unit,
    onShareJpeg: () -> Unit,
    onSharePdf: () -> Unit,
    onDelete: () -> Unit
) {
    val overdue = order.orderStatus == OrderStatus.PENDING && DateUtils.isOverdue(order.dueDate)
    val statusBg = when (order.orderStatus) {
        OrderStatus.PENDING -> NurseryColors.WarningSoft
        OrderStatus.DELIVERED -> NurseryColors.BotanicalGreenLight
        OrderStatus.CANCELLED -> NurseryColors.CreamBackground
    }
    val statusColor = when (order.orderStatus) {
        OrderStatus.PENDING -> NurseryColors.WarningHeader
        OrderStatus.DELIVERED -> NurseryColors.BotanicalGreen
        OrderStatus.CANCELLED -> NurseryColors.MutedInk
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
        border = BorderStroke(1.dp, if (overdue) NurseryColors.DueRed.copy(alpha = 0.45f) else NurseryColors.OutlineSoft),
        modifier = Modifier.fillMaxWidth()
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
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${order.memoNumber}",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = NurseryColors.Maroon,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = order.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = order.orderStatus.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = IndianCurrencyFormatter.formatInr(order.grandTotal, currencySymbol, false),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val partyLine = buildString {
                    append(order.customerName.ifBlank { "Walk-in Customer" })
                    if (order.customerMobile.isNotBlank()) append(" • ${order.customerMobile}")
                }
                Text(
                    text = partyLine,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (order.dueDate.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (overdue) "Overdue: ${order.dueDate}" else "Delivery: ${order.dueDate}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (overdue) FontWeight.Bold else FontWeight.Medium,
                        color = if (overdue) NurseryColors.DueRed else NurseryColors.MutedInk,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.items.joinToString(", ") { "${it.description} (${it.quantity})" },
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Adv ${IndianCurrencyFormatter.formatInr(order.paidAmount, currencySymbol, false)} • Bal ${IndianCurrencyFormatter.formatInr(order.balanceDue, currencySymbol, false)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (order.balanceDue > 0.009) NurseryColors.DueRed else NurseryColors.BotanicalGreen,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (order.orderStatus == OrderStatus.PENDING) {
                    Button(
                        onClick = onDeliver,
                        colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = "Deliver →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Text(
                        text = if (order.orderStatus == OrderStatus.DELIVERED && order.linkedMemoId.isNotBlank()) "Billed" else order.orderStatus.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Order", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Order", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onPrint, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Print, contentDescription = "Print Order", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onShareJpeg, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Image, contentDescription = "Send As JPEG", tint = NurseryColors.BotanicalGreen, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onSharePdf, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Send As PDF", tint = NurseryColors.Maroon, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Order", tint = NurseryColors.DueRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
