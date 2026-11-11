package com.vishalnursery.cashmemo.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.vishalnursery.cashmemo.domain.model.AiExtractedItem
import com.vishalnursery.cashmemo.domain.model.AiScanPreview
import com.vishalnursery.cashmemo.domain.model.TranslationMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.max

/**
 * Multimodal Gemini AI Vision scanner & translator for handwritten/printed Hindi & English memos.
 */
class GeminiVisionService(private val context: Context) {

    private val gson = Gson()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    data class GeminiExtractionDto(
        @SerializedName("memoNumber") val memoNumber: String? = null,
        @SerializedName("date") val date: String? = null,
        @SerializedName("customerName") val customerName: String? = null,
        @SerializedName("customerMobile") val customerMobile: String? = null,
        @SerializedName("customerAddress") val customerAddress: String? = null,
        @SerializedName("discount") val discount: Double? = null,
        @SerializedName("taxPercentage") val taxPercentage: Double? = null,
        @SerializedName("previousDues") val previousDues: Double? = null,
        @SerializedName("paidAmount") val paidAmount: Double? = null,
        @SerializedName("items") val items: List<GeminiItemDto>? = null,
        @SerializedName("rawTranscription") val rawTranscription: String? = null,
        @SerializedName("warnings") val warnings: String? = null
    )

    data class GeminiItemDto(
        @SerializedName("originalText") val originalText: String? = null,
        @SerializedName("translatedName") val translatedName: String? = null,
        @SerializedName("quantity") val quantity: Double? = null,
        @SerializedName("rate") val rate: Double? = null
    )

