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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    onUpdateDiscount: (String) -> Unit,
    onUpdateDiscountType: (DiscountType) -> Unit,
    onUpdateTaxPercentage: (String) -> Unit,
    onUpdatePreviousDues: (String) -> Unit,
    onUpdatePaidAmount: (String) -> Unit,
    onUpdatePaymentMethod: (String) -> Unit,
    onUpdatePaymentNote: (String) -> Unit,
    onSaveMemo: (Boolean) -> Unit,
    onNewMemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val calc = editorState.calculation
    var showRawOcr by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    if (showQrDialog) {
        UpiQrDialog(
            amount = calc.totalPayable,
            settings = settings,
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
                    LaunchedEffect(item.id, item.quantity) {
                        if (item.quantity != (qtyInput.toDoubleOrNull() ?: 0.0)) {
                            qtyInput = formatCleanNumber(item.quantity, blankIfZero = false)
                        }
                    }
                    LaunchedEffect(item.id, item.rate) {
                        if (item.rate != (rateInput.toDoubleOrNull() ?: 0.0) && item.rate > 0.0) {
                            rateInput = formatCleanNumber(item.rate, blankIfZero = true)
                        }
                    }

                    val rateMatches = remember(item.description, plantRates) {
                        val q = item.description.trim().lowercase()
                        if (q.isEmpty()) emptyList()
                        else plantRates.filter { it.name.lowercase().contains(q) }.take(4)
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
                                onValueChange = { onUpdateItemDescription(index, it) },
                                label = { Text("Item / Plant Description", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (rateMatches.isNotEmpty() && item.rate == 0.0) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    rateMatches.forEach { matched ->
                                        val tierPrice = if (editorState.isWholesaleMode) {
                                            matched.wholesalePrice
                                        } else {
                                            matched.retailPrice
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSelectPlantRate(index, matched) }
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(matched.name, style = MaterialTheme.typography.bodySmall)
                                            Text(
                                                text = IndianCurrencyFormatter.formatInr(tierPrice, settings.currencySymbol),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = NurseryColors.Maroon
                                            )
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
                                    modifier = Modifier.weight(1f)
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

        // 5. Action Buttons Row
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
