package com.vishalnursery.cashmemo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vishalnursery.cashmemo.domain.model.AiExtractedItem
import com.vishalnursery.cashmemo.domain.model.AiScanPreview
import com.vishalnursery.cashmemo.domain.model.BusinessSettings
import com.vishalnursery.cashmemo.domain.model.TranslationMode
import com.vishalnursery.cashmemo.domain.util.IndianCurrencyFormatter
import com.vishalnursery.cashmemo.ui.theme.NurseryColors
import com.vishalnursery.cashmemo.ui.viewmodel.ScannerUiState
import java.io.File

@Composable
fun ScannerScreen(
    scannerState: ScannerUiState,
    settings: BusinessSettings,
    onSelectTranslationMode: (TranslationMode) -> Unit,
    onImageCaptured: (Uri) -> Unit,
    onOpenApiKeySetup: () -> Unit,
    onDismissApiKeyPrompt: () -> Unit,
    onSaveGeminiConfig: (String, String) -> Unit,
    onDetectFlashModels: (String?) -> Unit,
    onRunOfflineFallback: (Uri) -> Unit,
    onConfirmAiReview: (AiScanPreview) -> Unit,
    onDismissAiReview: () -> Unit,
    onOpenReview: () -> Unit,
    onNewBlankMemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImageCaptured(uri)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(hasCameraPermission, previewView) {
        val pv = previewView ?: return@LaunchedEffect
        if (!hasCameraPermission) return@LaunchedEffect
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                val cameraProvider = future.get()
                cameraProviderRef = cameraProvider
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = pv.surfaceProvider
                }
                cameraProvider.unbindAll()
                boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                cameraProviderRef?.unbindAll()
            }
        }
    }

    // Modal 1: Configure Gemini Free-Tier API Key & Model
    if (scannerState.showApiKeyPrompt) {
        GeminiApiKeySetupDialog(
            initialApiKey = settings.geminiApiKey,
            initialModel = settings.geminiModel,
            availableFlashModels = scannerState.availableFlashModels,
            isDetectingModels = scannerState.isDetectingModels,
            hasPendingImage = scannerState.pendingUriForKeySetup.isNotBlank(),
            onDetectModels = onDetectFlashModels,
            onSave = onSaveGeminiConfig,
            onRunOfflineFallback = {
                val uriText = scannerState.pendingUriForKeySetup
                if (uriText.isNotBlank()) {
                    onRunOfflineFallback(Uri.parse(uriText))
                } else {
                    onDismissApiKeyPrompt()
                }
            },
            onDismiss = onDismissApiKeyPrompt
        )
    }

    // Modal 2: Quick-Edit Prompt after Gemini reads & translates handwritten memo
    scannerState.pendingAiReview?.let { preview ->
        AiReviewEditDialog(
            preview = preview,
            currencySymbol = settings.currencySymbol,
            onConfirmPaste = onConfirmAiReview,
            onDismiss = onDismissAiReview
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top Gemini Engine Status + New Blank Memo bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val hasKey = settings.geminiApiKey.isNotBlank()
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (hasKey) NurseryColors.BotanicalGreenLight else NurseryColors.MaroonLight,
                border = BorderStroke(
                    1.dp,
                    if (hasKey) NurseryColors.BotanicalGreen.copy(alpha = 0.4f) else NurseryColors.Maroon.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
                    .clickable(onClick = onOpenApiKeySetup)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (hasKey) Icons.Default.AutoAwesome else Icons.Default.Key,
                        contentDescription = null,
                        tint = if (hasKey) NurseryColors.BotanicalGreen else NurseryColors.Maroon,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (hasKey) "Gemini AI • ${settings.geminiModel}" else "Set Free Gemini API Key",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (hasKey) NurseryColors.BotanicalGreen else NurseryColors.Maroon,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            OutlinedButton(
                onClick = onNewBlankMemo,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Blank Memo", maxLines = 1, softWrap = false)
            }
        }

        // Translation Output Mode Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = NurseryColors.Maroon,
                modifier = Modifier.size(16.dp)
            )
            TranslationMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.translationMode == mode,
                    onClick = { onSelectTranslationMode(mode) },
                    label = {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        }

        // CameraX Live Viewfinder Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF141112))
                .border(1.5.dp, NurseryColors.Maroon.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
        ) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Torch button
                IconButton(
                    onClick = {
                        torchEnabled = !torchEnabled
                        boundCamera?.cameraControl?.enableTorch(torchEnabled)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flash",
                        tint = Color.White
                    )
                }

                // Alignment hint banner
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Snap handwritten Hindi/English list or memo",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Camera permission lets you snap handwritten notes or bills for AI extraction.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Camera Permission")
                    }
                }
            }

            if (scannerState.isProcessingOcr) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.68f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = scannerState.processingLabel,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Bottom Camera & Gallery Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                enabled = !scannerState.isProcessingOcr,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Gallery", maxLines = 1, softWrap = false)
            }

            FloatingActionButton(
                onClick = {
                    if (scannerState.isProcessingOcr) return@FloatingActionButton
                    if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                        return@FloatingActionButton
                    }
                    val photoFile = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val savedUri = outputFileResults.savedUri ?: Uri.fromFile(photoFile)
                                onImageCaptured(savedUri)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                // Ignored safely
                            }
                        }
                    )
                },
                containerColor = NurseryColors.Maroon,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capture Cashmemo")
            }

            Button(
                onClick = onOpenReview,
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Review", maxLines = 1, softWrap = false)
            }
        }

        // Scan Error with Quick Recovery Actions
        scannerState.errorMessage?.let { err ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onOpenApiKeySetup) {
                            Text("Configure Key / Model")
                        }
                        if (scannerState.lastImageUri.isNotBlank()) {
                            TextButton(onClick = { onRunOfflineFallback(Uri.parse(scannerState.lastImageUri)) }) {
                                Text("Try Offline ML Kit")
                            }
                        }
                    }
                }
            }
        }

        // Extracted Transcription Preview Card
        if (scannerState.extractedRawText.isNotBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = NurseryColors.PaperSurface),
                border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Last Scan Transcription",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = NurseryColors.Maroon
                        )
                        Button(
                            onClick = onOpenReview,
                            colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.Maroon),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Go to Review", maxLines = 1, softWrap = false)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = scannerState.extractedRawText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 84.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}

