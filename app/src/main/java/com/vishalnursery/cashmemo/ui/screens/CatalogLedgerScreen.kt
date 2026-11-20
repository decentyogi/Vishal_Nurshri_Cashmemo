package com.vishalnursery.cashmemo.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.vishalnursery.cashmemo.data.repository.PlantRateImportPolicy
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.model.RateNameStyle
import com.vishalnursery.cashmemo.domain.model.ScanEngine
import com.vishalnursery.cashmemo.domain.util.AiPrompts
import com.vishalnursery.cashmemo.domain.util.AiTextParser
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.domain.util.ParsedRateRow
import com.vishalnursery.cashmemo.domain.util.PlantNameMatcher
import com.vishalnursery.cashmemo.ui.components.AiPasteSheet
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import com.vishalnursery.cashmemo.ui.viewmodel.RateReviewSource
import com.vishalnursery.cashmemo.ui.viewmodel.RateScanUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
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
    rateScan: RateScanUiState,
    onSelectScanEngine: (ScanEngine) -> Unit,
    onSelectRateNameStyle: (RateNameStyle) -> Unit,
    onScanRateImage: (Uri) -> Unit,
    onOpenRatePaste: (String?) -> Unit,
    onCloseRatePaste: () -> Unit,
    onUpdateRatePasteText: (String) -> Unit,
    onSubmitRatePaste: () -> Unit,
    onFileRatesParsed: (List<ParsedRateRow>) -> Unit,
    onDismissRateReview: () -> Unit,
    onConfirmRateImport: (List<PlantRate>, PlantRateImportPolicy) -> Unit,
    onClearRateScanError: () -> Unit,
    onOpenSettings: () -> Unit,
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

    // File Import launcher (JSON / CSV)
    val fileImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val parsed = parsePlantRatesFromUri(context, uri)
                if (parsed.isNotEmpty()) {
                    onFileRatesParsed(parsed)
                }
            }
        }
    }

    // Camera & Gallery launchers
    val photoFile = remember { File(context.cacheDir, "rate_list_capture.jpg") }
    val photoUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onScanRateImage(uri)
        }
    }

    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            onScanRateImage(photoUri)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                takePhotoLauncher.launch(photoUri)
            } catch (e: Exception) {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        } else {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            try {
                takePhotoLauncher.launch(photoUri)
            } catch (e: Exception) {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
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
            },
            onDelete = { target ->
                deletingPlantRate = target
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

    // Modal: Rate Import / Scan Review Dialog (Part 8)
    rateScan.pendingRows?.let { rows ->
        RateImportReviewDialog(
            parsedRows = rows,
            source = rateScan.pendingSource,
            imageUri = rateScan.imageUri,
            existingRates = plantRates,
            currencySymbol = settings.currencySymbol,
            onDismiss = onDismissRateReview,
            onApply = onConfirmRateImport
        )
    }

    // Modal: AI Paste Sheet for Rate List (Part 6)
    if (rateScan.isPasteOpen) {
        AiPasteSheet(
            title = "Rate list from another AI",
            promptText = AiPrompts.getRateListPrompt(settings.rateNameStyle),
            hint = "Paste the AI's reply here",
            liveStatus = { text ->
                if (text.isBlank()) null
                else {
                    val parsed = AiTextParser.parseRateList(text)
                    val needPrice = parsed.count { it.wholesalePrice == 0.0 && it.retailPrice == 0.0 }
                    if (parsed.isNotEmpty()) {
                        "${parsed.size} plants found" + (if (needPrice > 0) " - $needPrice need a price" else "")
                    } else {
                        "No plants detected yet"
                    }
                }
            },
            onSubmit = { onSubmitRatePaste() },
            onDismiss = onCloseRatePaste,
            imageUri = rateScan.imageUri,
            onPickImage = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            currentTranslationMode = settings.translationMode,
            onSelectTranslationMode = {},
            chipsCaption = "Plant names:",
            chipLabels = RateNameStyle.entries.map { it.label },
            selectedChipIndex = RateNameStyle.entries.indexOf(settings.rateNameStyle),
            onSelectChip = { onSelectRateNameStyle(RateNameStyle.entries[it]) },
            readyCheck = { AiTextParser.parseRateList(it).isNotEmpty() },
            submitLabel = "Review rates",
            pastedText = rateScan.pasteText,
            onPastedTextChanged = onUpdateRatePasteText
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
                    rateScan = rateScan,
                    onSelectScanEngine = onSelectScanEngine,
                    onSelectRateNameStyle = onSelectRateNameStyle,
                    onOpenRatePaste = onOpenRatePaste,
                    onClearRateScanError = onClearRateScanError,
                    onOpenSettings = onOpenSettings,
                    onAddPlant = {
                        plantRateDialogTarget = null
                        showPlantRateDialog = true
                    },
                    onEditPlant = { rate ->
                        plantRateDialogTarget = rate
                        showPlantRateDialog = true
                    },
                    onTakePhoto = { launchCamera() },
                    onChooseGallery = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onImportFile = { fileImportLauncher.launch("*/*") },
                    onExportCsv = onExportPlantRatesCsv,
                    onExportJson = onExportPlantRatesJson
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlantRatesTabPage(
    plantRates: List<PlantRate>,
    settings: BusinessSettings,
    rateScan: RateScanUiState,
    onSelectScanEngine: (ScanEngine) -> Unit,
    onSelectRateNameStyle: (RateNameStyle) -> Unit,
    onOpenRatePaste: (String?) -> Unit,
    onClearRateScanError: () -> Unit,
    onOpenSettings: () -> Unit,
    onAddPlant: () -> Unit,
    onEditPlant: (PlantRate) -> Unit,
    onTakePhoto: () -> Unit,
    onChooseGallery: () -> Unit,
    onImportFile: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Action Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Button(
                        onClick = onAddPlant,
                        colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Plant", maxLines = 1, softWrap = false)
                    }
                }

                var scanMenuOpen by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { scanMenuOpen = true },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Scan list", maxLines = 1, softWrap = false)
                    }
                    DropdownMenu(
                        expanded = scanMenuOpen,
                        onDismissRequest = { scanMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Take photo") },
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                            onClick = {
                                scanMenuOpen = false
                                onTakePhoto()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Choose from gallery") },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null) },
                            onClick = {
                                scanMenuOpen = false
                                onChooseGallery()
                            }
                        )
                    }
                }

                var fileMenuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(
                        onClick = { fileMenuOpen = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More actions", tint = NurseryColors.DarkInk)
                    }
                    DropdownMenu(
                        expanded = fileMenuOpen,
                        onDismissRequest = { fileMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Import from file (JSON / CSV)") },
                            leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                            onClick = {
                                fileMenuOpen = false
                                onImportFile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export CSV (Excel)") },
                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                            onClick = {
                                fileMenuOpen = false
                                onExportCsv()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export JSON") },
                            leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                            onClick = {
                                fileMenuOpen = false
                                onExportJson()
                            }
                        )
                    }
                }
            }
        }

        // 2. Engine Strip
        item {
            RateEngineStrip(
                scanEngine = settings.scanEngine,
                onSelectScanEngine = onSelectScanEngine,
                onOpenRatePaste = { onOpenRatePaste(null) },
                currentStyle = settings.rateNameStyle,
                onSelectStyle = onSelectRateNameStyle
            )
        }

        // 3. Animated Status Card
        item {
            if (rateScan.isScanning) {
                Surface(
                    color = NurseryColors.GreenSoft,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NurseryColors.BotanicalGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(
                            text = "Gemini is reading your rate list...",
                            style = MaterialTheme.typography.bodySmall,
                            color = NurseryColors.DarkInk
                        )
                    }
                }
            } else if (rateScan.errorMessage != null) {
                Surface(
                    color = NurseryColors.WarningSoft,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, NurseryColors.WarningHeader),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = NurseryColors.WarningHeader, modifier = Modifier.size(18.dp))
                            Text(
                                text = rateScan.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = NurseryColors.DarkInk,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onOpenRatePaste(rateScan.imageUri) },
                                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Use another AI", fontSize = 12.sp)
                            }

                            if (rateScan.errorMessage.contains("Settings", ignoreCase = true) ||
                                rateScan.errorMessage.contains("API key", ignoreCase = true)
                            ) {
                                OutlinedButton(
                                    onClick = onOpenSettings,
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Open Settings", fontSize = 12.sp)
                                }
                            }

                            TextButton(
                                onClick = onClearRateScanError,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Dismiss", color = NurseryColors.MutedInk, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 4. Sticky Header: Search + Sort + Category Chips + Count Line
        stickyHeader {
            Surface(
                color = NurseryColors.CreamBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 56.dp)
                        )

                        Box {
                            OutlinedButton(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(56.dp)
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

                    if (plantRates.isNotEmpty()) {
                        Text(
                            text = "${filteredRates.size} plants",
                            style = MaterialTheme.typography.labelSmall,
                            color = NurseryColors.MutedInk
                        )
                    }
                }
            }
        }

        // 5. Plant Rate Cards or Empty State
        if (filteredRates.isEmpty()) {
            item {
                if (plantRates.isEmpty()) {
                    EmptyRateListState(
                        onScan = onTakePhoto,
                        onPaste = { onOpenRatePaste(null) },
                        onImport = onImportFile,
                        onAdd = onAddPlant
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No plants match '$searchQuery'.", style = MaterialTheme.typography.bodyMedium, color = NurseryColors.MutedInk)
                    }
                }
            }
        } else {
            items(filteredRates, key = { it.id }) { rate ->
                PlantRateCard(
                    rate = rate,
                    currencySymbol = settings.currencySymbol,
                    onEdit = { onEditPlant(rate) }
                )
            }
        }
    }
}

