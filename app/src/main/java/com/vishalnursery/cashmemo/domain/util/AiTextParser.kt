package com.vishalnursery.cashmemo.domain.util

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.vishalnursery.cashmemo.domain.model.AiExtractedItem
import com.vishalnursery.cashmemo.domain.model.AiScanPreview
import java.util.UUID

data class ParsedRateRow(
    val id: String = "pr_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}",
    val name: String,
    val category: String = "Plants",
    val retailPrice: Double = 0.0,
    val wholesalePrice: Double = 0.0,
    val isValid: Boolean = true,
    val statusNote: String = ""
)

object AiTextParser {

    private val gson = Gson()

    private val devanagariDigits = mapOf(
        '०' to '0', '१' to '1', '२' to '2', '३' to '3', '४' to '4',
        '५' to '5', '६' to '6', '७' to '7', '८' to '8', '९' to '9'
    )

    fun normalizeDigits(input: String): String {
        val sb = java.lang.StringBuilder(input.length)
        for (ch in input) {
            sb.append(devanagariDigits[ch] ?: ch)
        }
        return sb.toString()
    }

    fun cleanNumber(raw: String): String {
        val normalized = normalizeDigits(raw)
            .replace("₹", "")
            .replace("Rs.", "", ignoreCase = true)
            .replace("Rs", "", ignoreCase = true)
            .replace("INR", "", ignoreCase = true)
            .replace("/-", "")
            .replace(",", "")
            .trim()
        if (normalized == "?" || normalized == "-" || normalized.isBlank()) return ""
        val digits = normalized.filter { it.isDigit() || it == '.' }
        return digits
    }

    private data class JsonExtraction(
        @SerializedName("memoNumber") val memoNumber: String? = null,
        @SerializedName("date") val date: String? = null,
        @SerializedName("customerName") val customerName: String? = null,
        @SerializedName("customerMobile") val customerMobile: String? = null,
        @SerializedName("customerAddress") val customerAddress: String? = null,
        @SerializedName("discount") val discount: Double? = null,
        @SerializedName("taxPercentage") val taxPercentage: Double? = null,
        @SerializedName("previousDues") val previousDues: Double? = null,
        @SerializedName("paidAmount") val paidAmount: Double? = null,
        @SerializedName("items") val items: List<JsonItem>? = null,
        @SerializedName("rawTranscription") val rawTranscription: String? = null,
        @SerializedName("warnings") val warnings: String? = null
    )

    private data class JsonItem(
        @SerializedName("originalText") val originalText: String? = null,
        @SerializedName("translatedName") val translatedName: String? = null,
        @SerializedName("quantity") val quantity: Double? = null,
        @SerializedName("rate") val rate: Double? = null
    )

    fun parseMemoText(text: String, imageUri: String? = null): AiScanPreview {
        val trimmed = text.trim()
        val stripped = stripCodeFences(trimmed)

        // Try JSON parsing first if it looks like JSON
        if (stripped.startsWith("{") && stripped.endsWith("}")) {
            runCatching {
                val parsed = gson.fromJson(stripped, JsonExtraction::class.java)
                if (parsed != null && (parsed.items?.isNotEmpty() == true || !parsed.customerName.isNullOrBlank())) {
                    val mappedItems = (parsed.items ?: emptyList()).map { item ->
                        val orig = item.originalText.orEmpty().trim()
                        val trans = item.translatedName.orEmpty().trim().ifEmpty { orig }
                        val q = item.quantity?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "1"
                        val r = item.rate?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "0"
                        AiExtractedItem(
                            originalText = orig,
                            translatedName = trans,
                            quantity = q,
                            rate = r
                        )
                    }
                    return AiScanPreview(
                        memoNumber = parsed.memoNumber.orEmpty().trim(),
                        date = parsed.date.orEmpty().trim(),
                        customerName = parsed.customerName.orEmpty().trim(),
                        customerMobile = parsed.customerMobile.orEmpty().trim(),
                        customerAddress = parsed.customerAddress.orEmpty().trim(),
                        discount = parsed.discount?.takeIf { it > 0.0 }?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty(),
                        taxPercentage = parsed.taxPercentage?.takeIf { it > 0.0 }?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty(),
                        previousDues = parsed.previousDues?.takeIf { it > 0.0 }?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty(),
                        paidAmount = parsed.paidAmount?.takeIf { it > 0.0 }?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }.orEmpty(),
                        items = if (mappedItems.isNotEmpty()) mappedItems else listOf(AiExtractedItem()),
                        rawTranscription = text,
                        warnings = parsed.warnings.orEmpty().trim(),
                        modelUsed = "Pasted AI text",
                        sourceImageUri = imageUri.orEmpty()
                    )
                }
            }
        }

