package com.vishalnursery.cashmemo.domain.util

import java.util.Locale

object PlantNameMatcher {

    fun normalize(s: String): String {
        val lower = s.lowercase(Locale.ROOT)
        val sb = StringBuilder()
        for (ch in lower) {
            // Keep letters, digits, whitespace, and Devanagari range
            if (ch.isLetterOrDigit() || ch.isWhitespace() || ch in '\u0900'..'\u097F') {
                sb.append(ch)
            }
        }
        return sb.toString().replace(Regex("""\s+"""), " ").trim()
    }

    fun keys(name: String): Set<String> {
        val raw = name.trim()
        if (raw.isBlank()) return emptySet()
        val result = mutableSetOf<String>()

        val normWhole = normalize(raw)
        if (normWhole.isNotBlank()) {
            result.add(normWhole)
        }

        val openIdx = raw.indexOf('(')
        if (openIdx != -1) {
            val before = raw.substring(0, openIdx).trim()
            val normBefore = normalize(before)
            if (normBefore.isNotBlank()) {
                result.add(normBefore)
            }

            val closeIdx = raw.indexOf(')', startIndex = openIdx + 1)
            if (closeIdx != -1 && closeIdx > openIdx + 1) {
                val inside = raw.substring(openIdx + 1, closeIdx).trim()
                val normInside = normalize(inside)
                if (normInside.isNotBlank()) {
                    result.add(normInside)
                }
            }
        }

        return result
    }

    fun matches(a: String, b: String): Boolean {
        val aKeys = keys(a)
        val bKeys = keys(b)
        if (aKeys.isEmpty() || bKeys.isEmpty()) return false
        return aKeys.intersect(bKeys).isNotEmpty()
    }
}