@Composable
private fun GeminiApiKeySetupDialog(
    initialApiKey: String,
    initialModel: String,
    availableFlashModels: List<String>,
    isDetectingModels: Boolean,
    hasPendingImage: Boolean,
    onDetectModels: (String?) -> Unit,
    onSave: (String, String) -> Unit,
    onRunOfflineFallback: () -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember(initialApiKey) { mutableStateOf(initialApiKey) }
    var model by remember(initialModel) {
        mutableStateOf(initialModel.ifBlank { "gemini-3.5-flash" })
    }
    var showKey by remember { mutableStateOf(false) }

    val presetModels = remember(availableFlashModels) {
        (availableFlashModels + listOf("gemini-3.5-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite"))
            .distinct()
            .take(5)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NurseryColors.Maroon)
                Text("Gemini Free-Tier AI Scan", fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Paste your free Google AI Studio API key to read handwritten Hindi/English nursery slips accurately. Saved only on your phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Gemini API Key (AIza...)", maxLines = 1) },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showKey) "Hide key" else "Show key"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Gemini Flash Model", maxLines = 1) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Select / Check Models",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { onDetectModels(apiKey) },
                        enabled = !isDetectingModels && apiKey.isNotBlank()
                    ) {
                        if (isDetectingModels) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Check Key", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetModels.forEach { preset ->
                        FilterChip(
                            selected = model.trim() == preset,
                            onClick = { model = preset },
                            label = { Text(preset, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(apiKey, model) },
                enabled = apiKey.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen)
            ) {
                Text(if (hasPendingImage) "Save & Scan Now" else "Save Config")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (hasPendingImage) {
                    TextButton(onClick = onRunOfflineFallback) {
                        Text("Offline OCR")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun AiReviewEditDialog(
    preview: AiScanPreview,
    currencySymbol: String,
    onConfirmPaste: (AiScanPreview) -> Unit,
    onDismiss: () -> Unit
) {
    var items by remember(preview) {
        mutableStateOf(
            preview.items.ifEmpty {
                listOf(AiExtractedItem(translatedName = "", quantity = "1", rate = "0"))
            }
        )
    }
    var customerName by remember(preview) { mutableStateOf(preview.customerName) }
    var customerMobile by remember(preview) { mutableStateOf(preview.customerMobile) }
    var customerAddress by remember(preview) { mutableStateOf(preview.customerAddress) }
    var discount by remember(preview) { mutableStateOf(preview.discount) }
    var paidAmount by remember(preview) { mutableStateOf(preview.paidAmount) }
    var showRawText by remember { mutableStateOf(false) }
    var showCustomerExtras by remember {
        mutableStateOf(
            preview.customerName.isNotBlank() ||
                preview.customerMobile.isNotBlank() ||
                preview.customerAddress.isNotBlank() ||
                preview.discount.isNotBlank() ||
                preview.paidAmount.isNotBlank()
        )
    }

    val estimatedSubtotal = items.sumOf { item ->
        (item.quantity.toDoubleOrNull() ?: 0.0) * (item.rate.toDoubleOrNull() ?: 0.0)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = NurseryColors.CreamBackground,
            border = BorderStroke(1.dp, NurseryColors.Maroon.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 20.dp)
                .heightIn(max = 680.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Surface(color = NurseryColors.Maroon) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Verify & Edit AI Scan",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "${preview.modelUsed} • ${items.size} item(s) • Est. ${
                                    IndianCurrencyFormatter.formatInr(estimatedSubtotal, currencySymbol, false)
                                }",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Scrollable quick-edit fields
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (preview.warnings.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NurseryColors.GoldAccent.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, NurseryColors.GoldAccent)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = NurseryColors.Maroon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = preview.warnings,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NurseryColors.InkDark
                                )
                            }
                        }
                    }

                    // Collapsible Raw Transcription Comparison
                    if (preview.rawTranscription.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NurseryColors.BotanicalGreenLight,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showRawText = !showRawText }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Original Note Read by AI (Tap to compare)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NurseryColors.BotanicalGreen
                                    )
                                    Icon(
                                        imageVector = if (showRawText) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = NurseryColors.BotanicalGreen
                                    )
                                }
                                AnimatedVisibility(visible = showRawText) {
                                    Text(
                                        text = preview.rawTranscription,
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Editable Items
                    items.forEachIndexed { index, item ->
                        key(item.id) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NurseryColors.PaperSurface,
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
                                            text = buildString {
                                                append("Row #${index + 1}")
                                                if (item.originalText.isNotBlank() &&
                                                    !item.originalText.equals(item.translatedName, ignoreCase = true)
                                                ) {
                                                    append(" • Note: ${item.originalText}")
                                                }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NurseryColors.Maroon,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        val rowTotal = (item.quantity.toDoubleOrNull() ?: 0.0) *
                                            (item.rate.toDoubleOrNull() ?: 0.0)
                                        Text(
                                            text = IndianCurrencyFormatter.formatInr(rowTotal, currencySymbol, false),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(
                                            onClick = {
                                                items = if (items.size <= 1) {
                                                    listOf(AiExtractedItem(translatedName = "", quantity = "1", rate = "0"))
                                                } else {
                                                    items.toMutableList().apply { removeAt(index) }
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove Item",
                                                tint = NurseryColors.DueRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = item.translatedName,
                                        onValueChange = { updatedName ->
                                            items = items.mapIndexed { i, existing ->
                                                if (i == index) existing.copy(translatedName = updatedName) else existing
                                            }
                                        },
                                        label = { Text("Plant / Item Name", maxLines = 1) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = item.quantity,
                                            onValueChange = { updatedQty ->
                                                items = items.mapIndexed { i, existing ->
                                                    if (i == index) existing.copy(quantity = updatedQty) else existing
                                                }
                                            },
                                            label = { Text("Qty", maxLines = 1) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = item.rate,
                                            onValueChange = { updatedRate ->
                                                items = items.mapIndexed { i, existing ->
                                                    if (i == index) existing.copy(rate = updatedRate) else existing
                                                }
                                            },
                                            label = { Text("Rate ($currencySymbol)", maxLines = 1) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add missing row directly at bottom of AI verify list
                    OutlinedButton(
                        onClick = {
                            items = items + AiExtractedItem(translatedName = "", quantity = "1", rate = "0")
                        },
                        border = BorderStroke(1.dp, NurseryColors.Maroon.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Missing Item", fontWeight = FontWeight.Bold)
                    }

                    // Customer & Settlement Optional Fields
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NurseryColors.PaperSurface,
                        border = BorderStroke(1.dp, NurseryColors.OutlineSoft),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showCustomerExtras = !showCustomerExtras },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Customer & Settlement Details (Optional)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NurseryColors.Maroon
                                )
                                Icon(
                                    imageVector = if (showCustomerExtras) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                            AnimatedVisibility(visible = showCustomerExtras) {
                                Column(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customerName,
                                        onValueChange = { customerName = it },
                                        label = { Text("Customer / Nursery Name", maxLines = 1) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = customerMobile,
                                            onValueChange = { customerMobile = it },
                                            label = { Text("Mobile", maxLines = 1) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = customerAddress,
                                            onValueChange = { customerAddress = it },
                                            label = { Text("Address / Village", maxLines = 1) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = discount,
                                            onValueChange = { discount = it },
                                            label = { Text("Discount", maxLines = 1) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = paidAmount,
                                            onValueChange = { paidAmount = it },
                                            label = { Text("Paid", maxLines = 1) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Sticky Confirm / Discard Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Discard")
                    }
                    Button(
                        onClick = {
                            onConfirmPaste(
                                preview.copy(
                                    customerName = customerName,
                                    customerMobile = customerMobile,
                                    customerAddress = customerAddress,
                                    discount = discount,
                                    paidAmount = paidAmount,
                                    items = items
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NurseryColors.BotanicalGreen)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Paste into Bill", maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
