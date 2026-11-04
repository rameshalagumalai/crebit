package com.example.pam

import com.example.pam.data.model.DateRangeType
import com.example.pam.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    @Test
    fun testDateRanges() {
        val (todayStart, todayEnd) = DateUtils.getDateRange(DateRangeType.TODAY)
        assertTrue(todayEnd > todayStart)

        val (monthStart, monthEnd) = DateUtils.getDateRange(DateRangeType.THIS_MONTH)
        assertTrue(monthEnd > monthStart)
        assertTrue(monthEnd - monthStart >= 28L * 24 * 60 * 60 * 1000)

        val (allStart, allEnd) = DateUtils.getDateRange(DateRangeType.ALL_TIME)
        assertEquals(0L, allStart)
        assertEquals(Long.MAX_VALUE, allEnd)
    }

    @Test
    fun testRelativeDateToday() {
        val now = System.currentTimeMillis()
        val rel = DateUtils.formatRelativeDate(now)
        assertEquals("Today", rel)
    }

    @Test
    fun testRelativeDateYesterday() {
        val yesterday = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        val rel = DateUtils.formatRelativeDate(yesterday)
        assertEquals("Yesterday", rel)
    }

    @Test
    fun testTimeFormat() {
        val now = System.currentTimeMillis()
        val timeStr = DateUtils.formatTime(now)
        assertTrue(timeStr.matches(Regex("\\d{2}:\\d{2} (AM|PM)")))
    }

    @Test
    fun testCustomDateRange() {
        val start = 1700000000000L
        val end = 1700500000000L
        val (rangeStart, rangeEnd) = DateUtils.getDateRange(DateRangeType.CUSTOM, start, end)
        assertEquals(start, rangeStart)
        assertEquals(end, rangeEnd)

        val (defStart, defEnd) = DateUtils.getDateRange(DateRangeType.CUSTOM)
        assertEquals(0L, defStart)
        assertEquals(Long.MAX_VALUE, defEnd)
    }

    @Test
    fun testFormatDateRange() {
        // Same day
        val cal = java.util.Calendar.getInstance()
        val startOfDay = cal.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
        }.timeInMillis
        val endOfDay = cal.apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
        }.timeInMillis
        val formattedSameDay = DateUtils.formatDateRange(startOfDay, endOfDay)
        assertEquals(DateUtils.formatDate(startOfDay), formattedSameDay)

        // Multiple days same year
        val multiDays = DateUtils.formatDateRange(startOfDay, endOfDay + 5 * 24 * 60 * 60 * 1000L)
        assertTrue(multiDays.contains("-"))
    }
}