    suspend fun listAvailableFlashModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanKey = apiKey.trim()
            require(cleanKey.isNotEmpty()) { "Gemini API key is empty" }
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models")
                .addHeader("x-goog-api-key", cleanKey)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error(extractApiErrorMessage(response.code, body))
                }
                val root = JSONObject(body)
                val modelsArr = root.optJSONArray("models") ?: JSONArray()
                val candidates = mutableListOf<String>()
                for (i in 0 until modelsArr.length()) {
                    val obj = modelsArr.getJSONObject(i)
                    val fullName = obj.optString("name", "") // e.g. "models/gemini-3.5-flash"
                    val shortName = fullName.removePrefix("models/")
                    val methods = obj.optJSONArray("supportedGenerationMethods")
                    val supportsGen = (0 until (methods?.length() ?: 0)).any {
                        methods?.optString(it) == "generateContent"
                    }
                    if (supportsGen &&
                        shortName.contains("flash", ignoreCase = true) &&
                        !shortName.contains("tts", ignoreCase = true) &&
                        !shortName.contains("audio", ignoreCase = true) &&
                        !shortName.contains("image", ignoreCase = true)
                    ) {
                        candidates.add(shortName)
                    }
                }
                sortFlashModels(candidates)
            }
        }
    }

    suspend fun analyzeReceipt(
        uri: Uri,
        apiKey: String,
        preferredModel: String,
        translationMode: TranslationMode
    ): Result<AiScanPreview> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanKey = apiKey.trim()
            require(cleanKey.isNotEmpty()) { "Please add your Gemini API key first." }

            val base64Jpeg = encodeCompressedJpegBase64(uri)
            val prompt = buildExtractionPrompt(translationMode)

            val initialModel = preferredModel.trim().ifBlank { "gemini-3.5-flash" }
            val (rawJsonText, usedModel) = tryModelWithAutoFallback(cleanKey, initialModel, base64Jpeg, prompt)
            val cleanJson = stripCodeFences(rawJsonText)

            val dto = gson.fromJson(cleanJson, GeminiExtractionDto::class.java)
                ?: error("Gemini returned empty structured output.")

            val mappedItems = (dto.items ?: emptyList()).map { item ->
                val orig = item.originalText.orEmpty().trim()
                val trans = item.translatedName.orEmpty().trim().ifEmpty { orig }
                AiExtractedItem(
                    originalText = orig,
                    translatedName = trans,
                    quantity = formatNum(item.quantity ?: 1.0, defaultText = "1"),
                    rate = formatNum(item.rate ?: 0.0, defaultText = "0")
                )
            }

            AiScanPreview(
                memoNumber = dto.memoNumber.orEmpty().trim(),
                date = dto.date.orEmpty().trim(),
                customerName = dto.customerName.orEmpty().trim(),
                customerMobile = dto.customerMobile.orEmpty().trim(),
                customerAddress = dto.customerAddress.orEmpty().trim(),
                discount = dto.discount?.takeIf { it > 0.0 }?.let { formatNum(it) }.orEmpty(),
                taxPercentage = dto.taxPercentage?.takeIf { it > 0.0 }?.let { formatNum(it) }.orEmpty(),
                previousDues = dto.previousDues?.takeIf { it > 0.0 }?.let { formatNum(it) }.orEmpty(),
                paidAmount = dto.paidAmount?.takeIf { it > 0.0 }?.let { formatNum(it) }.orEmpty(),
                items = if (mappedItems.isNotEmpty()) mappedItems else listOf(AiExtractedItem()),
                rawTranscription = dto.rawTranscription.orEmpty().trim(),
                warnings = dto.warnings.orEmpty().trim(),
                modelUsed = usedModel,
                sourceImageUri = uri.toString()
            )
        }
    }

    private fun tryModelWithAutoFallback(
        apiKey: String,
        initialModel: String,
        base64Jpeg: String,
        prompt: String
    ): Pair<String, String> {
        val firstAttempt = callGenerateContent(apiKey, initialModel, base64Jpeg, prompt)
        if (firstAttempt.isSuccess) {
            return firstAttempt.getOrThrow() to initialModel
        }

        val err = firstAttempt.exceptionOrNull()
        val msg = err?.message.orEmpty()
        // If model ID isn't available (404 / NOT_FOUND), query live available Flash models and retry
        if (msg.contains("404") || msg.contains("not found", ignoreCase = true) || msg.contains("NOT_FOUND")) {
            val available = listAvailableFlashModels(apiKey).getOrNull().orEmpty()
            val fallbacks = (available + listOf(
                "gemini-3.5-flash",
                "gemini-3.8-flash",
                "gemini-3.6-flash",
                "gemini-2.5-flash",
                "gemini-2.5-flash-lite",
                "gemini-2.0-flash"
            )).distinct().filter { it != initialModel }

            for (candidate in fallbacks) {
                val retry = callGenerateContent(apiKey, candidate, base64Jpeg, prompt)
                if (retry.isSuccess) {
                    return retry.getOrThrow() to candidate
                }
            }
        }
        throw err ?: IllegalStateException("Gemini API request failed")
    }

    private fun callGenerateContent(
        apiKey: String,
        modelId: String,
        base64Jpeg: String,
        prompt: String
    ): Result<String> = runCatching {
        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Jpeg)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("responseMimeType", "application/json")
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent"
        val request = Request.Builder()
            .url(url)
            .addHeader("x-goog-api-key", apiKey)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error(extractApiErrorMessage(response.code, respBody))
            }
            val root = JSONObject(respBody)
            val candidates = root.optJSONArray("candidates")
                ?: error("Gemini returned no candidates.")
            if (candidates.length() == 0) error("Empty response from Gemini.")
            val parts = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        }
    }

    private fun buildExtractionPrompt(mode: TranslationMode): String {
        val translationRule = when (mode) {
            TranslationMode.ENGLISH_WITH_HINDI ->
                "Translate Hindi/Devanagari or Hinglish plant/item names into clear English followed by the original Hindi script in parentheses, e.g. 'Litchi (लीची)', 'Mango (आम)', 'Marigold (गेंदा)', 'Bay Leaf / Tejpatta (तेजपात)', 'Hibiscus (गुलहर)', 'Banana (केला)'. If already English, keep clean English."
            TranslationMode.ENGLISH_ONLY ->
                "Translate all Hindi/Devanagari or Hinglish plant/item names into clear English, e.g. 'Litchi', 'Mango', 'Marigold', 'Bay Leaf (Tejpatta)', 'Hibiscus', 'Banana'."
            TranslationMode.ORIGINAL ->
                "Keep item names accurately transcribed in their original script (Hindi Devanagari or English) without translating."
        }

        return """
            You are an expert OCR & translation assistant for an Indian plant nursery (Vishal Nursery) reading handwritten notes, paper slips, and cash memos in Hindi (Devanagari), Hinglish, and English.

            Carefully read every line in the image. Watch closely for common handwritten Indian nursery shorthand such as:
            - "5 लीची x 50" -> quantity = 5, item = लीची, rate = 50
            - "10 आम x 150" -> quantity = 10, item = आम, rate = 150
            - "12 गेंदा x 20" -> quantity = 12, item = गेंदा, rate = 20
            - Also handle formats where quantity is after item, or only total amount is written (set quantity = 1, rate = total).

            Translation rule for `translatedName`:
            $translationRule

            Return ONLY valid JSON matching this exact schema (use null or empty string if absent, never invent customer/memo fields not in the image):
            {
              "memoNumber": "",
              "date": "",
              "customerName": "",
              "customerMobile": "",
              "customerAddress": "",
              "discount": 0,
              "taxPercentage": 0,
              "previousDues": 0,
              "paidAmount": null,
              "items": [
                {
                  "originalText": "exact handwritten/printed text for this item",
                  "translatedName": "formatted item name per rule above",
                  "quantity": 1,
                  "rate": 0
                }
              ],
              "rawTranscription": "line-by-line transcription of what is written on the paper",
              "warnings": "mention any ambiguous handwriting or uncertain numbers for user verification, or empty string if clear"
            }
        """.trimIndent()
    }

    private fun encodeCompressedJpegBase64(uri: Uri): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

        val maxDim = 1600
        var sampleSize = 1
        while (bounds.outWidth / sampleSize > maxDim * 2 || bounds.outHeight / sampleSize > maxDim * 2) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: error("Unable to read image from URI")

        val rotationDegrees = context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f

        val oriented = if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it !== decoded) decoded.recycle()
            }
        } else {
            decoded
        }

        val longest = max(oriented.width, oriented.height)
        val finalBitmap = if (longest > maxDim) {
            val scale = maxDim.toFloat() / longest.toFloat()
            val w = (oriented.width * scale).toInt().coerceAtLeast(1)
            val h = (oriented.height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(oriented, w, h, true).also {
                if (it !== oriented) oriented.recycle()
            }
        } else {
            oriented
        }

        val out = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 82, out)
        finalBitmap.recycle()
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun extractApiErrorMessage(code: Int, body: String): String {
        val apiMsg = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return if (!apiMsg.isNullOrBlank()) {
            "Gemini API ($code): $apiMsg"
        } else {
            "Gemini API HTTP $code"
        }
    }

    private fun stripCodeFences(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("```")) {
            return trimmed
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }
        return trimmed
    }

    private fun sortFlashModels(models: List<String>): List<String> {
        val priority = listOf("3.8-flash", "3.6-flash", "3.5-flash", "3-flash", "2.5-flash", "2.0-flash")
        return models.distinct().sortedBy { model ->
            val idx = priority.indexOfFirst { model.contains(it, ignoreCase = true) }
            if (idx == -1) Int.MAX_VALUE else idx
        }
    }

    private fun formatNum(value: Double, defaultText: String = ""): String {
        if (value == 0.0 && defaultText.isNotEmpty()) return defaultText
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
}
