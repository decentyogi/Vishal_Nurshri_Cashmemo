package com.vishalnursery.cashmemo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.CustomerProfile
import com.vishalnursery.cashmemo.domain.model.PlantRate
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.ui.theme.NurseryColors

@Composable
fun CatalogLedgerScreen(
    customers: List<CustomerProfile>,
    plantRates: List<PlantRate>,
    settings: BusinessSettings,
    onSaveCustomer: (CustomerProfile) -> Unit,
    onSettleCustomerDue: (String, Double, String) -> Unit,
    onDeleteCustomer: (String) -> Unit,
    onSavePlantRate: (PlantRate) -> Unit,
    onDeletePlantRate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddRateDialog by remember { mutableStateOf(false) }
    var settleTarget by remember { mutableStateOf<CustomerProfile?>(null) }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onSave = {
                onSaveCustomer(it)
                showAddCustomerDialog = false
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
    onSettle: () -> Unit,
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
            Row {
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
private fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (CustomerProfile) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Customer") },
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
                        onSave(CustomerProfile(name = name.trim(), phoneNumber = phone.trim(), address = address.trim()))
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
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
