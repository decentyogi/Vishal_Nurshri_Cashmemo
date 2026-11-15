package com.vishalnursery.cashmemo.domain.util

import com.vishalnursery.cashmemo.domain.model.RateNameStyle
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

    fun getRateListPrompt(style: RateNameStyle): String {
        val nameRule = when (style) {
            RateNameStyle.ROMAN_WITH_HINDI ->
                "Write each plant name as: <Roman name> (<original Hindi>). The Roman name is a transliteration: how the Hindi word sounds, written in English letters the way an Indian shopkeeper would type it on a phone. Do NOT translate it into English. If the paper already shows an English or Roman name, keep it exactly and add no brackets."
            RateNameStyle.ENGLISH_WITH_HINDI ->
                "Write each plant name as: <common English name> (<original Hindi>). If the paper already shows an English name, keep it and add no brackets."
            RateNameStyle.AS_WRITTEN ->
                "Copy each plant name exactly as written, in its own script. Do not translate or transliterate."
        }

        return """
You are reading a photo of a handwritten or printed plant price list from a nursery (Hindi, Hinglish or English). It may be a ruled table with a header row, or numbered lines. There can be up to 50 rows.
Read EVERY row from top to bottom. Do not skip, merge, guess or invent anything. Write ? for any word or number you cannot read.

Reply with ONE plain-text code block and nothing else, one plant per line, in exactly this layout:

<plant name> | <wholesale rate> | <retail rate>

Rules:
- Do NOT output the header row, ruled lines, line numbers or any commentary.
- Use the column headings on the paper to decide which price is wholesale and which is retail. If there are no headings, the first price is wholesale and the second is retail.
- Always write wholesale first and retail second, even if the paper shows them the other way round.
- Rates are plain numbers: digits 0-9 only, no currency sign, no commas. Convert Devanagari digits to 0-9. Look closely at handwritten digits (1 and 7, 3 and 8, 5 and 6, 0 and 9 are easy to confuse).
- If a row has only one price, put it in the retail position and leave the wholesale position empty, keeping both | separators.
- $nameRule
""".trimIndent()
    }
}