        // Line-by-line parsing
        val lines = stripped.lines().map { it.trim() }

        var memoNumber = ""
        var date = ""
        var customerName = ""
        var customerMobile = ""
        var customerAddress = ""
        var discount = ""
        var previousDues = ""
        var paidAmount = ""
        var inItemsSection = false

        val items = mutableListOf<AiExtractedItem>()
        var unparsedLineCount = 0

        for (rawLine in lines) {
            val line = normalizeDigits(rawLine)
            if (line.isBlank()) continue

            // Check header divider
            if (line.startsWith("---") || line.startsWith("===") || line.startsWith("___")) {
                inItemsSection = true
                continue
            }

            if (!inItemsSection) {
                val lower = line.lowercase()
                val colonIdx = line.indexOf(':')
                if (colonIdx != -1) {
                    val key = lower.substring(0, colonIdx).trim()
                    val value = line.substring(colonIdx + 1).trim()
                    when {
                        key.contains("customer") || key.contains("name") || key == "नाम" -> {
                            customerName = value.takeUnless { it == "?" || it == "-" }.orEmpty()
                            continue
                        }
                        key.contains("mobile") || key.contains("phone") || key.contains("मो०") || key.contains("मो") -> {
                            customerMobile = cleanNumber(value)
                            continue
                        }
                        key.contains("address") || key.contains("village") || key == "पता" -> {
                            customerAddress = value.takeUnless { it == "?" || it == "-" }.orEmpty()
                            continue
                        }
                        key.contains("date") || key.contains("दिनांक") -> {
                            date = value.takeUnless { it == "?" || it == "-" }.orEmpty()
                            continue
                        }
                        key.contains("memo") || key.contains("bill") || key.contains("नं०") -> {
                            memoNumber = cleanNumber(value)
                            continue
                        }
                        key.contains("discount") || key.contains("छूट") -> {
                            discount = cleanNumber(value)
                            continue
                        }
                        key.contains("previous") || key.contains("dues") || key.contains("बकाया") -> {
                            previousDues = cleanNumber(value)
                            continue
                        }
                        key.contains("paid") || key.contains("जमा") -> {
                            paidAmount = cleanNumber(value)
                            continue
                        }
                    }
                }
            }

            // If we are here, treat line as item candidate
            val parsedItem = parseItemLine(line)
            if (parsedItem != null) {
                items.add(parsedItem)
            } else {
                // If line contains digits, keep as item so nothing is lost!
                if (line.any { it.isDigit() }) {
                    unparsedLineCount++
                    items.add(
                        AiExtractedItem(
                            originalText = line,
                            translatedName = line,
                            quantity = "1",
                            rate = "0"
                        )
                    )
                }
                // Pure chatter without digits is ignored
            }
        }

        val warnings = if (unparsedLineCount > 0) "$unparsedLineCount line(s) need manual checking" else ""

