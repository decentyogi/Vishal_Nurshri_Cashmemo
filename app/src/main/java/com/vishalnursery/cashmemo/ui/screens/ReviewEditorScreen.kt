package com.vishalnursery.cashmemo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vishalnursery.cashmemo.domain.model.BillExportLayout
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.DiscountType
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.util.CustomerMatcher
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.ui.components.UpiQrDialog
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import com.vishalnursery.cashmemo.ui.viewmodel.EditorUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReviewEditorScreen(
    editorState: EditorUiState,
    settings: BusinessSettings,
    customers: List<CustomerProfile>,
    plantRates: List<PlantRate>,
    onUpdateMemoNumber: (String) -> Unit,
    onUpdateDate: (String) -> Unit,
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

    var showRawOcr by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Active expanded row tracker (only one row expanded at a time on large bills)
    var expandedRowIndex by remember { mutableStateOf<Int?>(0) }

    // UPI QR Dialog: amount defaults to remaining balance due (or paid/total for fully paid)
    if (showQrDialog) {
        val qrAmount = IndianCurrencyFormatter.resolveBillQrAmount(calc.effectivePaid, calc.totalPayable, calc.balanceDue)
        UpiQrDialog(
            initialAmount = qrAmount,
            settings = settings,
            billLabel = "Memo #${editorState.memoNumber}",
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
                        text = "Share & Print Memo #${editorState.memoNumber}",
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
        // --- 1. PINNED TOP HEADER (<= 56dp, one compact row) ---
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
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (editorState.editingMemoId == null) "NEW CASH MEMO - #${editorState.memoNumber}" else "EDIT CASH MEMO - #${editorState.memoNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = NurseryColors.Maroon,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (rememberedTier != null && rememberedTier != editorState.isWholesaleMode) {
                        val tierLabel = if (rememberedTier) "Wholesale" else "Retail"
                        Text(
                            text = "Last bill: $tierLabel - switch",
                            style = MaterialTheme.typography.labelSmall,
                            color = NurseryColors.BotanicalGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onToggleWholesale(rememberedTier) }
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Large Retail | Wholesale Segmented Control
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NurseryColors.CreamBackground,
                    border = BorderStroke(1.2.dp, NurseryColors.Maroon),
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (!editorState.isWholesaleMode) NurseryColors.Maroon else Color.Transparent,
                            modifier = Modifier
                                .clickable { if (editorState.isWholesaleMode) onToggleWholesale(false) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Retail",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (!editorState.isWholesaleMode) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (!editorState.isWholesaleMode) Color.White else NurseryColors.Maroon
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (editorState.isWholesaleMode) NurseryColors.Maroon else Color.Transparent,
                            modifier = Modifier
                                .clickable { if (!editorState.isWholesaleMode) onToggleWholesale(true) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Wholesale",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (editorState.isWholesaleMode) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (editorState.isWholesaleMode) Color.White else NurseryColors.Maroon
                            )
                        }
                    }
                }
            }
        }

        // --- 2. SCROLLABLE CONTENT ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editorState.memoNumber,
                        onValueChange = onUpdateMemoNumber,
                        label = { Text("Memo #", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editorState.date,
                        onValueChange = onUpdateDate,
                        label = { Text("Date (DD/MM/YYYY)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }

            // Customer / Party Section
            Card(
                colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "CUSTOMER / PARTY DETAILS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = NurseryColors.Maroon
                    )

                    // Customer / Nursery Name with Trailing "+ Add" / "Ledger" Pill
                    OutlinedTextField(
                        value = editorState.customerName,
                        onValueChange = onUpdateCustomerName,
                        label = { Text("Customer / Nursery Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        trailingIcon = {
                            if (isCustomerInLedger) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = NurseryColors.BotanicalGreenLight,
                                    border = BorderStroke(1.dp, NurseryColors.BotanicalGreen),
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .height(30.dp)
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
                                            modifier = Modifier.size(14.dp)
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
                                        .height(32.dp)
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
                                            modifier = Modifier.size(14.dp)
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
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
                        OutlinedTextField(
                            value = editorState.customerAddress,
                            onValueChange = onUpdateCustomerAddress,
                            label = { Text("Address / Village", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
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
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            color = NurseryColors.Maroon
                        )
                        Text(
                            text = IndianCurrencyFormatter.formatInr(calc.subtotal, settings.currencySymbol),
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = NurseryColors.BotanicalGreen
                        )
                    }

                    editorState.items.forEachIndexed { index, item ->
                        val isExpanded = (expandedRowIndex == index)

                        if (!isExpanded) {
                            // Inactive row collapsed to single compact line
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NurseryColors.CreamBackground,
                                border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandedRowIndex = index }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Clone Row",
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveItemRow(index) },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Row",
                                            tint = NurseryColors.DueRed,
                                            modifier = Modifier.size(15.dp)
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
                                    modifier = Modifier.padding(10.dp),
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
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "Clone Row",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { onRemoveItemRow(index) },
                                                modifier = Modifier.size(30.dp)
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

                                    OutlinedTextField(
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
                                                                        color = NurseryColors.InkLight
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
                                                                        retailPrice = item.rate,
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

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
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
                                        OutlinedTextField(
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
                                                        expandedRowIndex = index + 1
                                                    } else {
                                                        onAddItemRow()
                                                        expandedRowIndex = index + 1
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
                                        val savedTierPrice = if (editorState.isWholesaleMode) {
                                            if (linkedPlant.wholesalePrice > 0.0) linkedPlant.wholesalePrice else linkedPlant.retailPrice
                                        } else linkedPlant.retailPrice

                                        val isCustom = Math.abs(item.rate - savedTierPrice) > 0.01

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp, start = 4.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            if (!isCustom) {
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

                    // Add row button: expands new row
                    OutlinedButton(
                        onClick = {
                            val newIdx = editorState.items.size
                            onAddItemRow()
                            expandedRowIndex = newIdx
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
                colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "TOTALS & SETTLEMENT",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = NurseryColors.Maroon
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
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
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (editorState.discountType == DiscountType.AMOUNT) NurseryColors.BotanicalGreen else NurseryColors.InkDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (editorState.discountType == DiscountType.PERCENT) NurseryColors.BotanicalGreenLight else NurseryColors.CreamBackground,
                            border = BorderStroke(1.dp, if (editorState.discountType == DiscountType.PERCENT) NurseryColors.BotanicalGreen else NurseryColors.OutlineSoft),
                            modifier = Modifier
                                .clickable { onUpdateDiscountType(DiscountType.PERCENT) }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (editorState.discountType == DiscountType.PERCENT) NurseryColors.BotanicalGreen else NurseryColors.InkDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        OutlinedTextField(
                            value = editorState.taxPercentageText,
                            onValueChange = onUpdateTaxPercentage,
                            label = { Text("Tax %", maxLines = 1) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editorState.previousDuesText,
                            onValueChange = onUpdatePreviousDues,
                            label = { Text("Previous Dues", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = editorState.paidAmountText,
                            onValueChange = onUpdatePaidAmount,
                            label = { Text("Paid (blank = full)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash", "Online", "Credit").forEach { method ->
                            FilterChip(
                                selected = editorState.paymentMethod.equals(method, ignoreCase = true),
                                onClick = { onUpdatePaymentMethod(method) },
                                label = { Text(method, style = MaterialTheme.typography.labelMedium) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = editorState.paymentStatusNote,
                        onValueChange = onUpdatePaymentNote,
                        label = { Text("Settlement Note (e.g. Carry Forward, Paid Online)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
                        if (calc.discountAmount > 0.009) {
                            SummaryRow("Discount", "- ${IndianCurrencyFormatter.formatInr(calc.discountAmount, settings.currencySymbol)}", valueColor = NurseryColors.Maroon)
                        }
                        if (calc.taxAmount > 0.009) {
                            SummaryRow("Tax", "+ ${IndianCurrencyFormatter.formatInr(calc.taxAmount, settings.currencySymbol)}")
                        }
                        if (calc.previousDues > 0.009) {
                            SummaryRow("Previous Dues", "+ ${IndianCurrencyFormatter.formatInr(calc.previousDues, settings.currencySymbol)}", valueColor = NurseryColors.DueRed)
                        }
                        SummaryRow("Total Payable", IndianCurrencyFormatter.formatInr(calc.totalPayable, settings.currencySymbol), isBold = true)
                        SummaryRow("Paid Amount", IndianCurrencyFormatter.formatInr(calc.effectivePaid, settings.currencySymbol))
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
        val isKeyboardOpen = WindowInsets.isImeVisible
        AnimatedVisibility(
            visible = !isKeyboardOpen,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 64.dp),
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
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            }
                    ) {
                        Text(
                            text = "Total: ${IndianCurrencyFormatter.formatInr(calc.totalPayable, settings.currencySymbol)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = NurseryColors.DarkInk
                        )
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