@Composable
private fun RateEngineStrip(
    scanEngine: ScanEngine,
    onSelectScanEngine: (ScanEngine) -> Unit,
    onOpenRatePaste: () -> Unit,
    currentStyle: RateNameStyle,
    onSelectStyle: (RateNameStyle) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Gemini | Other AI segmented pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = NurseryColors.CreamBackground,
            border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
        ) {
            Row(modifier = Modifier.padding(2.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (scanEngine == ScanEngine.GEMINI) NurseryColors.Maroon else Color.Transparent,
                    onClick = { onSelectScanEngine(ScanEngine.GEMINI) }
                ) {
                    Text(
                        text = "Gemini",
                        color = if (scanEngine == ScanEngine.GEMINI) Color.White else NurseryColors.DarkInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (scanEngine == ScanEngine.OTHER_AI) NurseryColors.Maroon else Color.Transparent,
                    onClick = { onSelectScanEngine(ScanEngine.OTHER_AI) }
                ) {
                    Text(
                        text = "Other AI",
                        color = if (scanEngine == ScanEngine.OTHER_AI) Color.White else NurseryColors.DarkInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Outlined pill: Paste AI text
        OutlinedButton(
            onClick = onOpenRatePaste,
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Paste AI text", fontSize = 12.sp)
        }

        Icon(
            imageVector = Icons.Default.Translate,
            contentDescription = null,
            tint = NurseryColors.MutedInk,
            modifier = Modifier.size(18.dp)
        )

        // Filter chips for RateNameStyle
        RateNameStyle.entries.forEach { style ->
            FilterChip(
                selected = currentStyle == style,
                onClick = { onSelectStyle(style) },
                label = { Text(style.label, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NurseryColors.GreenSoft,
                    selectedLabelColor = NurseryColors.GreenHeader
                )
            )
        }
    }
}

@Composable
private fun EmptyRateListState(
    onScan: () -> Unit,
    onPaste: () -> Unit,
    onImport: () -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No plant rates yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NurseryColors.DarkInk
            )
            Text(
                text = "Add or scan your nursery price list to get instant autocomplete while billing.",
                style = MaterialTheme.typography.bodySmall,
                color = NurseryColors.MutedInk,
                textAlign = TextAlign.Center
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            EmptyOptionRow(
                icon = Icons.Default.CameraAlt,
                title = "Scan a rate list",
                subtitle = "Take a photo or choose from gallery",
                onClick = onScan
            )
            EmptyOptionRow(
                icon = Icons.Default.ContentPaste,
                title = "Paste AI text",
                subtitle = "Paste rates from ChatGPT, Gemini, or Claude",
                onClick = onPaste
            )
            EmptyOptionRow(
                icon = Icons.Default.FileUpload,
                title = "Import a file",
                subtitle = "Load prices from JSON or CSV",
                onClick = onImport
            )
            EmptyOptionRow(
                icon = Icons.Default.Add,
                title = "Add manually",
                subtitle = "Type plant name, wholesale and retail price",
                onClick = onAdd
            )
        }
    }
}

@Composable
private fun EmptyOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = NurseryColors.PaperSurface,
        border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = NurseryColors.MaroonLight,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = NurseryColors.Maroon, modifier = Modifier.size(18.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = NurseryColors.DarkInk)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = NurseryColors.MutedInk)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = NurseryColors.MutedInk)
        }
    }
}

