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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vishalnursery.cashmemo.domain.model.BillExportLayout
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.DiscountType
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.ui.components.UpiQrDialog
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import com.vishalnursery.cashmemo.ui.viewmodel.EditorUiState

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
    var showRawOcr by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    if (showQrDialog) {
        val explicitPaid = editorState.paidAmountText.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
        val qrAmount = explicitPaid ?: calc.totalPayable.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        UpiQrDialog(
            initialAmount = qrAmount,
            settings = settings,
            billLabel = "Memo #${editorState.memoNumber}",
            initialTab = 0,
            onSavePermanentQr = onSavePermanentQr,
            onDismiss = { showQrDialog = false }
        )
    }

    val customerMatches = remember(editorState.customerName, customers) {
        val q = editorState.customerName.trim().lowercase()
        if (q.length < 2) emptyList()
        else customers.filter {
            it.name.lowercase().contains(q) || it.phoneNumber.contains(q)
        }.take(4)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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

        // 1. Invoice Metadata & Pricing Tier
        Card(
            colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
            border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editorState.editingMemoId == null) "NEW CASH MEMO" else "EDIT CASH MEMO",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = NurseryColors.Maroon
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Retail", style = MaterialTheme.typography.labelSmall)
                        Switch(
                            checked = editorState.isWholesaleMode,
                            onCheckedChange = onToggleWholesale,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        Text(
                            "Wholesale",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        // 2. Customer / Party Section (Centered text alignment matching Cashmemo_AI_Web)
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

                OutlinedTextField(
                    value = editorState.customerName,
                    onValueChange = onUpdateCustomerName,
                    label = { Text("Customer / Nursery Name", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth()
                )

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
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editorState.customerAddress,
                        onValueChange = onUpdateCustomerAddress,
                        label = { Text("Address / Village", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }

        // 3. Line Items Section
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
                    var qtyInput by remember(item.id) {
                        mutableStateOf(formatCleanNumber(item.quantity, blankIfZero = false))
                    }
                    var rateInput by remember(item.id) {
                        mutableStateOf(formatCleanNumber(item.rate, blankIfZero = true))
                    }
                    val qtyFocusRequester = remember { FocusRequester() }
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
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    ) {
                                                        Text(
                                                            text = matched.name,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = NurseryColors.InkDark,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        if (matched.category.isNotBlank()) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = NurseryColors.MaroonLight.copy(alpha = 0.5f)
                                                            ) {
                                                                Text(
                                                                    text = matched.category,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = NurseryColors.Maroon,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Spacer(Modifier.width(8.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text(
                                                            text = "R ${IndianCurrencyFormatter.formatInr(matched.retailPrice, settings.currencySymbol)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (!isWholesale) FontWeight.ExtraBold else FontWeight.Normal,
                                                            color = if (!isWholesale) NurseryColors.Maroon else NurseryColors.InkMedium
                                                        )
                                                        Text("|", style = MaterialTheme.typography.labelSmall, color = NurseryColors.OutlineSoft)
                                                        Text(
                                                            text = "W ${IndianCurrencyFormatter.formatInr(matched.wholesalePrice, settings.currencySymbol)}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = if (isWholesale) FontWeight.ExtraBold else FontWeight.Normal,
                                                            color = if (isWholesale) NurseryColors.Maroon else NurseryColors.InkMedium
                                                        )
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
                                                                retailPrice = if (!editorState.isWholesaleMode) item.rate else item.rate,
                                                                wholesalePrice = if (editorState.isWholesaleMode) item.rate else 0.0,
                                                                category = "General"
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
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1.3f)
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

                // Placed at the bottom of the item list so adding new plants never requires scrolling back up
                OutlinedButton(
                    onClick = onAddItemRow,
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

        // 4. Adjustments, Payment & Totals Card
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

                // Discount row + ₹ / % toggle on its own uncrowded row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = editorState.discountText,
                        onValueChange = onUpdateDiscount,
                        label = { Text("Discount", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    DiscountType.entries.forEach { dType ->
                        FilterChip(
                            selected = editorState.discountType == dType,
                            onClick = { onUpdateDiscountType(dType) },
                            label = {
                                Text(
                                    text = dType.label,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }

                // Equal 50/50 row with matching 1-line labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editorState.taxPercentageText,
                        onValueChange = onUpdateTaxPercentage,
                        label = { Text("Tax %", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editorState.previousDuesText,
                        onValueChange = onUpdatePreviousDues,
                        label = {
                            Text(
                                text = "Prev. Dues (${settings.currencySymbol})",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Paid Amount with clean 1-line label & helpful placeholder
                OutlinedTextField(
                    value = editorState.paidAmountText,
                    onValueChange = onUpdatePaidAmount,
                    label = {
                        Text(
                            text = "Paid Amount (${settings.currencySymbol})",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Leave blank if full amount paid",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Cash", "UPI", "Bank", "Card").forEach { method ->
                        FilterChip(
                            selected = editorState.paymentMethod == method,
                            onClick = { onUpdatePaymentMethod(method) },
                            label = {
                                Text(
                                    text = method,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }

                if (calc.balanceDue > 0.009) {
                    OutlinedTextField(
                        value = editorState.paymentStatusNote,
                        onValueChange = onUpdatePaymentNote,
                        label = { Text("Settlement Note") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()

                SummaryRow("Subtotal", IndianCurrencyFormatter.formatInr(calc.subtotal, settings.currencySymbol))
                if (calc.overallDiscountAmount > 0.0) {
                    SummaryRow(
                        "Discount",
                        "- ${IndianCurrencyFormatter.formatInr(calc.overallDiscountAmount, settings.currencySymbol)}",
                        NurseryColors.BotanicalGreen
                    )
                }
                if (calc.taxAmount > 0.0) {
                    SummaryRow(
                        "Tax (${calc.taxRate}%)",
                        IndianCurrencyFormatter.formatInr(calc.taxAmount, settings.currencySymbol)
                    )
                }
                if (calc.previousDues > 0.0) {
                    SummaryRow(
                        "Previous Dues",
                        "+ ${IndianCurrencyFormatter.formatInr(calc.previousDues, settings.currencySymbol)}",
                        NurseryColors.DueRed
                    )
                }
                SummaryRow(
                    "Total Payable",
                    IndianCurrencyFormatter.formatInr(calc.totalPayable, settings.currencySymbol),
                    NurseryColors.Maroon,
                    isBold = true
                )
                SummaryRow(
                    "Paid (${editorState.paymentMethod})",
                    IndianCurrencyFormatter.formatInr(calc.effectivePaid, settings.currencySymbol)
                )
                if (calc.balanceDue > 0.009) {
                    SummaryRow(
                        "Carry Forward",
                        IndianCurrencyFormatter.formatInr(calc.balanceDue, settings.currencySymbol),
                        NurseryColors.DueRed,
                        isBold = true
                    )
                }

                Text(
                    text = "In Words: ${IndianCurrencyFormatter.convertToIndianRupees(calc.totalPayable)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 5. Bill Export / Print Layout Quick Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
            border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BILL EXPORT / PRINT LAYOUT",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = NurseryColors.Maroon
                    )
                    if (settings.billExportLayout == BillExportLayout.CLASSIC_NURSERY) {
                        val validCount = editorState.items.count { it.description.isNotBlank() || it.amount > 0.0 }
                            .coerceAtLeast(1)
                        val pages = ((validCount - 1) / 15) + 1
                        Text(
                            text = "15/pg • $pages ${if (pages == 1) "Page" else "Pages"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = NurseryColors.BotanicalGreen
                        )
                    }
                }
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
        }

        // 6. Action Buttons Row (UPI QR, Save, Print)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { showQrDialog = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("UPI QR", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }

            Button(
                onClick = { onSaveMemo(false) },
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier.weight(1.1f)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save", maxLines = 1, softWrap = false)
            }

            Button(
                onClick = { onSaveMemo(true) },
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                modifier = Modifier.weight(1.1f)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Print", maxLines = 1, softWrap = false)
            }
        }

        // 7. High-Res Share Pills (Send As JPEG & Send As PDF)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onShareAsJpeg,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.3.dp, NurseryColors.BotanicalGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
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
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Button(
                onClick = onShareAsPdf,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.MaroonDark),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Send As PDF",
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
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
