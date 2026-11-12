package com.vishalnursery.cashmemo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CashMemo
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.ui.theme.NurseryColors

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
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<CustomerProfile?>(null) }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }
    var showAddRateDialog by remember { mutableStateOf(false) }
    var settleTarget by remember { mutableStateOf<CustomerProfile?>(null) }

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

    if (showAddRateDialog) {
        AddPlantRateDialog(
            onDismiss = { showAddRateDialog = false },
            onSave = {
                onSavePlantRate(it)
                showAddRateDialog = false
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

    val selectedCustomer = remember(selectedCustomerId, customers) {
        selectedCustomerId?.let { id -> customers.firstOrNull { it.id == id } }
    }
    selectedCustomer?.let { customer ->
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

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Customers (${customers.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Plant Rates (${plantRates.size})") }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    if (selectedTab == 0) showAddCustomerDialog = true else showAddRateDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (selectedTab == 0) "Add Customer Profile" else "Add Plant / Item Rate")
            }

            if (selectedTab == 0) {
                if (customers.isEmpty()) {
                    EmptyLedgerNotice("No customer records yet. Saved cash memos automatically sync here.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(customers, key = { it.id }) { customer ->
                            CustomerCard(
                                customer = customer,
                                currencySymbol = settings.currencySymbol,
                                onOpenProfile = { selectedCustomerId = customer.id },
                                onSettle = { settleTarget = customer },
                                onDelete = { onDeleteCustomer(customer.id) }
                            )
                        }
                    }
                }
            } else {
                if (plantRates.isEmpty()) {
                    EmptyLedgerNotice("No plant rates added yet. Add common plants for quick autocomplete.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(plantRates, key = { it.id }) { rate ->
                            PlantRateCard(
                                rate = rate,
                                currencySymbol = settings.currencySymbol,
                                onDelete = { onDeletePlantRate(rate.id) }
                            )
                        }
                    }
                }
            }
        }
    }
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
                Text(customer.name, fontWeight = FontWeight.Bold)
                if (customer.phoneNumber.isNotBlank()) {
                    Text(customer.phoneNumber, style = MaterialTheme.typography.bodySmall)
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
                    fontWeight = FontWeight.Bold
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
private fun PlantRateCard(
    rate: PlantRate,
    currencySymbol: String,
    onDelete: () -> Unit
) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(rate.name, fontWeight = FontWeight.Bold)
                Text(rate.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "R: ${IndianCurrencyFormatter.formatInr(rate.retailPrice, currencySymbol)}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "W: ${IndianCurrencyFormatter.formatInr(rate.wholesalePrice, currencySymbol)}",
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelSmall,
                        color = NurseryColors.BotanicalGreen
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Rate", tint = NurseryColors.DueRed)
                }
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
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Mobile") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val updated = initial?.copy(
                            name = name.trim(),
                            phoneNumber = phone.trim(),
                            address = address.trim()
                        ) ?: CustomerProfile(
                            name = name.trim(),
                            phoneNumber = phone.trim(),
                            address = address.trim()
                        )
                        onSave(updated)
                    }
                }
            ) { Text("Save") }
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
            modifier = Modifier.fillMaxSize(),
            color = NurseryColors.CreamBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = customer.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = NurseryColors.Maroon
                        )
                        if (customer.phoneNumber.isNotBlank()) {
                            Text(
                                text = "Mob: ${customer.phoneNumber}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (customer.address.isNotBlank()) {
                            Text(
                                text = customer.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row {
                        IconButton(onClick = onEditCustomer) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Customer", tint = NurseryColors.Maroon)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                    border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Billed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                IndianCurrencyFormatter.formatInr(customer.totalPurchased, currencySymbol),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Paid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                IndianCurrencyFormatter.formatInr(customer.totalPaid, currencySymbol),
                                fontWeight = FontWeight.Bold,
                                color = NurseryColors.BotanicalGreen
                            )
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Pending / Carry Forward Due", fontWeight = FontWeight.Bold)
                            Text(
                                text = IndianCurrencyFormatter.formatInr(customer.outstandingBalance, currencySymbol),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (customer.outstandingBalance > 0.009) NurseryColors.DueRed else NurseryColors.BotanicalGreen
                            )
                        }
                        if (customer.outstandingBalance > 0.009) {
                            OutlinedButton(
                                onClick = onSettleDue,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Settle / Record Payment")
                            }
                        }
                    }
                }

                Button(
                    onClick = onCreateNewBill,
                    colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (customer.outstandingBalance > 0.009) {
                            "+ Create New Bill for This Customer (Import Dues)"
                        } else {
                            "+ Create New Bill for This Customer"
                        },
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "BILLING HISTORY (${bills.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = NurseryColors.Maroon
                )

                if (bills.isEmpty()) {
                    EmptyLedgerNotice("No saved bills found for this customer yet.")
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bills, key = { it.id }) { bill ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                                border = BorderStroke(1.dp, NurseryColors.OutlineSoft)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "#${bill.memoNumber}",
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = NurseryColors.Maroon
                                            )
                                            Text(
                                                text = bill.date,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (bill.hasPendingDue) NurseryColors.DueRedContainer else NurseryColors.BotanicalGreenLight
                                            ) {
                                                Text(
                                                    text = if (bill.hasPendingDue) {
                                                        "Due ${IndianCurrencyFormatter.formatInr(bill.balanceDue, currencySymbol)}"
                                                    } else "Paid",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (bill.hasPendingDue) NurseryColors.DueRed else NurseryColors.BotanicalGreen,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = IndianCurrencyFormatter.formatInr(bill.totalPayable, currencySymbol),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    Text(
                                        text = "Paid: ${IndianCurrencyFormatter.formatInr(bill.paidAmount, currencySymbol)}" +
                                            if (bill.previousDues > 0.009) "  •  Prev Dues: ${IndianCurrencyFormatter.formatInr(bill.previousDues, currencySymbol)}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = bill.items.joinToString(", ") { "${it.description} (${it.quantity})" },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { onEditBill(bill) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Edit", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = { onPrintBill(bill) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Print", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = { onShareBillJpeg(bill) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("JPEG", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = { onShareBillPdf(bill) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("PDF", style = MaterialTheme.typography.labelSmall, maxLines = 1)
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

@Composable
private fun AddPlantRateDialog(
    onDismiss: () -> Unit,
    onSave: (PlantRate) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Plants") }
    var retail by remember { mutableStateOf("") }
    var wholesale by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Plant Rate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Plant / Variety Name") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                OutlinedTextField(
                    value = retail,
                    onValueChange = { retail = it },
                    label = { Text("Retail Price (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = wholesale,
                    onValueChange = { wholesale = it },
                    label = { Text("Wholesale Price (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        val rPrice = retail.toDoubleOrNull() ?: 0.0
                        val wPrice = wholesale.toDoubleOrNull() ?: rPrice
                        onSave(PlantRate(name = name.trim(), category = category.trim(), retailPrice = rPrice, wholesalePrice = wPrice))
                    }
                }
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
        title = { Text("Settle Due — ${customer.name}") },
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Balance Note") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) onConfirm(amt, noteText)
                }
            ) { Text("Record Payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
