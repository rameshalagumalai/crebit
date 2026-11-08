package com.example.pam

import com.example.pam.util.ai.DateTimeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateTimeParserTest {

    // Fixed reference time: Wednesday, Nov 4, 2026, 15:30:00 (3:30 PM)
    private val referenceTimeMillis: Long = Calendar.getInstance().apply {
        set(Calendar.YEAR, 2026)
        set(Calendar.MONTH, Calendar.NOVEMBER)
        set(Calendar.DAY_OF_MONTH, 4)
        set(Calendar.HOUR_OF_DAY, 15)
        set(Calendar.MINUTE, 30)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun testParseYesterday() {
        val result = DateTimeParser.parse("Coffee 180 yesterday", referenceTimeMillis)
        assertTrue(result.hasExplicitDate)
        assertFalse(result.hasExplicitTime)
        assertTrue(result.matchedStrings.contains("yesterday"))

        val cal = Calendar.getInstance().apply { timeInMillis = result.epochMillis }
        assertEquals(3, cal.get(Calendar.DAY_OF_MONTH)) // Nov 3, 2026
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
    }

    @Test
    fun testParseDayBeforeYesterday() {
        val result = DateTimeParser.parse("Dinner 1200 day before yesterday", referenceTimeMillis)
        assertTrue(result.hasExplicitDate)

        val cal = Calendar.getInstance().apply { timeInMillis = result.epochMillis }
        assertEquals(2, cal.get(Calendar.DAY_OF_MONTH)) // Nov 2, 2026
    }

    @Test
    fun testParseNDaysAgo() {
        val result = DateTimeParser.parse("Petrol 500 3 days ago", referenceTimeMillis)
        assertTrue(result.hasExplicitDate)

        val cal = Calendar.getInstance().apply { timeInMillis = result.epochMillis }
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH)) // Nov 1, 2026
    }

    @Test
    fun testParseTimeWithAmPm() {
        val result = DateTimeParser.parse("Coffee 150 at 8 pm", referenceTimeMillis)
        assertTrue(result.hasExplicitTime)

        val cal = Calendar.getInstance().apply { timeInMillis = result.epochMillis }
        assertEquals(20, cal.get(Calendar.HOUR_OF_DAY)) // 8 PM = 20:00
        assertEquals(0, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testParseTimeWithColonAndAmPm() {
        val result = DateTimeParser.parse("Cab 350 at 9:15 am", referenceTimeMillis)
        assertTrue(result.hasExplicitTime)

        val cal = Calendar.getInstance().apply { timeInMillis = result.epochMillis }
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testParseYesterdayAt8Pm() {
        val result = DateTimeParser.parse("Dinner 1200 yesterday at 8:30 pm", referenceTimeMillis)
        assertTrue(result.hasExplicitDate)
        assertTrue(result.hasExplicitTime)

        val cal = Calendar.getInstance().apply { timeInMillis = result.epochMillis }
        assertEquals(3, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(20, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
    }

    @Test
    fun testParseExplicitDayMonthDate() {
        val result = DateTimeParser.parse("Flight ticket 4500 on 25th October", referenceTimeMillis)
        assertTrue(result.hasExplicitDate)

        val cal = Calendar.getInstance().apply { timeInMillis = result.epochMillis }
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH))
    }

    @Test
    fun testParsePeriodOfDay_ThisMorning() {
        val result = DateTimeParser.parse("Coffee 120 this morning", referenceTimeMillis)
        assertTrue(result.hasExplicitTime)

        val cal = Calendar.getInstance().apply { timeInMillis = result.epochMillis }
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testParseNoDateOrTime() {
        val result = DateTimeParser.parse("Dosa for 200", referenceTimeMillis)
        assertFalse(result.hasExplicitDate)
        assertFalse(result.hasExplicitTime)
        assertTrue(result.matchedStrings.isEmpty())
        assertEquals(referenceTimeMillis, result.epochMillis)
    }

    @Test
    fun testParseSpeechTimeWithDottedAmPm() {
        // Speech-to-Text output with "p.m." and "a.m."
        val resultPm = DateTimeParser.parse("Dinner 800 at 8 p.m.", referenceTimeMillis)
        assertTrue(resultPm.hasExplicitTime)
        val calPm = Calendar.getInstance().apply { timeInMillis = resultPm.epochMillis }
        assertEquals(20, calPm.get(Calendar.HOUR_OF_DAY)) // 8:00 PM

        val resultAm = DateTimeParser.parse("Coffee 120 at 8:30 a.m.", referenceTimeMillis)
        assertTrue(resultAm.hasExplicitTime)
        val calAm = Calendar.getInstance().apply { timeInMillis = resultAm.epochMillis }
        assertEquals(8, calAm.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calAm.get(Calendar.MINUTE)) // 8:30 AM
    }

    @Test
    fun testParseRailwayTime_ShowsIn12HourFormat() {
        // Railway 20:00 -> converts correctly and formats as 12-hour format
        val result = DateTimeParser.parse("Dinner 1200 at 20:00", referenceTimeMillis)
        assertTrue(result.hasExplicitTime)
        val cal = Calendar.getInstance().apply { timeInMillis = result.epochMillis }
        assertEquals(20, cal.get(Calendar.HOUR_OF_DAY))

        // Formatted in 12-hour format
        val formattedTime = com.example.pam.util.DateUtils.formatTime(result.epochMillis)
        assertEquals("08:00 PM", formattedTime)

        // Railway 14:30 -> 02:30 PM
        val resultAfternoon = DateTimeParser.parse("Lunch 350 at 14:30", referenceTimeMillis)
        assertTrue(resultAfternoon.hasExplicitTime)
        val calAft = Calendar.getInstance().apply { timeInMillis = resultAfternoon.epochMillis }
        assertEquals(14, calAft.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calAft.get(Calendar.MINUTE))
        assertEquals("02:30 PM", com.example.pam.util.DateUtils.formatTime(resultAfternoon.epochMillis))
    }

    @Test
    fun testParseTimeWithPeriodOfDayPhrase() {
        val result = DateTimeParser.parse("Dinner 600 at 8 in the evening", referenceTimeMillis)
        assertTrue(result.hasExplicitTime)
        val cal = Calendar.getInstance().apply { timeInMillis = result.epochMillis }
        assertEquals(20, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals("08:00 PM", com.example.pam.util.DateUtils.formatTime(result.epochMillis))
    }

    @Test
    fun testParseTimeWithOclock() {
        val result = DateTimeParser.parse("Dinner 500 at 8 o'clock", referenceTimeMillis)
        assertTrue(result.hasExplicitTime)
        val cal = Calendar.getInstance().apply { timeInMillis = result.epochMillis }
        assertEquals(20, cal.get(Calendar.HOUR_OF_DAY)) // Dinner defaults to PM
        assertEquals("08:00 PM", com.example.pam.util.DateUtils.formatTime(result.epochMillis))
    }

    @Test
    fun testParseConversationalHourAt() {
        val resultDinner = DateTimeParser.parse("Dinner 500 at 8", referenceTimeMillis)
        assertTrue(resultDinner.hasExplicitTime)
        val calDinner = Calendar.getInstance().apply { timeInMillis = resultDinner.epochMillis }
        assertEquals(20, calDinner.get(Calendar.HOUR_OF_DAY))
        assertEquals("08:00 PM", com.example.pam.util.DateUtils.formatTime(resultDinner.epochMillis))

        val resultTea = DateTimeParser.parse("Tea 20 at 4", referenceTimeMillis)
        assertTrue(resultTea.hasExplicitTime)
        val calTea = Calendar.getInstance().apply { timeInMillis = resultTea.epochMillis }
        assertEquals(16, calTea.get(Calendar.HOUR_OF_DAY)) // 4 PM
        assertEquals("04:00 PM", com.example.pam.util.DateUtils.formatTime(resultTea.epochMillis))
    }
}

