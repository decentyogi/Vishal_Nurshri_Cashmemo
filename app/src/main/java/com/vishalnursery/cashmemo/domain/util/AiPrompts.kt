package com.vishalnursery.cashmemo.domain.util

import com.vishalnursery.cashmemo.domain.model.TranslationMode

object AiPrompts {

    fun getMemoPrompt(mode: TranslationMode): String {
        val translationRule = when (mode) {
            TranslationMode.ENGLISH_WITH_HINDI ->
                "Write every item name in English followed by the original Hindi in brackets: English name (हिंदी नाम). If it is already English, keep clean English."
            TranslationMode.ENGLISH_ONLY ->
                "Translate every item name into clear English."
            TranslationMode.ORIGINAL ->
                "Keep every item name exactly as written, in its original script. Do not translate."
        }

        return """
You are reading a photo of a handwritten or printed nursery cash memo (Hindi, Hinglish or English).
Read EVERY line carefully. Do not skip, merge, guess or invent anything. If a word or number is unreadable, write ? for it.

Reply with ONE plain-text code block and nothing else, in exactly this layout:

Customer: <name>
Mobile: <number>
Address: <text>
Date: <DD/MM/YYYY>
Memo No: <number>
Discount: <number>
Previous Dues: <number>
Paid: <number>
---
<qty> | <item name> | <rate>
<qty> | <item name> | <rate>

Rules:
- Leave a header value empty when it is not written on the paper. Never invent customer or memo details.
- After the --- line write one item per line, in the order written on the paper.
- qty and rate are plain numbers: digits 0-9 only, no currency sign, no commas.
- Shorthand "<qty> <item> x <rate>" means that quantity at that rate per piece. If only a line total is written, use qty 1 and rate = that total.
- $translationRule
""".trimIndent()
    }

    fun getRateListPrompt(mode: TranslationMode): String {
        val translationRule = when (mode) {
            TranslationMode.ENGLISH_WITH_HINDI ->
                "Write every plant name in English followed by the original Hindi in brackets: English name (हिंदी नाम). If it is already English, keep clean English."
            TranslationMode.ENGLISH_ONLY ->
                "Translate every plant name into clear English."
            TranslationMode.ORIGINAL ->
                "Keep every plant name exactly as written, in its original script. Do not translate."
        }

        return """
You are reading a photo of a nursery plant rate list or price board (Hindi, Hinglish or English).
Read EVERY line carefully. Do not skip, merge, guess or invent anything.

Reply with ONE plain-text code block and nothing else, one plant per line, in exactly this layout:

<plant name> | <wholesale rate> | <retail rate>
<plant name> | <wholesale rate> | <retail rate>

Rules:
- Drop the line number. Keep the plant name as written, handled per the rule below.
- Rates are plain numbers: digits 0-9 only, no currency sign, no commas.
- If a line has only one price, put it in the retail position and leave wholesale empty.
- $translationRule
""".trimIndent()
    }
}
