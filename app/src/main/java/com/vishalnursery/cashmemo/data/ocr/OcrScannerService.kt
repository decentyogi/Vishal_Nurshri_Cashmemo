package com.vishalnursery.cashmemo.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.vishalnursery.cashmemo.domain.model.OcrScriptMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Runs on-device ML Kit text recognition for camera captures and gallery receipts.
 */
class OcrScannerService(private val context: Context) {

    private val latinRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val devanagariRecognizer by lazy {
        TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    }

    suspend fun recognizeTextFromUri(
        uri: Uri,
        scriptMode: OcrScriptMode
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val inputImage = InputImage.fromFilePath(context, uri)
            val recognizer = when (scriptMode) {
                OcrScriptMode.HINDI_AND_ENGLISH -> devanagariRecognizer
                OcrScriptMode.ENGLISH_ONLY -> latinRecognizer
            }
            val visionText = recognizer.process(inputImage).await()
            visionText.text.trim()
        }
    }
}
