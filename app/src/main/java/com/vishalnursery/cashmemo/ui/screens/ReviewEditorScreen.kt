package com.vishalnursery.cashmemo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.vishalnursery.cashmemo.domain.model.BillExportLayout
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.DiscountType
import com.vishalnursery.cashmemo.domain.model.MemoType
import com.vishalnursery.cashmemo.domain.model.OrderStatus
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.util.CustomerMatcher
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.ui.components.CompactTextField
import com.vishalnursery.cashmemo.ui.components.LocalBottomOverlayInset
import com.vishalnursery.cashmemo.ui.components.UpiQrDialog
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import com.vishalnursery.cashmemo.ui.viewmodel.EditorUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ReviewEditorScreen(
    editorState: EditorUiState,
    settings: BusinessSettings,
    customers: List<CustomerProfile>,
    plantRates: List<PlantRate>,
    onUpdateMemoNumber: (String) -> Unit,
    onUpdateDate: (String) -> Unit,
    onToggleMemoType: (MemoType) -> Unit = {},
    onUpdateDueDate: (String) -> Unit = {},
    onUpdateOrderStatus: (OrderStatus) -> Unit = {},
    onUpdateCustomerName: (String) -> Unit,
    onUpdateCustomerMobile: (String) -> Unit,
    onUpdateCustomerAddress: (String) -> Unit,
    onSelectCustomer: (CustomerProfile) -> Unit,
    onAddCustomerToLedger: () -> Unit = {},
    lastTierForCustomer: (String, String) -> Boolean? = { _, _ -> null },
    onCustomerFieldFocusLost: () -> Unit = {},
    onToggleWholesale: (Boolean) -> Unit,
    onAddItemRow: () -> Unit,
    onCloneItemRow: (Int) -> Unit,
    onRemoveItemRow: (Int) -> Unit,
    onMoveItemRow: (Int, Int) -> Unit = { _, _ -> },
    onUpdateItemDescription: (Int, String) -> Unit,
    onUpdateItemQuantity: (Int, String) -> Unit,
    onUpdateItemRate: (Int, String) -> Unit,
    onSelectPlantRate: (Int, PlantRate) -> Unit,
    onResetItemRate: (Int) -> Unit = {},
    onSavePlantRate: (PlantRate) -> Unit = {},
    onUpdateDiscount: (String) -> Unit,
    onUpdateDiscountType: (DiscountType) -> Unit,
    onUpdateTaxPercentage: (String) -> Unit,
    onUpdatePreviousDues: (String) -> Unit,
    onUpdatePaidAmount: (String) -> Unit,
    onUpdatePaymentMethod: (String) -> Unit,
    onUpdatePaymentNote: (String) -> Unit,
    onSelectExportLayout: (BillExportLayout) -> Unit,
    onSaveMemo: (Boolean) -> Unit,
    onShareAsJpeg: () -> Unit,
    onShareAsPdf: () -> Unit,
    onSavePermanentQr: (String, String, String) -> Unit = { _, _, _ -> },
    onNewMemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calc = editorState.calculation
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val totalsBringIntoViewRequester = remember { BringIntoViewRequester() }
    val bottomOverlayInset = LocalBottomOverlayInset.current
    val density = LocalDensity.current

    DisposableEffect(Unit) {
        onDispose {
            bottomOverlayInset.value = 0.dp
        }
    }

    var showRawOcr by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    var showDeliveryDatePicker by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showDeliveryDatePicker) {
        val initialMillis = runCatching {
            if (editorState.dueDate.isNotBlank()) {
                SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(editorState.dueDate.trim())?.time
            } else null
        }.getOrNull() ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDeliveryDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val formatted = SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }.format(Date(millis))
                            onUpdateDueDate(formatted)
                        }
                        showDeliveryDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDeliveryDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Active expanded row tracker by ID (survives drag reordering)
    var expandedItemId by remember { mutableStateOf<String?>(editorState.items.firstOrNull()?.id) }
    var pendingExpandNewRow by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }
    var draggedItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var scrollViewportTopInRoot by remember { mutableFloatStateOf(0f) }
    var scrollViewportHeightPx by remember { mutableFloatStateOf(0f) }
    val rowTopInRootById = remember { mutableMapOf<String, Float>() }
    val haptic = LocalHapticFeedback.current
    val latestItems by rememberUpdatedState(editorState.items)

    LaunchedEffect(editorState.items) {
        if (pendingExpandNewRow) {
            expandedItemId = editorState.items.lastOrNull()?.id
            pendingExpandNewRow = false
        } else if (expandedItemId != null && editorState.items.none { it.id == expandedItemId }) {
            expandedItemId = editorState.items.firstOrNull()?.id
        }
        if (editorState.items.size <= 1) {
            isReorderMode = false
            draggedItemId = null
            dragOffsetY = 0f
        }
    }

    val reorderRowHeightPx = with(density) { 48.dp.toPx() }
    val reorderSpacingPx = with(density) { 6.dp.toPx() }
    val reorderStepPx = reorderRowHeightPx + reorderSpacingPx
    val edgeThresholdPx = with(density) { 64.dp.toPx() }

    LaunchedEffect(draggedItemId, isReorderMode) {
        val activeId = draggedItemId
        if (activeId == null || !isReorderMode) return@LaunchedEffect
        while (isActive && draggedItemId == activeId) {
            val rowTop = rowTopInRootById[activeId]
            if (rowTop != null && scrollViewportHeightPx > 0f) {
                val visualCenterInViewport = (rowTop - scrollViewportTopInRoot) + (reorderRowHeightPx * 0.5f) + dragOffsetY
                val rawStep = when {
                    visualCenterInViewport < edgeThresholdPx -> -12f
                    visualCenterInViewport > (scrollViewportHeightPx - edgeThresholdPx) -> 12f
                    else -> 0f
                }
                if (rawStep != 0f) {
                    val actualScrolled = scrollState.scrollBy(rawStep)
                    if (kotlin.math.abs(actualScrolled) > 0.1f) {
                        dragOffsetY += actualScrolled
                        val currentIdx = latestItems.indexOfFirst { it.id == activeId }
                        if (currentIdx != -1) {
                            if (dragOffsetY > reorderStepPx * 0.5f && currentIdx < latestItems.lastIndex) {
                                onMoveItemRow(currentIdx, currentIdx + 1)
                                dragOffsetY -= reorderStepPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (dragOffsetY < -reorderStepPx * 0.5f && currentIdx > 0) {
                                onMoveItemRow(currentIdx, currentIdx - 1)
                                dragOffsetY += reorderStepPx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }
            }
            delay(16)
        }
    }

    // UPI QR Dialog: amount defaults to remaining balance due (or paid/total for fully paid)
    if (showQrDialog) {
        val qrAmount = IndianCurrencyFormatter.resolveBillQrAmount(calc.effectivePaid, calc.totalPayable, calc.balanceDue)
        UpiQrDialog(
            initialAmount = qrAmount,
            settings = settings,
            billLabel = "${if (editorState.isOrder) "Order" else "Memo"} #${editorState.memoNumber}",
            initialTab = 0,
            onSavePermanentQr = onSavePermanentQr,
            onDismiss = { showQrDialog = false }
        )
    }

    // Share & Export Bottom Sheet
    if (showShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState = sheetState,
            containerColor = NurseryColors.PaperSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share & Print ${if (editorState.isOrder) "Order" else "Memo"} #${editorState.memoNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NurseryColors.Maroon
                    )
                    IconButton(onClick = { showShareSheet = false }) {
                        Icon(Icons.Default.ExpandMore, contentDescription = "Close")
                    }
                }

                // Layout Selector Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "BILL EXPORT LAYOUT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = NurseryColors.MutedInk
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BillExportLayout.entries.forEach { layout ->
                            FilterChip(
                                selected = settings.billExportLayout == layout,
                                onClick = { onSelectExportLayout(layout) },
                                label = {
                                    Text(
                                        text = layout.shortLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = NurseryColors.OutlineSoft)

                // High-Resolution Share & Print Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showShareSheet = false
                            onShareAsJpeg()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, NurseryColors.BotanicalGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            tint = NurseryColors.BotanicalGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Send As JPEG",
                            color = NurseryColors.BotanicalGreen,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = {
                            showShareSheet = false
                            onShareAsPdf()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.MaroonDark),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Send As PDF",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            showShareSheet = false
                            onSaveMemo(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Print Memo", fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            showShareSheet = false
                            showQrDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("UPI QR Code", fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }

    val customerMatches = remember(editorState.customerName, customers) {
        val q = editorState.customerName.trim().lowercase()
        if (q.length < 2) emptyList()
        else customers.filter {
            it.name.lowercase().contains(q) || it.phoneNumber.contains(q)
        }.take(4)
    }

    val isCustomerInLedger = remember(editorState.customerName, editorState.customerMobile, customers) {
        CustomerMatcher.findMatch(customers, editorState.customerName, editorState.customerMobile) != null
    }
    val canAddCustomer = editorState.customerName.isNotBlank() || editorState.customerMobile.isNotBlank()

    val rememberedTier = remember(editorState.customerName, editorState.customerMobile, editorState.editingMemoId) {
        lastTierForCustomer(editorState.customerName, editorState.customerMobile)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // --- 1. PINNED TOP HEADER (<= 56dp, doc type left + price tier right, 360dp-safe) ---
        val canSwitchDocType = editorState.editingMemoId == null && editorState.deliveringOrderId.isNullOrBlank()
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 56.dp),
            color = NurseryColors.PaperSurface,
            border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Cash Memo | Order Memo
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = NurseryColors.CreamBackground,
                    border = BorderStroke(1.dp, NurseryColors.Maroon.copy(alpha = if (canSwitchDocType) 1f else 0.45f)),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val cashBg by animateColorAsState(if (!editorState.isOrder) NurseryColors.Maroon else Color.Transparent, label = "cashBg")
                        val cashText by animateColorAsState(if (!editorState.isOrder) Color.White else NurseryColors.Maroon, label = "cashText")
                        val orderBg by animateColorAsState(if (editorState.isOrder) NurseryColors.Maroon else Color.Transparent, label = "orderBg")
                        val orderText by animateColorAsState(if (editorState.isOrder) Color.White else NurseryColors.Maroon, label = "orderText")

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .background(color = cashBg, shape = RoundedCornerShape(18.dp))
                                .clickable(enabled = canSwitchDocType) {
                                    if (editorState.isOrder) onToggleMemoType(MemoType.CASH)
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cash Memo",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (!editorState.isOrder) FontWeight.Bold else FontWeight.Medium,
                                color = cashText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .background(color = orderBg, shape = RoundedCornerShape(18.dp))
                                .clickable(enabled = canSwitchDocType) {
                                    if (!editorState.isOrder) onToggleMemoType(MemoType.ORDER)
                                }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Order Memo",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (editorState.isOrder) FontWeight.Bold else FontWeight.Medium,
                                color = orderText
                            )
                        }
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Right: Retail | Wholesale Segmented Control (compact 360dp-safe)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = NurseryColors.CreamBackground,
                    border = BorderStroke(1.dp, NurseryColors.Maroon),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val retailBg by animateColorAsState(if (!editorState.isWholesaleMode) NurseryColors.Maroon else Color.Transparent, label = "retailBg")
                        val retailText by animateColorAsState(if (!editorState.isWholesaleMode) Color.White else NurseryColors.Maroon, label = "retailText")
                        val wholesaleBg by animateColorAsState(if (editorState.isWholesaleMode) NurseryColors.Maroon else Color.Transparent, label = "wholesaleBg")
                        val wholesaleText by animateColorAsState(if (editorState.isWholesaleMode) Color.White else NurseryColors.Maroon, label = "wholesaleText")

                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .background(color = retailBg, shape = RoundedCornerShape(18.dp))
                                .clickable { if (editorState.isWholesaleMode) onToggleWholesale(false) }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Retail",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (!editorState.isWholesaleMode) FontWeight.Bold else FontWeight.Medium,
                                color = retailText
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .background(color = wholesaleBg, shape = RoundedCornerShape(18.dp))
                                .clickable { if (!editorState.isWholesaleMode) onToggleWholesale(true) }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Wholesale",
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                fontWeight = if (editorState.isWholesaleMode) FontWeight.Bold else FontWeight.Medium,
                                color = wholesaleText
                            )
                        }
                    }
                }
            }
        }

        // Remembered Tier Hint Banner (moved out of 56dp header so both pills stay on one row at 360dp @ 1.3x)
        if (!editorState.isOrder && rememberedTier != null && rememberedTier != editorState.isWholesaleMode) {
            val tierLabel = if (rememberedTier) "Wholesale" else "Retail"
            Surface(
                onClick = { onToggleWholesale(rememberedTier) },
                color = NurseryColors.BotanicalGreenLight,
                border = BorderStroke(1.dp, NurseryColors.BotanicalGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Last bill: $tierLabel — tap to switch",
                    style = MaterialTheme.typography.labelSmall,
                    color = NurseryColors.BotanicalGreen,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        // --- 2. SCROLLABLE CONTENT ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    scrollViewportTopInRoot = coords.positionInRoot().y
                    scrollViewportHeightPx = coords.size.height.toFloat()
                }
                .verticalScroll(scrollState)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Collapsible OCR Source Card when created from Camera/Gallery Scan
            if (editorState.rawOcrText.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NurseryColors.BotanicalGreenLight),
                    border = BorderStroke(1.dp, NurseryColors.BotanicalGreen.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRawOcr = !showRawOcr },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scanned AI / OCR Reference (Tap to inspect)",
                                style = MaterialTheme.typography.labelLarge,
                                color = NurseryColors.BotanicalGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = if (showRawOcr) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = NurseryColors.BotanicalGreen
                            )
                        }
                        AnimatedVisibility(visible = showRawOcr) {
                            Text(
                                text = editorState.rawOcrText,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            // Memo Number & Date Card
            Card(
                colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editorState.editingMemoId != null || !editorState.deliveringOrderId.isNullOrBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when {
                                    !editorState.deliveringOrderId.isNullOrBlank() -> "Delivering Order -> Cash Memo"
                                    editorState.isOrder -> "Editing Order #${editorState.memoNumber}"
                                    else -> "Editing Memo #${editorState.memoNumber}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NurseryColors.Maroon
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactTextField(
                            value = editorState.memoNumber,
                            onValueChange = onUpdateMemoNumber,
                            label = {
                                Text(
                                    if (editorState.isOrder) "Order #" else "Memo #",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        CompactTextField(
                            value = editorState.date,
                            onValueChange = onUpdateDate,
                            label = {
                                Text(
                                    if (editorState.isOrder) "Order Date" else "Date (DD/MM/YYYY)",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                    }
                    if (editorState.isOrder) {
                        Surface(
                            onClick = { showDeliveryDatePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            color = NurseryColors.CreamBackground,
                            border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 44.dp)
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Expected Delivery (optional)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NurseryColors.MutedInk
                                    )
                                    Text(
                                        text = editorState.dueDate.ifBlank { "Tap to select delivery date" },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (editorState.dueDate.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (editorState.dueDate.isNotBlank()) NurseryColors.InkDark else NurseryColors.MutedInk
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (editorState.dueDate.isNotBlank()) {
                                        IconButton(
                                            onClick = { onUpdateDueDate("") },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Clear delivery date",
                                                modifier = Modifier.size(16.dp),
                                                tint = NurseryColors.MutedInk
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = "Pick delivery date",
                                        tint = NurseryColors.Maroon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (editorState.editingMemoId != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OrderStatus.entries.forEach { status ->
                                    FilterChip(
                                        selected = editorState.orderStatus == status,
                                        onClick = { onUpdateOrderStatus(status) },
                                        label = { Text(status.label, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Customer / Party Section
            Card(
                colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CUSTOMER / PARTY DETAILS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = NurseryColors.Maroon
                    )

                    // Customer / Nursery Name with Trailing "+ Add" / "Ledger" Pill
                    CompactTextField(
                        value = editorState.customerName,
                        onValueChange = onUpdateCustomerName,
                        label = { Text("Customer / Nursery Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            color = NurseryColors.InkDark
                        ),
                        trailingIcon = {
                            if (isCustomerInLedger) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = NurseryColors.BotanicalGreenLight,
                                    border = BorderStroke(1.dp, NurseryColors.BotanicalGreen),
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .height(28.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = NurseryColors.BotanicalGreen,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "Ledger",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = NurseryColors.BotanicalGreen
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (canAddCustomer) NurseryColors.MaroonLight else Color.Transparent,
                                    border = BorderStroke(
                                        1.dp,
                                        if (canAddCustomer) NurseryColors.Maroon else NurseryColors.OutlineSoft
                                    ),
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .height(28.dp)
                                        .clickable(enabled = canAddCustomer, onClick = onAddCustomerToLedger)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = "Add to Ledger",
                                            tint = if (canAddCustomer) NurseryColors.Maroon else NurseryColors.MutedInk,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "+ Add",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (canAddCustomer) NurseryColors.Maroon else NurseryColors.MutedInk
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    onCustomerFieldFocusLost()
                                }
                            }
                    )

                    // Autocomplete suggestions
                    if (customerMatches.isNotEmpty() && editorState.customerMobile.isBlank()) {
                        Surface(
                            color = NurseryColors.MaroonLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                customerMatches.forEach { match ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectCustomer(match) }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${match.name} (${match.phoneNumber})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (match.outstandingBalance > 0.009) {
                                            Text(
                                                text = "Due: ${IndianCurrencyFormatter.formatInr(match.outstandingBalance)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NurseryColors.DueRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactTextField(
                            value = editorState.customerMobile,
                            onValueChange = onUpdateCustomerMobile,
                            label = { Text("Mobile", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { focusState ->
                                    if (!focusState.isFocused) {
                                        onCustomerFieldFocusLost()
                                    }
                                }
                        )
                        CompactTextField(
                            value = editorState.customerAddress,
                            onValueChange = onUpdateCustomerAddress,
                            label = { Text("Address / Village", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                color = NurseryColors.InkDark
                            ),
                            modifier = Modifier.weight(1.3f)
                        )
                    }

                    // Warning line if bill has dues and customer is not in ledger
                    if (calc.balanceDue > 0.009 && !isCustomerInLedger) {
                        Text(
                            text = "Dues are not tracked in Ledger — tap + Add",
                            style = MaterialTheme.typography.labelSmall,
                            color = NurseryColors.DueRed,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                        )
                    }
                }
            }

            // Line Items Section (Collapsible rows on large bills)
            Card(
                colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ITEMS / PLANTS (${editorState.items.size})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = NurseryColors.Maroon,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (editorState.items.size > 1) {
                                Surface(
                                    onClick = {
                                        isReorderMode = !isReorderMode
                                        draggedItemId = null
                                        dragOffsetY = 0f
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isReorderMode) NurseryColors.Maroon else NurseryColors.MaroonLight,
                                    border = BorderStroke(1.dp, NurseryColors.Maroon.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isReorderMode) Icons.Default.Check else Icons.Default.SwapVert,
                                            contentDescription = if (isReorderMode) "Done reordering" else "Reorder rows",
                                            tint = if (isReorderMode) Color.White else NurseryColors.Maroon,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isReorderMode) "Done" else "Reorder",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isReorderMode) Color.White else NurseryColors.Maroon,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                            Text(
                                text = IndianCurrencyFormatter.formatInr(calc.subtotal, settings.currencySymbol),
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NurseryColors.BotanicalGreen,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    editorState.items.forEachIndexed { index, item ->
                        key(item.id) {
                        val isExpanded = (expandedItemId == item.id)

                        if (isReorderMode) {
                            val isDragging = draggedItemId == item.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { coords ->
                                        rowTopInRootById[item.id] = coords.positionInRoot().y
                                    }
                                    .zIndex(if (isDragging) 10f else 0f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDragging) NurseryColors.PaperSurface else NurseryColors.CreamBackground,
                                    border = BorderStroke(
                                        width = if (isDragging) 1.5.dp else 1.dp,
                                        color = if (isDragging) NurseryColors.Maroon else NurseryColors.OutlineSoft
                                    ),
                                    shadowElevation = if (isDragging) 6.dp else 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .graphicsLayer {
                                            if (isDragging) {
                                                translationY = dragOffsetY
                                                scaleX = 1.02f
                                                scaleY = 1.02f
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(end = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .pointerInput(item.id, isReorderMode) {
                                                    detectVerticalDragGestures(
                                                        onDragStart = {
                                                            draggedItemId = item.id
                                                            dragOffsetY = 0f
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        },
                                                        onVerticalDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffsetY += dragAmount
                                                            val currentIdx = latestItems.indexOfFirst { it.id == item.id }
                                                            if (currentIdx != -1) {
                                                                if (dragOffsetY > reorderStepPx * 0.5f && currentIdx < latestItems.lastIndex) {
                                                                    onMoveItemRow(currentIdx, currentIdx + 1)
                                                                    dragOffsetY -= reorderStepPx
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                } else if (dragOffsetY < -reorderStepPx * 0.5f && currentIdx > 0) {
                                                                    onMoveItemRow(currentIdx, currentIdx - 1)
                                                                    dragOffsetY += reorderStepPx
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                }
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            draggedItemId = null
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragCancel = {
                                                            draggedItemId = null
                                                            dragOffsetY = 0f
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.DragHandle,
                                                contentDescription = "Drag to reorder",
                                                tint = if (isDragging) NurseryColors.Maroon else NurseryColors.MutedInk,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NurseryColors.MaroonLight
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = NurseryColors.Maroon,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = item.description.ifBlank { "Untitled Item" },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = IndianCurrencyFormatter.formatInr(item.amount, settings.currencySymbol, false),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = NurseryColors.InkDark,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        } else if (!isExpanded) {
                            // Inactive row collapsed to single compact line
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NurseryColors.CreamBackground,
                                border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedItemId = item.id }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = NurseryColors.MaroonLight
                                    ) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = NurseryColors.Maroon,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = item.description.ifBlank { "Untitled Item" },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${formatCleanNumber(item.quantity, blankIfZero = false)} × ${formatCleanNumber(item.rate, blankIfZero = true)} = ₹${formatCleanNumber(item.amount, blankIfZero = false)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = NurseryColors.InkDark
                                    )
                                    IconButton(
                                        onClick = { onCloneItemRow(index) },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Clone Row",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (expandedItemId == item.id) {
                                                expandedItemId = editorState.items.getOrNull(index + 1)?.id
                                                    ?: editorState.items.getOrNull(index - 1)?.id
                                            }
                                            if (editorState.items.size <= 1) {
                                                pendingExpandNewRow = true
                                            }
                                            onRemoveItemRow(index)
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Row",
                                            tint = NurseryColors.DueRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Active expanded card
                            var qtyInput by remember(item.id) {
                                mutableStateOf(formatCleanNumber(item.quantity, blankIfZero = false))
                            }
                            var rateInput by remember(item.id) {
                                mutableStateOf(formatCleanNumber(item.rate, blankIfZero = true))
                            }
                            val descFocusRequester = remember { FocusRequester() }
                            val qtyFocusRequester = remember { FocusRequester() }
                            val rateFocusRequester = remember { FocusRequester() }
                            var isDescFocused by remember { mutableStateOf(false) }
                            var showSuggestions by remember { mutableStateOf(false) }

                            LaunchedEffect(item.id, item.quantity) {
                                if (item.quantity != (qtyInput.toDoubleOrNull() ?: 0.0)) {
                                    qtyInput = formatCleanNumber(item.quantity, blankIfZero = false)
                                }
                            }
                            LaunchedEffect(item.id, item.rate) {
                                val currentVal = rateInput.toDoubleOrNull() ?: 0.0
                                if (Math.abs(item.rate - currentVal) > 0.0001) {
                                    rateInput = formatCleanNumber(item.rate, blankIfZero = true)
                                }
                            }

                            val query = item.description.trim().lowercase()
                            val rateMatches = remember(query, plantRates) {
                                if (query.isBlank()) emptyList()
                                else {
                                    val startsWithName = mutableListOf<PlantRate>()
                                    val containsName = mutableListOf<PlantRate>()
                                    val containsCategory = mutableListOf<PlantRate>()

                                    for (p in plantRates) {
                                        val n = p.name.trim().lowercase()
                                        val c = p.category.trim().lowercase()
                                        if (n.startsWith(query)) {
                                            startsWithName.add(p)
                                        } else if (n.contains(query)) {
                                            containsName.add(p)
                                        } else if (c.contains(query)) {
                                            containsCategory.add(p)
                                        }
                                    }
                                    (startsWithName + containsName + containsCategory).distinctBy { it.id }.take(8)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NurseryColors.CreamBackground,
                                border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = NurseryColors.MaroonLight
                                        ) {
                                            Text(
                                                text = "#${index + 1}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = NurseryColors.Maroon,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }

                                        Text(
                                            text = IndianCurrencyFormatter.formatInr(item.amount, settings.currencySymbol),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            color = NurseryColors.InkDark
                                        )

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = { onCloneItemRow(index) },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "Clone Row",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (expandedItemId == item.id) {
                                                        expandedItemId = editorState.items.getOrNull(index + 1)?.id
                                                            ?: editorState.items.getOrNull(index - 1)?.id
                                                    }
                                                    if (editorState.items.size <= 1) {
                                                        pendingExpandNewRow = true
                                                    }
                                                    onRemoveItemRow(index)
                                                },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete Row",
                                                    tint = NurseryColors.DueRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    CompactTextField(
                                        value = item.description,
                                        onValueChange = {
                                            onUpdateItemDescription(index, it)
                                            showSuggestions = true
                                        },
                                        label = { Text("Item / Plant Description", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Sentences,
                                            imeAction = ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = {
                                                try { qtyFocusRequester.requestFocus() } catch (_: Exception) {}
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(descFocusRequester)
                                            .onFocusChanged { focusState ->
                                                isDescFocused = focusState.isFocused
                                                if (focusState.isFocused) {
                                                    showSuggestions = true
                                                }
                                            }
                                    )

                                    if ((isDescFocused || showSuggestions) && item.description.isNotBlank()) {
                                        if (rateMatches.isNotEmpty()) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = NurseryColors.PaperSurface,
                                                border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                                                shadowElevation = 2.dp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 220.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .verticalScroll(rememberScrollState())
                                                        .padding(4.dp)
                                                ) {
                                                    rateMatches.forEach { matched ->
                                                        val isWholesale = editorState.isWholesaleMode
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    onSelectPlantRate(index, matched)
                                                                    showSuggestions = false
                                                                    try {
                                                                        qtyFocusRequester.requestFocus()
                                                                    } catch (_: Exception) {}
                                                                }
                                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = matched.name,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                if (matched.category.isNotBlank()) {
                                                                    Text(
                                                                        text = matched.category,
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = NurseryColors.MutedInk
                                                                    )
                                                                }
                                                            }
                                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                if (matched.wholesalePrice > 0.0) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(4.dp),
                                                                        color = if (isWholesale) NurseryColors.BotanicalGreenLight else NurseryColors.CreamBackground
                                                                    ) {
                                                                        Text(
                                                                            text = "W: ${IndianCurrencyFormatter.formatInr(matched.wholesalePrice, settings.currencySymbol)}",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            fontWeight = if (isWholesale) FontWeight.Bold else FontWeight.Normal,
                                                                            color = if (isWholesale) NurseryColors.BotanicalGreen else NurseryColors.InkMedium,
                                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                        )
                                                                    }
                                                                }
                                                                if (matched.retailPrice > 0.0) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(4.dp),
                                                                        color = if (!isWholesale) NurseryColors.BotanicalGreenLight else NurseryColors.CreamBackground
                                                                    ) {
                                                                        Text(
                                                                            text = "R: ${IndianCurrencyFormatter.formatInr(matched.retailPrice, settings.currencySymbol)}",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            fontWeight = if (!isWholesale) FontWeight.Bold else FontWeight.Normal,
                                                                            color = if (!isWholesale) NurseryColors.BotanicalGreen else NurseryColors.InkMedium,
                                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = NurseryColors.PaperSurface,
                                                border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = "No saved plant matches",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = NurseryColors.InkMedium
                                                    )
                                                    if (item.rate > 0.0) {
                                                        TextButton(
                                                            onClick = {
                                                                onSavePlantRate(
                                                                    PlantRate(
                                                                        name = item.description.trim(),
                                                                        retailPrice = if (editorState.isWholesaleMode) 0.0 else item.rate,
                                                                        wholesalePrice = if (editorState.isWholesaleMode) item.rate else 0.0,
                                                                        category = ""
                                                                    )
                                                                )
                                                                showSuggestions = false
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Add,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp),
                                                                tint = NurseryColors.BotanicalGreen
                                                            )
                                                            Spacer(Modifier.width(4.dp))
                                                            Text(
                                                                text = "Save \"${item.description.trim()}\" to Plant Rates",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = NurseryColors.BotanicalGreen,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        CompactTextField(
                                            value = qtyInput,
                                            onValueChange = {
                                                qtyInput = it
                                                onUpdateItemQuantity(index, it)
                                            },
                                            label = { Text("Qty", maxLines = 1) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal,
                                                imeAction = ImeAction.Next
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onNext = {
                                                    try { rateFocusRequester.requestFocus() } catch (_: Exception) {}
                                                }
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .focusRequester(qtyFocusRequester)
                                        )
                                        CompactTextField(
                                            value = rateInput,
                                            onValueChange = {
                                                rateInput = it
                                                onUpdateItemRate(index, it)
                                            },
                                            label = { Text("Rate (${settings.currencySymbol})", maxLines = 1) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Decimal,
                                                imeAction = ImeAction.Next
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onNext = {
                                                    if (index < editorState.items.size - 1) {
                                                        expandedItemId = editorState.items[index + 1].id
                                                    } else {
                                                        pendingExpandNewRow = true
                                                        onAddItemRow()
                                                    }
                                                }
                                            ),
                                            modifier = Modifier
                                                .weight(1.3f)
                                                .focusRequester(rateFocusRequester)
                                        )
                                    }

                                    val linkedPlant = editorState.rateLinks[item.id]?.let { link ->
                                        plantRates.firstOrNull { it.id == link.plantRateId }
                                    } ?: plantRates.firstOrNull { it.name.trim().equals(item.description.trim(), ignoreCase = true) }

                                    if (linkedPlant != null) {
                                        val tierLabel = if (editorState.isWholesaleMode) "wholesale" else "retail"
                                        val hasTierPrice = linkedPlant.hasPriceForTier(editorState.isWholesaleMode)
                                        val savedTierPrice = if (hasTierPrice) linkedPlant.priceForTier(editorState.isWholesaleMode) else 0.0
                                        val isCustom = hasTierPrice && Math.abs(item.rate - savedTierPrice) > 0.01

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp, start = 4.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            if (!hasTierPrice) {
                                                Text(
                                                    text = "No $tierLabel rate saved yet",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NurseryColors.MutedInk
                                                )
                                            } else if (!isCustom) {
                                                Text(
                                                    text = "Saved $tierLabel ${IndianCurrencyFormatter.formatInr(savedTierPrice, settings.currencySymbol)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NurseryColors.BotanicalGreen
                                                )
                                            } else {
                                                Text(
                                                    text = "Custom — saved $tierLabel ${IndianCurrencyFormatter.formatInr(savedTierPrice, settings.currencySymbol)} (tap to reset)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = NurseryColors.Maroon,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.clickable {
                                                        onResetItemRate(index)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }

                    // Add row button: expands new row
                    OutlinedButton(
                        onClick = {
                            isReorderMode = false
                            pendingExpandNewRow = true
                            onAddItemRow()
                        },
                        border = BorderStroke(1.dp, NurseryColors.Maroon.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = NurseryColors.Maroon,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Add Item / Plant Row",
                            color = NurseryColors.Maroon,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Totals & Settlement Section
            Card(
                modifier = Modifier.bringIntoViewRequester(totalsBringIntoViewRequester),
                colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TOTALS & SETTLEMENT",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = NurseryColors.Maroon
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompactTextField(
                            value = editorState.discountText,
                            onValueChange = onUpdateDiscount,
                            label = { Text("Discount", maxLines = 1) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.2f)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (editorState.discountType == DiscountType.AMOUNT) NurseryColors.BotanicalGreenLight else NurseryColors.CreamBackground,
                            border = BorderStroke(1.dp, if (editorState.discountType == DiscountType.AMOUNT) NurseryColors.BotanicalGreen else NurseryColors.OutlineSoft),
                            modifier = Modifier
                                .clickable { onUpdateDiscountType(DiscountType.AMOUNT) }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (editorState.discountType == DiscountType.AMOUNT) NurseryColors.BotanicalGreen else NurseryColors.InkDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (editorState.discountType == DiscountType.PERCENT) NurseryColors.BotanicalGreenLight else NurseryColors.CreamBackground,
                            border = BorderStroke(1.dp, if (editorState.discountType == DiscountType.PERCENT) NurseryColors.BotanicalGreen else NurseryColors.OutlineSoft),
                            modifier = Modifier
                                .clickable { onUpdateDiscountType(DiscountType.PERCENT) }
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (editorState.discountType == DiscountType.PERCENT) NurseryColors.BotanicalGreen else NurseryColors.InkDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        CompactTextField(
                            value = editorState.taxPercentageText,
                            onValueChange = onUpdateTaxPercentage,
                            label = { Text("Tax %", maxLines = 1) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!editorState.isOrder) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CompactTextField(
                                value = editorState.previousDuesText,
                                onValueChange = onUpdatePreviousDues,
                                label = { Text("Previous Dues", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )

                            CompactTextField(
                                value = editorState.paidAmountText,
                                onValueChange = onUpdatePaidAmount,
                                label = { Text("Paid (blank = full)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        CompactTextField(
                            value = editorState.paidAmountText,
                            onValueChange = onUpdatePaidAmount,
                            label = { Text("Advance Paid (optional)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val methodOptions = if (editorState.isOrder) listOf("Cash", "Online") else listOf("Cash", "Online", "Credit")
                        methodOptions.forEach { method ->
                            FilterChip(
                                selected = editorState.paymentMethod.equals(method, ignoreCase = true),
                                onClick = { onUpdatePaymentMethod(method) },
                                label = { Text(method, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    CompactTextField(
                        value = editorState.paymentStatusNote,
                        onValueChange = onUpdatePaymentNote,
                        label = {
                            Text(
                                if (editorState.isOrder) "Order Note (optional)" else "Settlement Note (e.g. Carry Forward, Paid Online)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = NurseryColors.OutlineSoft)

                    // Calculation Summary Card
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SummaryRow("Subtotal", IndianCurrencyFormatter.formatInr(calc.subtotal, settings.currencySymbol))
                        if (calc.overallDiscountAmount > 0.009) {
                            SummaryRow("Discount", "- ${IndianCurrencyFormatter.formatInr(calc.overallDiscountAmount, settings.currencySymbol)}", valueColor = NurseryColors.Maroon)
                        }
                        if (calc.taxAmount > 0.009) {
                            SummaryRow("Tax", "+ ${IndianCurrencyFormatter.formatInr(calc.taxAmount, settings.currencySymbol)}")
                        }
                        if (!editorState.isOrder && calc.previousDues > 0.009) {
                            SummaryRow("Previous Dues", "+ ${IndianCurrencyFormatter.formatInr(calc.previousDues, settings.currencySymbol)}", valueColor = NurseryColors.DueRed)
                        }
                        SummaryRow(
                            if (editorState.isOrder) "Order Total" else "Total Payable",
                            IndianCurrencyFormatter.formatInr(calc.totalPayable, settings.currencySymbol),
                            isBold = true
                        )
                        SummaryRow(
                            if (editorState.isOrder) "Advance Paid" else "Paid Amount",
                            IndianCurrencyFormatter.formatInr(calc.effectivePaid, settings.currencySymbol)
                        )
                        SummaryRow(
                            "Balance Due",
                            IndianCurrencyFormatter.formatInr(calc.balanceDue, settings.currencySymbol),
                            valueColor = if (calc.balanceDue > 0.009) NurseryColors.DueRed else NurseryColors.BotanicalGreen,
                            isBold = true
                        )
                    }

                    Text(
                        text = "Words: ${IndianCurrencyFormatter.convertToIndianRupees(calc.totalPayable)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onNewMemo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear & Start New Blank Memo")
            }

            Spacer(Modifier.height(16.dp))
        }

        // --- 3. PINNED BOTTOM BAR (<= 64dp, hidden when keyboard is open) ---
        val isKeyboardOpen = WindowInsets.isImeVisible || WindowInsets.ime.getBottom(density) > 0
        LaunchedEffect(isKeyboardOpen) {
            if (isKeyboardOpen) {
                bottomOverlayInset.value = 0.dp
            }
        }

        AnimatedVisibility(
            visible = !isKeyboardOpen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 64.dp)
                    .onSizeChanged { size ->
                        if (!isKeyboardOpen) {
                            bottomOverlayInset.value = with(density) { size.height.toDp() }
                        }
                    },
                color = NurseryColors.PaperSurface,
                border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Total Payable & Due, tapping scrolls to totals card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                coroutineScope.launch {
                                    totalsBringIntoViewRequester.bringIntoView()
                                }
                            }
                    ) {
                        if (editorState.isOrder) {
                            Text(
                                text = "Total: ${IndianCurrencyFormatter.formatInr(calc.totalPayable, settings.currencySymbol)} • Adv: ${IndianCurrencyFormatter.formatInr(calc.effectivePaid, settings.currencySymbol)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = NurseryColors.DarkInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (calc.totalPayable > 0.009) {
                                Text(
                                    text = "Bal: ${IndianCurrencyFormatter.formatInr(calc.balanceDue, settings.currencySymbol)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (calc.balanceDue > 0.009) NurseryColors.DueRed else NurseryColors.BotanicalGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
                            Text(
                                text = "Total: ${IndianCurrencyFormatter.formatInr(calc.totalPayable, settings.currencySymbol)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = NurseryColors.DarkInk
                            )
                            if (calc.totalPayable > 0.009) {
                                if (calc.balanceDue > 0.009) {
                                    Text(
                                        text = "Due: ${IndianCurrencyFormatter.formatInr(calc.balanceDue, settings.currencySymbol)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = NurseryColors.DueRed
                                    )
                                } else {
                                    Text(
                                        text = "Fully Paid",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = NurseryColors.BotanicalGreen
                                    )
                                }
                            }
                        }
                    }

                    // Right: Save & Primary Share
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onSaveMemo(false) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showShareSheet = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = NurseryColors.InkDark,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = valueColor,
            style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
        )
    }
}

private fun formatCleanNumber(value: Double, blankIfZero: Boolean): String {
    if (value == 0.0 && blankIfZero) return ""
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
