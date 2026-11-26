package com.vishalnursery.cashmemo.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    @Test
    fun parseRejectsInvalidCalendarDatesAndBadFormats() {
        assertNull(DateUtils.parseDdMmYyyyEpochDay(""))
        assertNull(DateUtils.parseDdMmYyyyEpochDay("30/02/2026"))
        assertNull(DateUtils.parseDdMmYyyyEpochDay("31/04/2026"))
        assertNull(DateUtils.parseDdMmYyyyEpochDay("15/13/2026"))
        assertNull(DateUtils.parseDdMmYyyyEpochDay("2026-11-26"))
        assertNull(DateUtils.parseDdMmYyyyEpochDay("15/11/26"))
    }

    @Test
    fun sortKeyOrdersChronologicallyAcrossMonthsAndYears() {
        val dates = listOf("05/01/2027", "28/11/2026", "01/12/2026", "", "15/11/2026")
        val sorted = dates.sortedBy { DateUtils.dateSortKey(it) }
        assertEquals(
            listOf("15/11/2026", "28/11/2026", "01/12/2026", "05/01/2027", ""),
            sorted
        )
    }

    @Test
    fun overdueTodayAndTomorrowClassificationAreExact() {
        val today = DateUtils.parseDdMmYyyyEpochDay("26/11/2026")!!
        assertTrue(DateUtils.isOverdue("25/11/2026", today))
        assertTrue(DateUtils.isOverdue("31/12/2025", today))
        assertFalse(DateUtils.isOverdue("26/11/2026", today))
        assertFalse(DateUtils.isOverdue("27/11/2026", today))
        assertFalse(DateUtils.isOverdue("invalid", today))

        assertTrue(DateUtils.isToday("26/11/2026", today))
        assertFalse(DateUtils.isToday("27/11/2026", today))

        assertTrue(DateUtils.isTomorrow("27/11/2026", today))
        assertFalse(DateUtils.isTomorrow("26/11/2026", today))
    }

    @Test
    fun roundTripUtcMillisPreservesDdMmYyyy() {
        val millis = DateUtils.parseDdMmYyyyUtcMillis("09/03/2026")!!
        assertEquals("09/03/2026", DateUtils.formatUtcMillisDdMmYyyy(millis))
    }
}