        return AiScanPreview(
            memoNumber = memoNumber,
            date = date,
            customerName = customerName,
            customerMobile = customerMobile,
            customerAddress = customerAddress,
            discount = discount,
            taxPercentage = "",
            previousDues = previousDues,
            paidAmount = paidAmount,
            items = if (items.isNotEmpty()) items else listOf(AiExtractedItem()),
            rawTranscription = text,
            warnings = warnings,
            modelUsed = "Pasted AI text",
            sourceImageUri = imageUri.orEmpty()
        )
    }

    private fun parseItemLine(rawLine: String): AiExtractedItem? {
        var line = rawLine.trim()
        if (line.isBlank() || line.startsWith("#") || line.startsWith("|---")) return null

        // Strip leading markdown pipe or table borders
        if (line.startsWith("|") && line.endsWith("|")) {
            val cells = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            if (cells.size >= 2) {
                // Could be Qty | Item | Rate OR Item | Qty | Rate
                val firstNum = cells[0].toDoubleOrNull()
                val secondNum = cells[1].toDoubleOrNull()
                val thirdNum = cells.getOrNull(2)?.let { cleanNumber(it).toDoubleOrNull() }

                return when {
                    firstNum != null && cells.size >= 3 && thirdNum != null -> {
                        AiExtractedItem(
                            originalText = cells[1],
                            translatedName = cells[1],
                            quantity = cleanNumber(cells[0]).ifEmpty { "1" },
                            rate = cleanNumber(cells[2]).ifEmpty { "0" }
                        )
                    }
                    firstNum != null && cells.size >= 2 && secondNum != null -> {
                        // Qty | Rate (missing name)
                        AiExtractedItem(
                            originalText = "Item",
                            translatedName = "Item",
                            quantity = cleanNumber(cells[0]).ifEmpty { "1" },
                            rate = cleanNumber(cells[1]).ifEmpty { "0" }
                        )
                    }
                    else -> {
                        // Item | Rate OR Item | Qty | Rate
                        val name = cells[0]
                        val qty = if (cells.size >= 3) cleanNumber(cells[1]).ifEmpty { "1" } else "1"
                        val rate = cleanNumber(cells.last()).ifEmpty { "0" }
                        AiExtractedItem(
                            originalText = name,
                            translatedName = name,
                            quantity = qty,
                            rate = rate
                        )
                    }
                }
            }
        }

        // Pipe separated line: <qty> | <item name> | <rate>
        if (line.contains("|")) {
            val parts = line.split("|").map { it.trim() }
            if (parts.size >= 3) {
                val p0 = parts[0]
                val p1 = parts[1]
                val p2 = parts[2]
                val p0Digits = cleanNumber(p0)
                val p2Digits = cleanNumber(p2)
                return if (p0Digits.isNotEmpty()) {
                    AiExtractedItem(
                        originalText = p1,
                        translatedName = p1,
                        quantity = p0Digits.ifEmpty { "1" },
                        rate = p2Digits.ifEmpty { "0" }
                    )
                } else {
                    AiExtractedItem(
                        originalText = p0,
                        translatedName = p0,
                        quantity = cleanNumber(p1).ifEmpty { "1" },
                        rate = p2Digits.ifEmpty { "0" }
                    )
                }
            } else if (parts.size == 2) {
                val p0 = parts[0]
                val p1 = parts[1]
                val p1Digits = cleanNumber(p1)
                return AiExtractedItem(
                    originalText = p0,
                    translatedName = p0,
                    quantity = "1",
                    rate = p1Digits.ifEmpty { "0" }
                )
            }
        }

        // Loose pattern: "5 लीची x 50" or "5 X 50" or "10 Mango @ 150"
        val xMatch = Regex("""^(\d+(?:\.\d+)?)\s*(.*?)\s*[xX*@]\s*(\d+(?:\.\d+)?)$""").find(line)
        if (xMatch != null) {
            val qty = xMatch.groupValues[1]
            val desc = xMatch.groupValues[2].trim().ifEmpty { "Item" }
            val rate = xMatch.groupValues[3]
            return AiExtractedItem(
                originalText = desc,
                translatedName = desc,
                quantity = qty,
                rate = rate
            )
        }

        // Loose pattern: "Item Name 5 x 50"
        val itemXMatch = Regex("""^(.*?)\s+(\d+(?:\.\d+)?)\s*[xX*@]\s*(\d+(?:\.\d+)?)$""").find(line)
        if (itemXMatch != null) {
            val desc = itemXMatch.groupValues[1].trim()
            val qty = itemXMatch.groupValues[2]
            val rate = itemXMatch.groupValues[3]
            return AiExtractedItem(
                originalText = desc,
                translatedName = desc,
                quantity = qty,
                rate = rate
            )
        }

        return null
    }

    fun parseRateList(text: String): List<ParsedRateRow> {
        val stripped = stripCodeFences(text.trim())
        val lines = stripped.lines().map { it.trim() }.filter { it.isNotBlank() }
        val results = mutableListOf<ParsedRateRow>()

        for (rawLine in lines) {
            val line = normalizeDigits(rawLine)
            if (line.startsWith("#") || line.startsWith("|---") || line.equals("name | wholesale | retail", ignoreCase = true)) {
                continue
            }

            // Strip leading numbering: "1.", "1)", "1 -", etc.
            val cleanedNumbering = line.replace(Regex("""^\s*\d+[\.\)\-:]\s*"""), "").trim()
            if (cleanedNumbering.isBlank()) continue

            // Check pipe separated: <plant name> | <wholesale rate> | <retail rate>
            if (cleanedNumbering.contains("|")) {
                val parts = cleanedNumbering.split("|").map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size >= 3) {
                    val name = parts[0]
                    val w = cleanNumber(parts[1]).toDoubleOrNull() ?: 0.0
                    val r = cleanNumber(parts[2]).toDoubleOrNull() ?: 0.0
                    val isValid = r > 0.0 || w > 0.0
                    results.add(
                        ParsedRateRow(
                            name = name,
                            wholesalePrice = w,
                            retailPrice = r,
                            isValid = isValid,
                            statusNote = if (!isValid) "Missing price" else ""
                        )
                    )
                    continue
                } else if (parts.size == 2) {
                    val name = parts[0]
                    val price = cleanNumber(parts[1]).toDoubleOrNull() ?: 0.0
                    val isValid = price > 0.0
                    results.add(
                        ParsedRateRow(
                            name = name,
                            wholesalePrice = 0.0,
                            retailPrice = price,
                            isValid = isValid,
                            statusNote = if (!isValid) "Missing price" else ""
                        )
                    )
                    continue
                }
            }

            // Tab or comma separated
            val separator = if (cleanedNumbering.contains("\t")) "\t" else if (cleanedNumbering.contains(",")) "," else null
            if (separator != null) {
                val parts = cleanedNumbering.split(separator).map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size >= 3) {
                    val name = parts[0]
                    val w = cleanNumber(parts[1]).toDoubleOrNull() ?: 0.0
                    val r = cleanNumber(parts[2]).toDoubleOrNull() ?: 0.0
                    val isValid = r > 0.0 || w > 0.0
                    results.add(
                        ParsedRateRow(
                            name = name,
                            wholesalePrice = w,
                            retailPrice = r,
                            isValid = isValid,
                            statusNote = if (!isValid) "Missing price" else ""
                        )
                    )
                    continue
                } else if (parts.size == 2) {
                    val name = parts[0]
                    val price = cleanNumber(parts[1]).toDoubleOrNull() ?: 0.0
                    val isValid = price > 0.0
                    results.add(
                        ParsedRateRow(
                            name = name,
                            wholesalePrice = 0.0,
                            retailPrice = price,
                            isValid = isValid,
                            statusNote = if (!isValid) "Missing price" else ""
                        )
                    )
                    continue
                }
            }

            // Pattern: "<Plant Name> <wholesale> <retail>" or "<Plant Name> <retail>"
            val tokens = cleanedNumbering.split(Regex("""\s+"""))
            if (tokens.size >= 2) {
                val lastToken = cleanNumber(tokens.last()).toDoubleOrNull()
                val secondLastToken = if (tokens.size >= 3) cleanNumber(tokens[tokens.size - 2]).toDoubleOrNull() else null

                if (lastToken != null && secondLastToken != null && tokens.size >= 3) {
                    val name = tokens.subList(0, tokens.size - 2).joinToString(" ")
                    results.add(
                        ParsedRateRow(
                            name = name,
                            wholesalePrice = secondLastToken,
                            retailPrice = lastToken,
                            isValid = true
                        )
                    )
                    continue
                } else if (lastToken != null) {
                    val name = tokens.subList(0, tokens.size - 1).joinToString(" ")
                    results.add(
                        ParsedRateRow(
                            name = name,
                            wholesalePrice = 0.0,
                            retailPrice = lastToken,
                            isValid = true
                        )
                    )
                    continue
                }
            }

            // Unparseable row: mark invalid with missing price so user can type it or uncheck
            results.add(
                ParsedRateRow(
                    name = cleanedNumbering,
                    wholesalePrice = 0.0,
                    retailPrice = 0.0,
                    isValid = false,
                    statusNote = "Missing price"
                )
            )
        }

        return results
    }

    private fun stripCodeFences(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.startsWith("```")) {
            val firstLineEnd = trimmed.indexOf('\n')
            if (firstLineEnd != -1) {
                trimmed = trimmed.substring(firstLineEnd + 1)
            } else {
                trimmed = trimmed.removePrefix("```")
            }
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.removeSuffix("```").trimEnd()
        }
        return trimmed.trim()
    }
}
