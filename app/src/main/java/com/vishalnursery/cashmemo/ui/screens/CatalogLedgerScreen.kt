package com.vishalnursery.cashmemo.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vishalnursery.cashmemo.data.repository.PlantRateImportPolicy
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.model.ScanEngine
import com.vishalnursery.cashmemo.domain.util.AiPrompts
import com.vishalnursery.cashmemo.domain.util.AiTextParser
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.domain.util.ParsedRateRow
import com.vishalnursery.cashmemo.ui.components.AiPasteSheet
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PlantRateSortOrder(val label: String) {
    A_TO_Z("Name (A to Z)"),
    RECENTLY_UPDATED("Recently Updated"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low")
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CatalogLedgerScreen(
    customers: List<CustomerProfile>,
    memos: List<CashMemo>,
    plantRates: List<PlantRate>,
    settings: BusinessSettings,
    onSaveCustomer: (CustomerProfile) -> Unit,
    onSettleCustomerDue: (String, Double, String) -> Unit,
    onDeleteCustomer: (String) -> Unit,
    onCreateBillForCustomer: (CustomerProfile) -> Unit,
    onEditMemo: (CashMemo) -> Unit,
    onPrintMemo: (CashMemo) -> Unit,
    onShareMemoJpeg: (CashMemo) -> Unit,
    onShareMemoPdf: (CashMemo) -> Unit,
    onSavePlantRate: (PlantRate) -> Unit,
    onDeletePlantRate: (String) -> Unit,
    onExportPlantRatesJson: () -> Unit = {},
    onExportPlantRatesCsv: () -> Unit = {},
    onImportPlantRates: (List<PlantRate>, PlantRateImportPolicy) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 2 }

    // Dialog & Flow States
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerProfile?>(null) }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var settleTarget by remember { mutableStateOf<CustomerProfile?>(null) }

    var plantRateDialogTarget by remember { mutableStateOf<PlantRate?>(null) }
    var showPlantRateDialog by remember { mutableStateOf(false) }
    var deletingPlantRate by remember { mutableStateOf<PlantRate?>(null) }

    // Rate Import & Scan Review Dialog
    var pendingImportRates by remember { mutableStateOf<List<ParsedRateRow>?>(null) }
    var showImportMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    // Scan Rate List flow
    var isScanningRateList by remember { mutableStateOf(false) }
    var scanRateListImageUri by remember { mutableStateOf<String?>(null) }
    var isRateAiPasteOpen by remember { mutableStateOf(false) }
    var rateAiPasteText by remember { mutableStateOf("") }

    // File Import launcher (JSON / CSV)
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val parsed = parsePlantRatesFromUri(context, uri)
                if (parsed.isNotEmpty()) {
                    pendingImportRates = parsed
                }
            }
        }
    }

    // Rate list photo launcher
    val ratePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scanRateListImageUri = uri.toString()
            if (settings.scanEngine == ScanEngine.OTHER_AI) {
                isRateAiPasteOpen = true
            } else {
                // If Gemini, scan directly
                isScanningRateList = true
                coroutineScope.launch(Dispatchers.IO) {
                    val result = runCatching {
                        val service = com.vishalnursery.cashmemo.data.ocr.GeminiVisionService(context)
                        service.analyzeRateList(
                            uri = uri,
                            apiKey = settings.geminiApiKey,
                            preferredModel = settings.geminiModel,
                            translationMode = settings.translationMode
                        ).getOrThrow()
                    }
                    withContext(Dispatchers.Main) {
                        isScanningRateList = false
                        result.onSuccess { rows ->
                            pendingImportRates = rows
                        }.onFailure {
                            // On failure, open AI paste sheet as fallback
                            isRateAiPasteOpen = true
                        }
                    }
                }
            }
        }
    }

    // Modal: Customer Dialogs
    if (showAddCustomerDialog) {
        CustomerFormDialog(
            title = "Add Customer",
            initial = null,
            onDismiss = { showAddCustomerDialog = false },
            onSave = {
                onSaveCustomer(it)
                showAddCustomerDialog = false
            }
        )
    }

    editingCustomer?.let { target ->
        CustomerFormDialog(
            title = "Edit Customer",
            initial = target,
            onDismiss = { editingCustomer = null },
            onSave = { updated ->
                onSaveCustomer(updated)
                editingCustomer = null
            }
        )
    }

    settleTarget?.let { customer ->
        SettleDueDialog(
            customer = customer,
            currencySymbol = settings.currencySymbol,
            onDismiss = { settleTarget = null },
            onConfirm = { amount, note ->
                onSettleCustomerDue(customer.id, amount, note)
                settleTarget = null
            }
        )
    }

    // Modal: Plant Rate Dialog (Add / Edit)
    if (showPlantRateDialog) {
        PlantRateDialog(
            initial = plantRateDialogTarget,
            allRates = plantRates,
            currencySymbol = settings.currencySymbol,
            onDismiss = {
                showPlantRateDialog = false
                plantRateDialogTarget = null
            },
            onSave = { savedRate ->
                onSavePlantRate(savedRate)
                showPlantRateDialog = false
                plantRateDialogTarget = null
            }
        )
    }

    // Modal: Delete confirmation with Undo
    deletingPlantRate?.let { rate ->
        AlertDialog(
            onDismissRequest = { deletingPlantRate = null },
            title = { Text("Delete Plant Rate") },
            text = { Text("Are you sure you want to remove '${rate.name}' from your saved plant rates?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlantRate(rate.id)
                        deletingPlantRate = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.DueRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingPlantRate = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal: Rate Import / Scan Review Dialog
    pendingImportRates?.let { rows ->
        RateImportReviewDialog(
            parsedRows = rows,
            existingRates = plantRates,
            currencySymbol = settings.currencySymbol,
            onDismiss = { pendingImportRates = null },
            onApply = { approvedRates, policy ->
                onImportPlantRates(approvedRates, policy)
                pendingImportRates = null
            }
        )
    }

    // Modal: AI Paste Sheet for Rate List
    if (isRateAiPasteOpen) {
        AiPasteSheet(
            title = "Paste Rate List from AI",
            promptText = AiPrompts.getRateListPrompt(settings.translationMode),
            hint = "Paste plant rate lines (e.g. Mango | 100 | 150)...",
            liveStatus = { text ->
                if (text.isBlank()) null
                else {
                    val parsed = AiTextParser.parseRateList(text)
                    if (parsed.isNotEmpty()) "${parsed.size} plants detected" else "No plants detected yet"
                }
            },
            onSubmit = { text ->
                val parsed = AiTextParser.parseRateList(text)
                isRateAiPasteOpen = false
                rateAiPasteText = ""
                pendingImportRates = parsed
            },
            onDismiss = { isRateAiPasteOpen = false },
            imageUri = scanRateListImageUri,
            onPickImage = { ratePhotoLauncher.launch("image/*") },
            currentTranslationMode = settings.translationMode,
            onSelectTranslationMode = {},
            pastedText = rateAiPasteText,
            onPastedTextChanged = { rateAiPasteText = it }
        )
    }

    // Modal: Customer Profile & Bills Dialog
    selectedCustomerId?.let { id ->
        val customer = customers.firstOrNull { it.id == id }
        if (customer != null) {
            val customerBills = remember(customer, memos) {
                val cPhone = customer.phoneNumber.trim()
                val cName = customer.name.trim()
                memos.filter { m ->
                    (cPhone.isNotEmpty() && m.customerMobile.trim() == cPhone) ||
                        (cName.isNotEmpty() && m.customerName.trim().equals(cName, ignoreCase = true))
                }.sortedByDescending { it.createdAt }
            }
            CustomerProfileHistoryDialog(
                customer = customer,
                bills = customerBills,
                currencySymbol = settings.currencySymbol,
                onDismiss = { selectedCustomerId = null },
                onEditCustomer = { editingCustomer = customer },
                onSettleDue = { settleTarget = customer },
                onCreateNewBill = {
                    selectedCustomerId = null
                    onCreateBillForCustomer(customer)
                },
                onEditBill = { bill ->
                    selectedCustomerId = null
                    onEditMemo(bill)
                },
                onPrintBill = onPrintMemo,
                onShareBillJpeg = onShareMemoJpeg,
                onShareBillPdf = onShareMemoPdf
            )
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Tab Row synced with HorizontalPager
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = NurseryColors.PaperSurface,
            contentColor = NurseryColors.Maroon,
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
                text = { Text("Customers (${customers.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("Plant Rates (${plantRates.size})", fontWeight = FontWeight.Bold) }
            )
        }

        // Horizontal Pager: Page 0 = Customers, Page 1 = Plant Rates
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            when (pageIndex) {
                0 -> CustomersTabPage(
                    customers = customers,
                    settings = settings,
                    onAddCustomer = { showAddCustomerDialog = true },
                    onOpenProfile = { selectedCustomerId = it.id },
                    onSettle = { settleTarget = it },
                    onDelete = { onDeleteCustomer(it.id) }
                )
                1 -> PlantRatesTabPage(
                    plantRates = plantRates,
                    settings = settings,
                    isScanning = isScanningRateList,
                    onAddPlant = {
                        plantRateDialogTarget = null
                        showPlantRateDialog = true
                    },
                    onEditPlant = { rate ->
                        plantRateDialogTarget = rate
                        showPlantRateDialog = true
                    },
                    onDeletePlant = { rate ->
                        deletingPlantRate = rate
                    },
                    onImportClick = { showImportMenu = true },
                    onExportClick = { showExportMenu = true }
                )
            }
        }

        // Import & Export Dropdown Menus anchored
        Box {
            DropdownMenu(
                expanded = showImportMenu,
                onDismissRequest = { showImportMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Import from File (JSON / CSV)") },
                    leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    onClick = {
                        showImportMenu = false
                        fileImportLauncher.launch("*/*")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Scan Rate List (Photo / Image)") },
                    leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    onClick = {
                        showImportMenu = false
                        ratePhotoLauncher.launch("image/*")
                    }
                )
                DropdownMenuItem(
                    text = { Text("Paste AI Rate Text") },
                    leadingIcon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                    onClick = {
                        showImportMenu = false
                        scanRateListImageUri = null
                        isRateAiPasteOpen = true
                    }
                )
            }

            DropdownMenu(
                expanded = showExportMenu,
                onDismissRequest = { showExportMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Export Rates as CSV (Excel compatible)") },
                    leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                    onClick = {
                        showExportMenu = false
                        onExportPlantRatesCsv()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export Rates as JSON") },
                    leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                    onClick = {
                        showExportMenu = false
                        onExportPlantRatesJson()
                    }
                )
            }
        }
    }
}

@Composable
private fun CustomersTabPage(
    customers: List<CustomerProfile>,
    settings: BusinessSettings,
    onAddCustomer: () -> Unit,
    onOpenProfile: (CustomerProfile) -> Unit,
    onSettle: (CustomerProfile) -> Unit,
    onDelete: (CustomerProfile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onAddCustomer,
            colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add Customer Profile", fontWeight = FontWeight.Bold)
        }

        if (customers.isEmpty()) {
            EmptyLedgerNotice("No customer records yet. Saved cash memos automatically sync here.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(customers, key = { it.id }) { customer ->
                    CustomerCard(
                        customer = customer,
                        currencySymbol = settings.currencySymbol,
                        onOpenProfile = { onOpenProfile(customer) },
                        onSettle = { onSettle(customer) },
                        onDelete = { onDelete(customer) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlantRatesTabPage(
    plantRates: List<PlantRate>,
    settings: BusinessSettings,
    isScanning: Boolean,
    onAddPlant: () -> Unit,
    onEditPlant: (PlantRate) -> Unit,
    onDeletePlant: (PlantRate) -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf(PlantRateSortOrder.A_TO_Z) }
    var showSortMenu by remember { mutableStateOf(false) }

    val distinctCategories = remember(plantRates) {
        listOf("All") + plantRates.map { it.category.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val filteredRates = remember(plantRates, searchQuery, selectedCategory, sortOrder) {
        val q = searchQuery.trim().lowercase()
        plantRates.filter { rate ->
            val matchQuery = q.isEmpty() || rate.name.lowercase().contains(q) || rate.category.lowercase().contains(q)
            val matchCat = selectedCategory == "All" || rate.category.trim().equals(selectedCategory, ignoreCase = true)
            matchQuery && matchCat
        }.sortedWith { a, b ->
            when (sortOrder) {
                PlantRateSortOrder.A_TO_Z -> a.name.compareTo(b.name, ignoreCase = true)
                PlantRateSortOrder.RECENTLY_UPDATED -> b.updatedAt.compareTo(a.updatedAt)
                PlantRateSortOrder.PRICE_LOW_HIGH -> a.retailPrice.compareTo(b.retailPrice)
                PlantRateSortOrder.PRICE_HIGH_LOW -> b.retailPrice.compareTo(a.retailPrice)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Action Bar: [+ Add Plant], [Import], [Export]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onAddPlant,
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1.3f).height(44.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Plant", maxLines = 1, softWrap = false)
            }

            OutlinedButton(
                onClick = onImportClick,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Import", maxLines = 1, softWrap = false)
            }

            OutlinedButton(
                onClick = onExportClick,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Export", maxLines = 1, softWrap = false)
            }
        }

        if (isScanning) {
            Surface(
                color = NurseryColors.GreenSoft,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Scanning plant rate list with AI...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Search Bar + Sort Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search plants...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(50.dp)
            )

            Box {
                OutlinedButton(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    PlantRateSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.label) },
                            trailingIcon = {
                                if (sortOrder == order) Icon(Icons.Default.Check, contentDescription = null)
                            },
                            onClick = {
                                sortOrder = order
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Category Filter Chips
        if (distinctCategories.size > 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                distinctCategories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NurseryColors.GreenSoft,
                            selectedLabelColor = NurseryColors.GreenHeader
                        )
                    )
                }
            }
        }

        // Count Summary
        Text(
            text = "Showing ${filteredRates.size} of ${plantRates.size} plants",
            style = MaterialTheme.typography.labelSmall,
            color = NurseryColors.MutedInk
        )

        // Plant Rate Cards List
        if (filteredRates.isEmpty()) {
            EmptyLedgerNotice(
                if (plantRates.isEmpty())
                    "No plant rates added yet. Add plants or scan a rate list to use instant autocomplete."
                else
                    "No plants match '$searchQuery'."
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(filteredRates, key = { it.id }) { rate ->
                    PlantRateCard(
                        rate = rate,
                        currencySymbol = settings.currencySymbol,
                        onEdit = { onEditPlant(rate) },
                        onDelete = { onDeletePlant(rate) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlantRateCard(
    rate: PlantRate,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
        border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(rate.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NurseryColors.DarkInk)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (rate.category.isNotBlank()) {
                        Surface(
                            color = NurseryColors.CreamBackground,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
                        ) {
                            Text(
                                text = rate.category,
                                fontSize = 11.sp,
                                color = NurseryColors.MutedInk,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (rate.updatedAt > 0L) {
                        val dateStr = SimpleDateFormat("dd/MM/yy", Locale.US).format(Date(rate.updatedAt))
                        Text(
                            text = "Updated $dateStr",
                            fontSize = 11.sp,
                            color = NurseryColors.MutedInk.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "R: ${IndianCurrencyFormatter.formatInr(rate.retailPrice, currencySymbol)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NurseryColors.Maroon
                    )
                    Text(
                        text = "W: ${IndianCurrencyFormatter.formatInr(rate.wholesalePrice, currencySymbol)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NurseryColors.GreenHeader
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = NurseryColors.DueRed.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlantRateDialog(
    initial: PlantRate?,
    allRates: List<PlantRate>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (PlantRate) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "Plants") }
    var retailText by remember(initial) {
        mutableStateOf(if ((initial?.retailPrice ?: 0.0) > 0.0) initial!!.retailPrice.toString() else "")
    }
    var wholesaleText by remember(initial) {
        mutableStateOf(if ((initial?.wholesalePrice ?: 0.0) > 0.0) initial!!.wholesalePrice.toString() else "")
    }

    val isEditing = initial != null
    val isDuplicateName = remember(name) {
        val clean = name.trim().lowercase()
        clean.isNotEmpty() && allRates.any { it.name.trim().lowercase() == clean && it.id != (initial?.id ?: "") }
    }

    val existingCategories = remember(allRates) {
        allRates.map { it.category.trim() }.filter { it.isNotBlank() && it != "Plants" }.distinct().take(6)
    }

    val isValid = name.isNotBlank() && (retailText.toDoubleOrNull() ?: 0.0) > 0.0 || (wholesaleText.toDoubleOrNull() ?: 0.0) > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Edit Plant Rate" else "Add Plant Rate", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Plant / Variety Name *") },
                    singleLine = true,
                    isError = isDuplicateName,
                    supportingText = {
                        if (isDuplicateName) {
                            Text("Warning: plant name already exists", color = NurseryColors.WarningHeader)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (existingCategories.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        existingCategories.forEach { cat ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (category.trim() == cat) NurseryColors.GreenSoft else NurseryColors.CreamBackground,
                                border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                                modifier = Modifier.clickable { category = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    color = if (category.trim() == cat) NurseryColors.GreenHeader else NurseryColors.DarkInk,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = retailText,
                        onValueChange = { retailText = it },
                        label = { Text("Retail Price ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = wholesaleText,
                        onValueChange = { wholesaleText = it },
                        label = { Text("Wholesale ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val r = retailText.toDoubleOrNull() ?: 0.0
                    val w = wholesaleText.toDoubleOrNull() ?: r
                    val saved = PlantRate(
                        id = initial?.id ?: "pr_${System.currentTimeMillis()}",
                        name = name.trim(),
                        category = category.trim().ifBlank { "Plants" },
                        retailPrice = r,
                        wholesalePrice = w,
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(saved)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon)
            ) {
                Text("Save Plant")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RateImportReviewDialog(
    parsedRows: List<ParsedRateRow>,
    existingRates: List<PlantRate>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onApply: (List<PlantRate>, PlantRateImportPolicy) -> Unit
) {
    var policy by remember { mutableStateOf(PlantRateImportPolicy.ADD_NEW_AND_UPDATE_EXISTING) }

    // Track checked states and inline editable prices
    val checkedMap = remember {
        mutableStateMapOf<Int, Boolean>().apply {
            parsedRows.forEachIndexed { idx, row ->
                put(idx, row.isValid && (row.retailPrice > 0.0 || row.wholesalePrice > 0.0))
            }
        }
    }

    val retailEdits = remember {
        mutableStateMapOf<Int, String>().apply {
            parsedRows.forEachIndexed { idx, row ->
                put(idx, if (row.retailPrice > 0.0) row.retailPrice.toString() else "")
            }
        }
    }

    val wholesaleEdits = remember {
        mutableStateMapOf<Int, String>().apply {
            parsedRows.forEachIndexed { idx, row ->
                put(idx, if (row.wholesalePrice > 0.0) row.wholesalePrice.toString() else "")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = NurseryColors.CreamBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Review Rate List Import", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                Text(
                    text = "${parsedRows.size} plants detected. Review prices, select policy, and confirm.",
                    style = MaterialTheme.typography.bodySmall,
                    color = NurseryColors.MutedInk
                )

                // Import Policy Selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                    border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Import Policy:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        PlantRateImportPolicy.entries.forEach { p ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { policy = p }
                            ) {
                                RadioButton(
                                    selected = policy == p,
                                    onClick = { policy = p },
                                    colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = NurseryColors.Maroon)
                                )
                                Text(p.label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Table of Parsed Rows
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(parsedRows.size) { index ->
                        val row = parsedRows[index]
                        val isChecked = checkedMap[index] ?: false
                        val currentRetail = retailEdits[index] ?: ""
                        val currentWholesale = wholesaleEdits[index] ?: ""
                        val hasPrice = (currentRetail.toDoubleOrNull() ?: 0.0) > 0.0 || (currentWholesale.toDoubleOrNull() ?: 0.0) > 0.0

                        val existing = existingRates.firstOrNull { it.name.trim().equals(row.name.trim(), ignoreCase = true) }
                        val status = when {
                            !hasPrice -> "Missing price"
                            existing == null -> "New"
                            existing.retailPrice != (currentRetail.toDoubleOrNull() ?: 0.0) ||
                                existing.wholesalePrice != (currentWholesale.toDoubleOrNull() ?: 0.0) -> "Will update"
                            else -> "Unchanged"
                        }

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (!hasPrice) NurseryColors.WarningSoft else NurseryColors.PaperSurface
                            ),
                            border = BorderStroke(1.dp, if (!hasPrice) NurseryColors.WarningHeader else NurseryColors.OutlineSoft),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (hasPrice) checkedMap[index] = checked
                                    },
                                    enabled = hasPrice,
                                    colors = CheckboxDefaults.colors(checkedColor = NurseryColors.Maroon)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(row.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            color = when (status) {
                                                "New" -> NurseryColors.GreenSoft
                                                "Will update" -> NurseryColors.MaroonLight
                                                "Missing price" -> NurseryColors.WarningHeader
                                                else -> NurseryColors.CreamBackground
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (status) {
                                                    "New" -> NurseryColors.GreenHeader
                                                    "Will update" -> NurseryColors.Maroon
                                                    "Missing price" -> Color.White
                                                    else -> NurseryColors.MutedInk
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (existing != null && status == "Will update") {
                                            Text(
                                                text = "Current: R ${existing.retailPrice.toInt()} / W ${existing.wholesalePrice.toInt()}",
                                                fontSize = 10.sp,
                                                color = NurseryColors.MutedInk
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.width(140.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    OutlinedTextField(
                                        value = currentRetail,
                                        onValueChange = {
                                            retailEdits[index] = it
                                            if ((it.toDoubleOrNull() ?: 0.0) > 0.0) checkedMap[index] = true
                                        },
                                        placeholder = { Text("R ₹", fontSize = 10.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    )

                                    OutlinedTextField(
                                        value = currentWholesale,
                                        onValueChange = {
                                            wholesaleEdits[index] = it
                                            if ((it.toDoubleOrNull() ?: 0.0) > 0.0) checkedMap[index] = true
                                        },
                                        placeholder = { Text("W ₹", fontSize = 10.sp) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                val checkedCount = checkedMap.values.count { it }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val approved = parsedRows.mapIndexedNotNull { idx, row ->
                                if (checkedMap[idx] == true) {
                                    val r = retailEdits[idx]?.toDoubleOrNull() ?: 0.0
                                    val w = wholesaleEdits[idx]?.toDoubleOrNull() ?: r
                                    PlantRate(
                                        name = row.name.trim(),
                                        category = row.category.ifBlank { "Plants" },
                                        retailPrice = r,
                                        wholesalePrice = w,
                                        updatedAt = System.currentTimeMillis()
                                    )
                                } else null
                            }
                            onApply(approved, policy)
                        },
                        enabled = checkedCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                        modifier = Modifier.weight(1.5f).height(48.dp)
                    ) {
                        Text("Import $checkedCount Plants")
                    }
                }
            }
        }
    }
}

private suspend fun parsePlantRatesFromUri(context: android.content.Context, uri: Uri): List<ParsedRateRow> = withContext(Dispatchers.IO) {
    runCatching {
        val stream = context.contentResolver.openInputStream(uri) ?: return@withContext emptyList()
        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
        val text = reader.readText()
        reader.close()

        // Check if JSON
        val trimmed = text.trim()
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val list = Gson().fromJson<List<Map<String, Any>>>(trimmed, type)
            return@withContext list.mapNotNull { map ->
                val name = map["name"]?.toString()?.trim() ?: return@mapNotNull null
                val cat = map["category"]?.toString()?.trim() ?: "Plants"
                val r = (map["retailPrice"] as? Number)?.toDouble() ?: 0.0
                val w = (map["wholesalePrice"] as? Number)?.toDouble() ?: r
                ParsedRateRow(name = name, category = cat, retailPrice = r, wholesalePrice = w)
            }
        }

        // CSV parsing with auto-delimiter
        AiTextParser.parseRateList(text)
    }.getOrElse { emptyList() }
}

@Composable
private fun EmptyLedgerNotice(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CustomerCard(
    customer: CustomerProfile,
    currencySymbol: String,
    onOpenProfile: () -> Unit,
    onSettle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
        border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenProfile)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(customer.name, fontWeight = FontWeight.Bold, color = NurseryColors.DarkInk)
                if (customer.phoneNumber.isNotBlank()) {
                    Text(customer.phoneNumber, style = MaterialTheme.typography.bodySmall, color = NurseryColors.MutedInk)
                }
                if (customer.address.isNotBlank()) {
                    Text(
                        customer.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (customer.outstandingBalance > 0.009) {
                        "Due: ${IndianCurrencyFormatter.formatInr(customer.outstandingBalance, currencySymbol)} (${customer.lastPromiseNote})"
                    } else "Settled • Total: ${IndianCurrencyFormatter.formatInr(customer.totalPurchased, currencySymbol)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (customer.outstandingBalance > 0.009) NurseryColors.DueRed else NurseryColors.BotanicalGreen,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (customer.outstandingBalance > 0.009) {
                    IconButton(onClick = onSettle) {
                        Icon(
                            Icons.Default.Payments,
                            contentDescription = "Settle Payment",
                            tint = NurseryColors.BotanicalGreen
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Customer", tint = NurseryColors.DueRed)
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Open Profile",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CustomerFormDialog(
    title: String,
    initial: CustomerProfile?,
    onDismiss: () -> Unit,
    onSave: (CustomerProfile) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var phone by remember(initial) { mutableStateOf(initial?.phoneNumber ?: "") }
    var address by remember(initial) { mutableStateOf(initial?.address ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Village") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            CustomerProfile(
                                id = initial?.id ?: "cust_${System.currentTimeMillis()}",
                                name = name.trim(),
                                phoneNumber = phone.trim(),
                                address = address.trim(),
                                outstandingBalance = initial?.outstandingBalance ?: 0.0,
                                totalPurchased = initial?.totalPurchased ?: 0.0,
                                totalPaid = initial?.totalPaid ?: 0.0,
                                lastPromiseNote = initial?.lastPromiseNote ?: "Carry Forward",
                                lastTransactionAt = System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon)
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettleDueDialog(
    customer: CustomerProfile,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf(customer.outstandingBalance.toString()) }
    var noteText by remember { mutableStateOf("Carry Forward") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settle Due — ${customer.name}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Outstanding: ${IndianCurrencyFormatter.formatInr(customer.outstandingBalance, currencySymbol)}",
                    fontWeight = FontWeight.Bold,
                    color = NurseryColors.DueRed
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount Received") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Balance Note") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) onConfirm(amt, noteText)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen)
            ) { Text("Record Payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CustomerProfileHistoryDialog(
    customer: CustomerProfile,
    bills: List<CashMemo>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onEditCustomer: () -> Unit,
    onSettleDue: () -> Unit,
    onCreateNewBill: () -> Unit,
    onEditBill: (CashMemo) -> Unit,
    onPrintBill: (CashMemo) -> Unit,
    onShareBillJpeg: (CashMemo) -> Unit,
    onShareBillPdf: (CashMemo) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = NurseryColors.CreamBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(customer.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (customer.phoneNumber.isNotBlank()) {
                            Text(customer.phoneNumber, style = MaterialTheme.typography.bodySmall, color = NurseryColors.MutedInk)
                        }
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
                }

                // Balance Summary
                Card(
                    colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                    border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Due", style = MaterialTheme.typography.labelSmall, color = NurseryColors.MutedInk)
                            Text(
                                text = IndianCurrencyFormatter.formatInr(customer.outstandingBalance, currencySymbol),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (customer.outstandingBalance > 0.009) NurseryColors.DueRed else NurseryColors.BotanicalGreen
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (customer.outstandingBalance > 0.009) {
                                Button(
                                    onClick = onSettleDue,
                                    colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("Settle Due")
                                }
                            }
                            Button(
                                onClick = onCreateNewBill,
                                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("New Bill")
                            }
                        }
                    }
                }

                // Bill History
                Text("Order / Bill History (${bills.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)

                if (bills.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No prior cash memos found for this customer.", color = NurseryColors.MutedInk)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bills, key = { it.id }) { bill ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                                border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditBill(bill) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Memo #${bill.memoNumber} • ${bill.date}", fontWeight = FontWeight.Bold)
                                        Text("${bill.items.size} item(s) • Total: ${IndianCurrencyFormatter.formatInr(bill.totalPayable, currencySymbol)}", style = MaterialTheme.typography.bodySmall)
                                        if (bill.balanceDue > 0.009) {
                                            Text("Due: ${IndianCurrencyFormatter.formatInr(bill.balanceDue, currencySymbol)}", color = NurseryColors.DueRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = { onPrintBill(bill) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { onShareBillJpeg(bill) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Image, contentDescription = "Share JPEG", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { onShareBillPdf(bill) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Share PDF", modifier = Modifier.size(16.dp))
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
}