@Composable
private fun PlantRateCard(
    rate: PlantRate,
    currencySymbol: String,
    onEdit: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
        border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rate.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = NurseryColors.DarkInk
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
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

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Wholesale: ",
                        fontSize = 11.sp,
                        color = NurseryColors.MutedInk
                    )
                    Text(
                        text = IndianCurrencyFormatter.formatInr(rate.wholesalePrice, currencySymbol),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NurseryColors.BotanicalGreen
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text(
                        text = "Retail: ",
                        fontSize = 11.sp,
                        color = NurseryColors.MutedInk
                    )
                    Text(
                        text = IndianCurrencyFormatter.formatInr(rate.retailPrice, currencySymbol),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NurseryColors.Maroon
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
    onSave: (PlantRate) -> Unit,
    onDelete: (PlantRate) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var category by remember(initial) { mutableStateOf(initial?.category ?: "") }
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
        allRates.map { it.category.trim() }.filter { it.isNotBlank() }.distinct().take(6)
    }

    val isValid = name.isNotBlank() && ((retailText.toDoubleOrNull() ?: 0.0) > 0.0 || (wholesaleText.toDoubleOrNull() ?: 0.0) > 0.0)

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
                        value = wholesaleText,
                        onValueChange = { wholesaleText = it },
                        label = { Text("Wholesale ($currencySymbol)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = retailText,
                        onValueChange = { retailText = it },
                        label = { Text("Retail ($currencySymbol)") },
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
                    val saved = (initial ?: PlantRate(name = name.trim())).copy(
                        name = name.trim(),
                        category = category.trim(),
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing) {
                    TextButton(
                        onClick = {
                            initial?.let { onDelete(it) }
                            onDismiss()
                        }
                    ) {
                        Text("Delete", color = NurseryColors.DueRed)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun RateImportReviewDialog(
    parsedRows: List<ParsedRateRow>,
    source: RateReviewSource,
    imageUri: String?,
    existingRates: List<PlantRate>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onApply: (List<PlantRate>, PlantRateImportPolicy) -> Unit
) {
    var policy by remember { mutableStateOf(PlantRateImportPolicy.ADD_NEW_AND_UPDATE_EXISTING) }
    var isMoreOptionsOpen by remember { mutableStateOf(false) }
    var showReplaceConfirmDialog by remember { mutableStateOf(false) }

    // Collapsible photo state
    var showLargePhoto by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var thumbnailBitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUri) {
        if (!imageUri.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(imageUri)
                    val stream = context.contentResolver.openInputStream(uri)
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    val bmp = BitmapFactory.decodeStream(stream, null, opts)
                    stream?.close()
                    bmp
                }.getOrNull()?.let { thumbnailBitmap = it }
            }
        }
    }

    // Editable state keyed by row.id
    val nameEdits = remember(parsedRows) {
        mutableStateMapOf<String, String>().apply {
            parsedRows.forEach { row -> put(row.id, row.name) }
        }
    }
    val wholesaleEdits = remember(parsedRows) {
        mutableStateMapOf<String, String>().apply {
            parsedRows.forEach { row ->
                put(row.id, if (row.wholesalePrice > 0.0) row.wholesalePrice.toString() else "")
            }
        }
    }
    val retailEdits = remember(parsedRows) {
        mutableStateMapOf<String, String>().apply {
            parsedRows.forEach { row ->
                put(row.id, if (row.retailPrice > 0.0) row.retailPrice.toString() else "")
            }
        }
    }
    val categoryEdits = remember(parsedRows) {
        mutableStateMapOf<String, String>().apply {
            parsedRows.forEach { row -> put(row.id, row.category) }
        }
    }

    // Initial checked state: valid rows with prices and names start checked, problems start unchecked
    val checkedMap = remember(parsedRows) {
        mutableStateMapOf<String, Boolean>().apply {
            val nameCounts = parsedRows.groupingBy { PlantNameMatcher.normalize(it.name) }.eachCount()
            parsedRows.forEach { row ->
                val norm = PlantNameMatcher.normalize(row.name)
                val isDup = (nameCounts[norm] ?: 0) > 1
                val hasPrice = row.retailPrice > 0.0 || row.wholesalePrice > 0.0
                val hasName = row.name.isNotBlank() && row.name != "?"
                put(row.id, hasPrice && hasName && row.isValid && !isDup)
            }
        }
    }

    // Duplicate detection in this list
    val currentNormCounts = remember(nameEdits.values.toList()) {
        nameEdits.values.groupingBy { PlantNameMatcher.normalize(it) }.eachCount()
    }

    // Summary counts
    val summary = remember(nameEdits.values.toList(), wholesaleEdits.values.toList(), retailEdits.values.toList(), existingRates) {
        var newCount = 0
        var updateCount = 0
        var needCheckCount = 0

        parsedRows.forEach { row ->
            val name = nameEdits[row.id] ?: ""
            val w = wholesaleEdits[row.id]?.toDoubleOrNull() ?: 0.0
            val r = retailEdits[row.id]?.toDoubleOrNull() ?: 0.0
            val match = existingRates.firstOrNull { PlantNameMatcher.matches(it.name, name) }

            if (name.isBlank() || (w == 0.0 && r == 0.0) || (w > r && r > 0.0)) {
                needCheckCount++
            } else if (match == null) {
                newCount++
            } else if (match.retailPrice != r || match.wholesalePrice != w) {
                updateCount++
            }
        }
        Triple(newCount, updateCount, needCheckCount)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .imePadding(),
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
                        Text(
                            text = "Review Plant Rates",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = NurseryColors.DarkInk
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text("${summary.first} new", fontSize = 11.sp, color = NurseryColors.BotanicalGreen, fontWeight = FontWeight.Bold)
                            Text("•", fontSize = 11.sp, color = NurseryColors.MutedInk)
                            Text("${summary.second} update", fontSize = 11.sp, color = NurseryColors.Maroon, fontWeight = FontWeight.Bold)
                            if (summary.third > 0) {
                                Text("•", fontSize = 11.sp, color = NurseryColors.MutedInk)
                                Text("${summary.third} need checking", fontSize = 11.sp, color = NurseryColors.WarningHeader, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NurseryColors.DarkInk)
                    }
                }

                // Collapsible photo strip when image exists
                if (thumbnailBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NurseryColors.PaperSurface,
                        border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showLargePhoto = !showLargePhoto },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Image(
                                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                                    contentDescription = "Original scan photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (showLargePhoto) "Hide photo preview" else "Tap photo to compare digits",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NurseryColors.Maroon
                                    )
                                    Text(
                                        text = "Compare against handwritten values while checking",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NurseryColors.MutedInk
                                    )
                                }
                                Icon(
                                    imageVector = if (showLargePhoto) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = NurseryColors.MutedInk
                                )
                            }

                            if (showLargePhoto) {
                                Spacer(Modifier.height(8.dp))
                                Image(
                                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                                    contentDescription = "Full photo preview",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                        }
                    }
                }

                // Toolbar: Select all | Select none | Swap Wholesale <-> Retail
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            parsedRows.forEach { row ->
                                val currentName = nameEdits[row.id] ?: ""
                                val currentW = wholesaleEdits[row.id]?.toDoubleOrNull() ?: 0.0
                                val currentR = retailEdits[row.id]?.toDoubleOrNull() ?: 0.0
                                if (currentName.isNotBlank() && (currentW > 0.0 || currentR > 0.0)) {
                                    checkedMap[row.id] = true
                                }
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Select all", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { checkedMap.clear() },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Select none", fontSize = 11.sp)
                    }

                    Spacer(Modifier.weight(1f))

                    OutlinedButton(
                        onClick = {
                            parsedRows.forEach { row ->
                                val oldW = wholesaleEdits[row.id] ?: ""
                                val oldR = retailEdits[row.id] ?: ""
                                wholesaleEdits[row.id] = oldR
                                retailEdits[row.id] = oldW
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Swap W <-> R", fontSize = 11.sp)
                    }
                }

                // Rows list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(parsedRows, key = { it.id }) { row ->
                        val isChecked = checkedMap[row.id] ?: false
                        val currentName = nameEdits[row.id] ?: ""
                        val currentWText = wholesaleEdits[row.id] ?: ""
                        val currentRText = retailEdits[row.id] ?: ""
                        val currentW = currentWText.toDoubleOrNull() ?: 0.0
                        val currentR = currentRText.toDoubleOrNull() ?: 0.0
                        val hasPrice = currentW > 0.0 || currentR > 0.0
                        val isNameMissing = currentName.isBlank()
                        val norm = PlantNameMatcher.normalize(currentName)
                        val isDuplicateInList = norm.isNotBlank() && (currentNormCounts[norm] ?: 0) > 1

                        val match = existingRates.firstOrNull { PlantNameMatcher.matches(it.name, currentName) }

                        val isWarningWholesaleHigher = currentW > currentR && currentR > 0.0

                        val statusLabel: String
                        val statusColor: Color
                        val statusBg: Color

                        when {
                            isNameMissing -> {
                                statusLabel = "Name missing"
                                statusColor = Color.White
                                statusBg = NurseryColors.DueRed
                            }
                            !hasPrice -> {
                                statusLabel = "Missing price"
                                statusColor = Color.White
                                statusBg = NurseryColors.DueRed
                            }
                            isDuplicateInList -> {
                                statusLabel = "Duplicate in list"
                                statusColor = NurseryColors.WarningHeader
                                statusBg = NurseryColors.WarningSoft
                            }
                            isWarningWholesaleHigher -> {
                                statusLabel = "Check: wholesale higher than retail"
                                statusColor = NurseryColors.WarningHeader
                                statusBg = NurseryColors.WarningSoft
                            }
                            match == null -> {
                                statusLabel = "New"
                                statusColor = NurseryColors.GreenHeader
                                statusBg = NurseryColors.GreenSoft
                            }
                            match.retailPrice != currentR || match.wholesalePrice != currentW -> {
                                statusLabel = "Will update (R ${match.retailPrice.toInt()} -> ${currentR.toInt()})"
                                statusColor = NurseryColors.Maroon
                                statusBg = NurseryColors.MaroonLight
                            }
                            else -> {
                                statusLabel = "Unchanged"
                                statusColor = NurseryColors.MutedInk
                                statusBg = NurseryColors.CreamBackground
                            }
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                            border = BorderStroke(1.dp, if (!hasPrice || isNameMissing) NurseryColors.DueRed.copy(alpha = 0.5f) else NurseryColors.OutlineSoft),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            if (hasPrice && !isNameMissing) {
                                                checkedMap[row.id] = checked
                                            }
                                        },
                                        enabled = hasPrice && !isNameMissing,
                                        colors = CheckboxDefaults.colors(checkedColor = NurseryColors.Maroon)
                                    )

                                    OutlinedTextField(
                                        value = currentName,
                                        onValueChange = { newName ->
                                            nameEdits[row.id] = newName
                                            if (newName.isNotBlank() && hasPrice) checkedMap[row.id] = true
                                        },
                                        label = { Text("Plant Name") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 56.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = currentWText,
                                        onValueChange = {
                                            wholesaleEdits[row.id] = it
                                            if ((it.toDoubleOrNull() ?: 0.0) > 0.0 && !isNameMissing) checkedMap[row.id] = true
                                        },
                                        label = { Text("Wholesale ($currencySymbol)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 56.dp)
                                    )

                                    OutlinedTextField(
                                        value = currentRText,
                                        onValueChange = {
                                            retailEdits[row.id] = it
                                            if ((it.toDoubleOrNull() ?: 0.0) > 0.0 && !isNameMissing) checkedMap[row.id] = true
                                        },
                                        label = { Text("Retail ($currencySymbol)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 56.dp)
                                    )
                                }

                                Surface(
                                    color = statusBg,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = statusLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Import Policy Selector
                Card(
                    colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                    border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { policy = PlantRateImportPolicy.ADD_NEW_AND_UPDATE_EXISTING }
                        ) {
                            RadioButton(
                                selected = policy == PlantRateImportPolicy.ADD_NEW_AND_UPDATE_EXISTING,
                                onClick = { policy = PlantRateImportPolicy.ADD_NEW_AND_UPDATE_EXISTING },
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = NurseryColors.Maroon)
                            )
                            Text("Add new + update existing", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { policy = PlantRateImportPolicy.ADD_NEW_ONLY }
                        ) {
                            RadioButton(
                                selected = policy == PlantRateImportPolicy.ADD_NEW_ONLY,
                                onClick = { policy = PlantRateImportPolicy.ADD_NEW_ONLY },
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = NurseryColors.Maroon)
                            )
                            Text("Add new only", style = MaterialTheme.typography.bodySmall)
                        }

                        if (source == RateReviewSource.FILE) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isMoreOptionsOpen = !isMoreOptionsOpen }
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("More options", style = MaterialTheme.typography.labelSmall, color = NurseryColors.MutedInk)
                                Icon(
                                    if (isMoreOptionsOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = NurseryColors.MutedInk
                                )
                            }

                            if (isMoreOptionsOpen) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showReplaceConfirmDialog = true }
                                ) {
                                    RadioButton(
                                        selected = policy == PlantRateImportPolicy.REPLACE_ALL,
                                        onClick = { showReplaceConfirmDialog = true },
                                        colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = NurseryColors.DueRed)
                                    )
                                    Text("Replace all plant rates (destructive)", style = MaterialTheme.typography.bodySmall, color = NurseryColors.DueRed)
                                }
                            }
                        }
                    }
                }

                // Footer Actions
                val checkedCount = checkedMap.values.count { it }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val approved = parsedRows.mapIndexedNotNull { _, row ->
                                if (checkedMap[row.id] == true) {
                                    val name = (nameEdits[row.id] ?: row.name).trim()
                                    val r = retailEdits[row.id]?.toDoubleOrNull() ?: 0.0
                                    val w = wholesaleEdits[row.id]?.toDoubleOrNull() ?: r
                                    val cat = (categoryEdits[row.id] ?: row.category).trim()
                                    PlantRate(
                                        name = name,
                                        category = cat,
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
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Text("Save $checkedCount plants", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showReplaceConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showReplaceConfirmDialog = false },
            title = { Text("Replace All Plant Rates?", color = NurseryColors.DueRed, fontWeight = FontWeight.Bold) },
            text = {
                Text("This will permanently delete all ${existingRates.size} existing plant rates and replace them with the imported list. Are you sure?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        policy = PlantRateImportPolicy.REPLACE_ALL
                        showReplaceConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.DueRed)
                ) {
                    Text("Confirm Replace All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private suspend fun parsePlantRatesFromUri(context: Context, uri: Uri): List<ParsedRateRow> = withContext(Dispatchers.IO) {
    runCatching {
        val stream = context.contentResolver.openInputStream(uri) ?: return@withContext emptyList()
        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
        val rawText = reader.readText()
        reader.close()

        val text = rawText.removePrefix("\uFEFF").trim()

        // 1. JSON Parsing: bare array, {"rates": [...]}, or backup format
        if ((text.startsWith("[") && text.endsWith("]")) || (text.startsWith("{") && text.endsWith("}"))) {
            val jsonRows = runCatching {
                val array = if (text.startsWith("[")) {
                    JSONArray(text)
                } else {
                    val root = JSONObject(text)
                    root.optJSONArray("rates")
                }
                if (array != null && array.length() > 0) {
                    val list = mutableListOf<ParsedRateRow>()
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val name = item.optString("name", item.optString("plant", item.optString("PlantName", ""))).trim()
                        val cat = item.optString("category", item.optString("Category", "")).trim()
                        val r = item.optDouble("retailPrice", item.optDouble("retail", item.optDouble("RetailPrice", 0.0)))
                        val w = item.optDouble("wholesalePrice", item.optDouble("wholesale", item.optDouble("WholesalePrice", 0.0)))
                        val hasPrice = r > 0.0 || w > 0.0
                        val nameMissing = name.isBlank() || name == "?"
                        list.add(
                            ParsedRateRow(
                                name = if (name == "?") "" else name,
                                category = cat,
                                retailPrice = r,
                                wholesalePrice = w,
                                isValid = !nameMissing && hasPrice,
                                statusNote = if (nameMissing) "Name missing" else if (!hasPrice) "Missing price" else ""
                            )
                        )
                    }
                    list
                } else null
            }.getOrNull()

            if (!jsonRows.isNullOrEmpty()) {
                return@withContext jsonRows
            }
        }

        // 2. Real CSV Parsing
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return@withContext emptyList()

        // Detect delimiter (comma, semicolon, tab)
        val firstLine = lines.first()
        val commaCount = firstLine.count { it == ',' }
        val semiCount = firstLine.count { it == ';' }
        val tabCount = firstLine.count { it == '\t' }
        val delimiter = when {
            semiCount > commaCount && semiCount > tabCount -> ';'
            tabCount > commaCount && tabCount > semiCount -> '\t'
            else -> ','
        }

        fun parseCsvRow(row: String): List<String> {
            val cells = mutableListOf<String>()
            val sb = java.lang.StringBuilder()
            var inQuotes = false
            var i = 0
            while (i < row.length) {
                val c = row[i]
                if (c == '\"') {
                    if (inQuotes && i + 1 < row.length && row[i + 1] == '\"') {
                        sb.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                } else if (c == delimiter && !inQuotes) {
                    cells.add(sb.toString().trim())
                    sb.setLength(0)
                } else {
                    sb.append(c)
                }
                i++
            }
            cells.add(sb.toString().trim())
            return cells
        }

        val firstRowCells = parseCsvRow(firstLine)
        val lowerHeaders = firstRowCells.map { it.lowercase() }

        var nameCol = -1
        var catCol = -1
        var retailCol = -1
        var wholesaleCol = -1

        for (i in lowerHeaders.indices) {
            val h = lowerHeaders[i]
            when {
                nameCol == -1 && (h.contains("plant") || h.contains("name") || h.contains("item")) -> nameCol = i
                catCol == -1 && h.contains("category") -> catCol = i
                wholesaleCol == -1 && h.contains("wholesale") -> wholesaleCol = i
                retailCol == -1 && (h.contains("retail") || h.contains("price") || h.contains("rate")) -> retailCol = i
            }
        }

        val hasHeader = nameCol != -1 || retailCol != -1 || wholesaleCol != -1
        val dataLines = if (hasHeader) lines.drop(1) else lines

        if (!hasHeader) {
            // Assume order: Name, Wholesale, Retail (or Name, Retail if 2 columns)
            if (firstRowCells.size >= 3) {
                nameCol = 0
                wholesaleCol = 1
                retailCol = 2
            } else {
                nameCol = 0
                wholesaleCol = -1
                retailCol = 1
            }
        }

        val parsed = mutableListOf<ParsedRateRow>()
        for (line in dataLines) {
            val cells = parseCsvRow(line)
            if (cells.isEmpty()) continue
            val rawName = if (nameCol in cells.indices) cells[nameCol] else cells.firstOrNull().orEmpty()
            val cat = if (catCol in cells.indices) cells[catCol] else ""
            val wStr = if (wholesaleCol in cells.indices) AiTextParser.cleanNumber(cells[wholesaleCol]) else ""
            val rStr = if (retailCol in cells.indices) AiTextParser.cleanNumber(cells[retailCol]) else ""
            val w = wStr.toDoubleOrNull() ?: 0.0
            val r = rStr.toDoubleOrNull() ?: 0.0
            val nameMissing = rawName.isBlank() || rawName == "?"
            val name = if (rawName == "?") "" else rawName
            val hasPrice = r > 0.0 || w > 0.0

            parsed.add(
                ParsedRateRow(
                    name = name,
                    category = cat,
                    retailPrice = r,
                    wholesalePrice = w,
                    isValid = !nameMissing && hasPrice,
                    statusNote = if (nameMissing) "Name missing" else if (!hasPrice) "Missing price" else ""
                )
            )
        }
        parsed
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
    val isValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer / Nursery Name *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile / Phone") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Village / Address") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val saved = (initial ?: CustomerProfile(name = name.trim())).copy(
                        name = name.trim(),
                        phoneNumber = phone.trim(),
                        address = address.trim()
                    )
                    onSave(saved)
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SettleDueDialog(
    customer: CustomerProfile,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, String) -> Unit
) {
    var amountText by remember { mutableStateOf(if (customer.outstandingBalance > 0.0) customer.outstandingBalance.toString() else "") }
    var note by remember { mutableStateOf("Paid in Cash") }
    val enteredAmount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = enteredAmount > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settle Customer Due", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Customer: ${customer.name}", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Current Due: ${IndianCurrencyFormatter.formatInr(customer.outstandingBalance, currencySymbol)}",
                    color = NurseryColors.DueRed,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Paid Amount ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Payment Note (e.g. Paid via UPI)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(enteredAmount, note.trim()) },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen)
            ) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
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
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = NurseryColors.CreamBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header with actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NurseryColors.DarkInk)
                        if (customer.phoneNumber.isNotBlank()) {
                            Text(customer.phoneNumber, style = MaterialTheme.typography.bodySmall, color = NurseryColors.MutedInk)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Balance summary card
                Card(
                    colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                    border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Outstanding Due", style = MaterialTheme.typography.labelSmall, color = NurseryColors.MutedInk)
                            Text(
                                text = IndianCurrencyFormatter.formatInr(customer.outstandingBalance, currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (customer.outstandingBalance > 0.009) NurseryColors.DueRed else NurseryColors.BotanicalGreen
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (customer.outstandingBalance > 0.009) {
                                Button(
                                    onClick = onSettleDue,
                                    colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Settle Due", fontSize = 12.sp)
                                }
                            }
                            OutlinedButton(
                                onClick = onEditCustomer,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Edit Profile", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // New bill button
                Button(
                    onClick = onCreateNewBill,
                    colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Create New Bill for this Customer", fontWeight = FontWeight.Bold)
                }

                Text("Billing History (${bills.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = NurseryColors.MutedInk)

                // Bills list
                if (bills.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No past cash memos found for this customer.", color = NurseryColors.MutedInk, style = MaterialTheme.typography.bodySmall)
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
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Memo #${bill.memoNumber} • ${bill.date}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "Total: ${IndianCurrencyFormatter.formatInr(bill.totalPayable, currencySymbol)} | Paid: ${IndianCurrencyFormatter.formatInr(bill.paidAmount, currencySymbol)} | Due: ${IndianCurrencyFormatter.formatInr(bill.balanceDue, currencySymbol)}",
                                            fontSize = 11.sp,
                                            color = if (bill.balanceDue > 0.009) NurseryColors.DueRed else NurseryColors.BotanicalGreen
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onEditBill(bill) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit Bill", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { onPrintBill(bill) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Default.Print, contentDescription = "Print Bill", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { onShareBillJpeg(bill) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Default.Image, contentDescription = "Share JPEG", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(onClick = { onShareBillPdf(bill) }, modifier = Modifier.size(40.dp)) {
                                            Icon(Icons.Default.FileUpload, contentDescription = "Share PDF", modifier = Modifier.size(16.dp))
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
