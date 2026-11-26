package com.vishalnursery.cashmemo.domain.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pure JVM date helpers for strict dd/MM/yyyy parsing, chronological sorting,
 * and timezone-safe DatePicker conversion.
 */
object DateUtils {
    private const val MILLIS_PER_DAY = 86_400_000L

    private fun strictDdMmYyyyFormat(): SimpleDateFormat =
        SimpleDateFormat("dd/MM/yyyy", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }

    fun todayDdMmYyyy(): String {
        val local = Calendar.getInstance()
        val y = local.get(Calendar.YEAR)
        val m = local.get(Calendar.MONTH) + 1
        val d = local.get(Calendar.DAY_OF_MONTH)
        return String.format(Locale.US, "%02d/%02d/%04d", d, m, y)
    }

    fun todayEpochDay(): Long {
        val local = Calendar.getInstance()
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                local.get(Calendar.YEAR),
                local.get(Calendar.MONTH),
                local.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
        }
        return utc.timeInMillis / MILLIS_PER_DAY
    }

    fun parseDdMmYyyyUtcMillis(text: String): Long? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        val parts = clean.split('/')
        if (parts.size != 3) return null
        val year = parts[2].toIntOrNull() ?: return null
        if (parts[2].length != 4 || year !in 1970..2100) return null
        return runCatching {
            strictDdMmYyyyFormat().parse(clean)?.time
        }.getOrNull()
    }

    fun formatUtcMillisDdMmYyyy(millis: Long): String {
        return strictDdMmYyyyFormat().format(Date(millis))
    }

    fun parseDdMmYyyyEpochDay(text: String): Long? {
        val millis = parseDdMmYyyyUtcMillis(text) ?: return null
        return millis / MILLIS_PER_DAY
    }

    fun dateSortKey(ddMmYyyy: String): Long {
        return parseDdMmYyyyEpochDay(ddMmYyyy) ?: Long.MAX_VALUE
    }

    fun isOverdue(ddMmYyyy: String, referenceEpochDay: Long = todayEpochDay()): Boolean {
        val day = parseDdMmYyyyEpochDay(ddMmYyyy) ?: return false
        return day < referenceEpochDay
    }

    fun isToday(ddMmYyyy: String, referenceEpochDay: Long = todayEpochDay()): Boolean {
        val day = parseDdMmYyyyEpochDay(ddMmYyyy) ?: return false
        return day == referenceEpochDay
    }

    fun isTomorrow(ddMmYyyy: String, referenceEpochDay: Long = todayEpochDay()): Boolean {
        val day = parseDdMmYyyyEpochDay(ddMmYyyy) ?: return false
        return day == referenceEpochDay + 1L
    }
}
