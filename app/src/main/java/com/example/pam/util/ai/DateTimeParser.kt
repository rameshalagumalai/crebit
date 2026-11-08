package com.example.pam.util.ai

import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

data class ParsedDateTime(
    val epochMillis: Long,
    val hasExplicitDate: Boolean,
    val hasExplicitTime: Boolean,
    val matchedStrings: List<String>
)

object DateTimeParser {

    private val monthMap = mapOf(
        "jan" to Calendar.JANUARY, "january" to Calendar.JANUARY,
        "feb" to Calendar.FEBRUARY, "february" to Calendar.FEBRUARY,
        "mar" to Calendar.MARCH, "march" to Calendar.MARCH,
        "apr" to Calendar.APRIL, "april" to Calendar.APRIL,
        "may" to Calendar.MAY,
        "jun" to Calendar.JUNE, "june" to Calendar.JUNE,
        "jul" to Calendar.JULY, "july" to Calendar.JULY,
        "aug" to Calendar.AUGUST, "august" to Calendar.AUGUST,
        "sep" to Calendar.SEPTEMBER, "sept" to Calendar.SEPTEMBER, "september" to Calendar.SEPTEMBER,
        "oct" to Calendar.OCTOBER, "october" to Calendar.OCTOBER,
        "nov" to Calendar.NOVEMBER, "november" to Calendar.NOVEMBER,
        "dec" to Calendar.DECEMBER, "december" to Calendar.DECEMBER
    )

    private val dayOfWeekMap = mapOf(
        "sunday" to Calendar.SUNDAY, "sun" to Calendar.SUNDAY,
        "monday" to Calendar.MONDAY, "mon" to Calendar.MONDAY,
        "tuesday" to Calendar.TUESDAY, "tue" to Calendar.TUESDAY,
        "wednesday" to Calendar.WEDNESDAY, "wed" to Calendar.WEDNESDAY,
        "thursday" to Calendar.THURSDAY, "thu" to Calendar.THURSDAY,
        "friday" to Calendar.FRIDAY, "fri" to Calendar.FRIDAY,
        "saturday" to Calendar.SATURDAY, "sat" to Calendar.SATURDAY
    )

    /**
     * Parses date and time from natural speech or text input.
     * Returns a [ParsedDateTime] with extracted epoch milliseconds, flags, and recognized substrings.
     */
    fun parse(input: String, nowMillis: Long = System.currentTimeMillis()): ParsedDateTime {
        val lower = input.lowercase(Locale.US)
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }

        var hasExplicitDate = false
        var hasExplicitTime = false
        val matchedStrings = mutableListOf<String>()

        // 1. Relative Dates: "day before yesterday", "yesterday", "today", "tonight", "last night"
        if (lower.contains(Regex("\\bday before yesterday\\b"))) {
            cal.add(Calendar.DAY_OF_YEAR, -2)
            hasExplicitDate = true
            matchedStrings.add("day before yesterday")
        } else if (lower.contains(Regex("\\byesterday\\b"))) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            hasExplicitDate = true
            matchedStrings.add("yesterday")
        } else if (lower.contains(Regex("\\blast night\\b"))) {
            // If currently daytime, last night means yesterday
            if (cal.get(Calendar.HOUR_OF_DAY) < 14) {
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
            cal.set(Calendar.HOUR_OF_DAY, 21)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            hasExplicitDate = true
            hasExplicitTime = true
            matchedStrings.add("last night")
        } else if (lower.contains(Regex("\\btonight\\b"))) {
            cal.set(Calendar.HOUR_OF_DAY, 20)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            hasExplicitDate = true
            hasExplicitTime = true
            matchedStrings.add("tonight")
        } else if (lower.contains(Regex("\\btoday\\b"))) {
            hasExplicitDate = true
            matchedStrings.add("today")
        }

        // 2. Relative Days Ago: e.g. "2 days ago", "3 days ago"
        val daysAgoPattern = Pattern.compile("\\b(\\d+)\\s+days?\\s+ago\\b", Pattern.CASE_INSENSITIVE)
        val daysAgoMatcher = daysAgoPattern.matcher(input)
        if (!hasExplicitDate && daysAgoMatcher.find()) {
            val days = daysAgoMatcher.group(1)?.toIntOrNull() ?: 1
            cal.add(Calendar.DAY_OF_YEAR, -days)
            hasExplicitDate = true
            daysAgoMatcher.group(0)?.let { matchedStrings.add(it) }
        }

        // 3. Days of the week: e.g. "last friday", "on friday", "this monday", "friday"
        val dayOfWeekRegex = "\\b(?:(last|this|on)\\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun)\\b"
        val dowPattern = Pattern.compile(dayOfWeekRegex, Pattern.CASE_INSENSITIVE)
        val dowMatcher = dowPattern.matcher(input)
        if (!hasExplicitDate && dowMatcher.find()) {
            val prefix = dowMatcher.group(1)?.lowercase(Locale.US)
            val dayName = dowMatcher.group(2)?.lowercase(Locale.US)
            val targetDow = dayOfWeekMap[dayName]
            if (targetDow != null) {
                val isLast = prefix == "last"
                val diff = calculateDayDiff(cal.get(Calendar.DAY_OF_WEEK), targetDow, isLast)
                cal.add(Calendar.DAY_OF_YEAR, -diff)
                hasExplicitDate = true
                dowMatcher.group(0)?.let { matchedStrings.add(it) }
            }
        }

        // 4. Explicit Dates: e.g. "4th nov", "25th october 2026", "nov 4", "november 25th"
        // 4A: Day Month (e.g., "25th October", "4 nov")
        val dayMonthPattern = Pattern.compile(
            "\\b(?:on\\s+)?(\\d{1,2})(?:st|nd|rd|th)?\\s+([a-zA-Z]{3,9})(?:\\s+(\\d{4}))?\\b",
            Pattern.CASE_INSENSITIVE
        )
        val dmMatcher = dayMonthPattern.matcher(input)
        if (!hasExplicitDate && dmMatcher.find()) {
            val day = dmMatcher.group(1)?.toIntOrNull()
            val monthStr = dmMatcher.group(2)?.lowercase(Locale.US)
            val year = dmMatcher.group(3)?.toIntOrNull()
            val month = monthMap[monthStr]
            if (day != null && day in 1..31 && month != null) {
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                if (year != null && year in 2000..2100) {
                    cal.set(Calendar.YEAR, year)
                }
                hasExplicitDate = true
                dmMatcher.group(0)?.let { matchedStrings.add(it) }
            }
        }

        // 4B: Month Day (e.g., "October 25th", "Nov 4")
        val monthDayPattern = Pattern.compile(
            "\\b(?:on\\s+)?([a-zA-Z]{3,9})\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:\\s+(\\d{4}))?\\b",
            Pattern.CASE_INSENSITIVE
        )
        val mdMatcher = monthDayPattern.matcher(input)
        if (!hasExplicitDate && mdMatcher.find()) {
            val monthStr = mdMatcher.group(1)?.lowercase(Locale.US)
            val day = mdMatcher.group(2)?.toIntOrNull()
            val year = mdMatcher.group(3)?.toIntOrNull()
            val month = monthMap[monthStr]
            if (day != null && day in 1..31 && month != null) {
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                if (year != null && year in 2000..2100) {
                    cal.set(Calendar.YEAR, year)
                }
                hasExplicitDate = true
                mdMatcher.group(0)?.let { matchedStrings.add(it) }
            }
        }

        // 4C: Numeric Date: "25/10/2026", "25-10-2026", "25.10"
        val numericDatePattern = Pattern.compile(
            "\\b(?:on\\s+)?(\\d{1,2})[/\\-.](\\d{1,2})(?:[/\\-.](\\d{2,4}))?\\b"
        )
        val numMatcher = numericDatePattern.matcher(input)
        if (!hasExplicitDate && numMatcher.find()) {
            val part1 = numMatcher.group(1)?.toIntOrNull() ?: 0
            val part2 = numMatcher.group(2)?.toIntOrNull() ?: 0
            val year = numMatcher.group(3)?.toIntOrNull()
            // DD/MM standard
            if (part1 in 1..31 && part2 in 1..12) {
                cal.set(Calendar.DAY_OF_MONTH, part1)
                cal.set(Calendar.MONTH, part2 - 1)
                if (year != null) {
                    val fullYear = if (year < 100) 2000 + year else year
                    cal.set(Calendar.YEAR, fullYear)
                }
                hasExplicitDate = true
                numMatcher.group(0)?.let { matchedStrings.add(it) }
            }
        }

        // 5. Time parsing
        // 5A. 12-Hour clock with explicit AM/PM marker (handles "8 pm", "8 p.m.", "8:30 pm", "8:30 p.m.", "8.30 p.m.", "8am", "8 a.m.")
        val amPmPattern = Pattern.compile(
            "\\b(?:at\\s+)?(\\d{1,2})(?:[:.](\\d{2}))?\\s*(?:([ap])\\s*\\.?\\s*m\\.?|([ap]m))(?=\\b|\\s|[^a-zA-Z0-9]|$)",
            Pattern.CASE_INSENSITIVE
        )
        val amPmMatcher = amPmPattern.matcher(input)
        if (!hasExplicitTime && amPmMatcher.find()) {
            val hour = amPmMatcher.group(1)?.toIntOrNull() ?: 0
            val minute = amPmMatcher.group(2)?.toIntOrNull() ?: 0
            val marker = (amPmMatcher.group(3) ?: amPmMatcher.group(4))?.lowercase(Locale.US) ?: ""
            val isPm = marker.startsWith("p")

            var resolvedHour = hour
            if (isPm && hour < 12) resolvedHour += 12
            if (!isPm && hour == 12) resolvedHour = 0

            if (resolvedHour in 0..23 && minute in 0..59) {
                cal.set(Calendar.HOUR_OF_DAY, resolvedHour)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                hasExplicitTime = true
                amPmMatcher.group(0)?.let { matchedStrings.add(it.trim()) }
            }
        }

        // 5B. 12-Hour clock with period-of-day phrase or o'clock (e.g. "at 8 in the evening", "8:30 in the morning", "at 8 o'clock in the evening")
        val hourPeriodPattern = Pattern.compile(
            "\\b(?:at\\s+)?(\\d{1,2})(?:[:.](\\d{2}))?\\s*(?:o['\\s]?clock)?\\s*(?:in\\s+the\\s+(morning|evening|afternoon)|this\\s+(morning|evening|afternoon)|at\\s+(night))\\b",
            Pattern.CASE_INSENSITIVE
        )
        val hourPeriodMatcher = hourPeriodPattern.matcher(input)
        if (!hasExplicitTime && hourPeriodMatcher.find()) {
            val hour = hourPeriodMatcher.group(1)?.toIntOrNull() ?: 0
            val minute = hourPeriodMatcher.group(2)?.toIntOrNull() ?: 0
            val period = (hourPeriodMatcher.group(3) ?: hourPeriodMatcher.group(4) ?: hourPeriodMatcher.group(5))?.lowercase(Locale.US)

            var resolvedHour = hour
            when (period) {
                "evening", "night" -> {
                    if (hour < 12) resolvedHour = hour + 12
                }
                "afternoon" -> {
                    if (hour < 12) resolvedHour = hour + 12
                }
                "morning" -> {
                    if (hour == 12) resolvedHour = 0
                }
            }

            if (resolvedHour in 0..23 && minute in 0..59) {
                cal.set(Calendar.HOUR_OF_DAY, resolvedHour)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                hasExplicitTime = true
                hourPeriodMatcher.group(0)?.let { matchedStrings.add(it.trim()) }
            }
        }

        // Standalone o'clock (e.g. "at 8 o'clock", "8 o clock")
        val oclockPattern = Pattern.compile(
            "\\b(?:at\\s+)?(\\d{1,2})\\s*o['\\s]?clock\\b",
            Pattern.CASE_INSENSITIVE
        )
        val oclockMatcher = oclockPattern.matcher(input)
        if (!hasExplicitTime && oclockMatcher.find()) {
            val hour = oclockMatcher.group(1)?.toIntOrNull() ?: 0
            var resolvedHour = hour
            val isEveningContext = lower.contains("dinner") || lower.contains("night") ||
                    lower.contains("evening") || lower.contains("supper")
            val isMorningContext = lower.contains("breakfast") || lower.contains("morning")

            if (isEveningContext && hour < 12) {
                resolvedHour = hour + 12
            } else if (!isMorningContext && hour in 1..6) {
                resolvedHour = hour + 12
            } else if (!isMorningContext && hour in 7..11) {
                val currentHour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
                if (currentHour >= 18) resolvedHour = hour + 12
            }

            if (resolvedHour in 0..23) {
                cal.set(Calendar.HOUR_OF_DAY, resolvedHour)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                hasExplicitTime = true
                oclockMatcher.group(0)?.let { matchedStrings.add(it.trim()) }
            }
        }

        // 5C. 24-Hour / Railway time: e.g. "14:00", "at 20:30", "08:15", "at 1400 hrs", "2000 hours"
        val railwayColonPattern = Pattern.compile(
            "\\b(?:at\\s+)?([01]?\\d|2[0-3]):([0-5]\\d)(?:\\s*(?:hrs?|hours?))?\\b",
            Pattern.CASE_INSENSITIVE
        )
        val railwayMatcher = railwayColonPattern.matcher(input)
        if (!hasExplicitTime && railwayMatcher.find()) {
            val hour = railwayMatcher.group(1)?.toIntOrNull() ?: 0
            val minute = railwayMatcher.group(2)?.toIntOrNull() ?: 0
            if (hour in 0..23 && minute in 0..59) {
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                hasExplicitTime = true
                railwayMatcher.group(0)?.let { matchedStrings.add(it.trim()) }
            }
        }

        // Standalone railway hours 13..23 with "at": e.g. "at 14", "at 20", "at 21 hrs"
        val railwayHourPattern = Pattern.compile(
            "\\bat\\s+(1[3-9]|2[0-3])(?:\\s*(?:hrs?|hours?))?\\b",
            Pattern.CASE_INSENSITIVE
        )
        val rHourMatcher = railwayHourPattern.matcher(input)
        if (!hasExplicitTime && rHourMatcher.find()) {
            val hour = rHourMatcher.group(1)?.toIntOrNull() ?: 0
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            hasExplicitTime = true
            rHourMatcher.group(0)?.let { matchedStrings.add(it.trim()) }
        }

        // 5D. Standalone conversational 12-hour with "at": e.g. "at 8", "at 4", "at 8:30"
        val standaloneAtPattern = Pattern.compile(
            "\\bat\\s+([1-9]|1[0-2])(?::([0-5]\\d))?\\b",
            Pattern.CASE_INSENSITIVE
        )
        val atMatcher = standaloneAtPattern.matcher(input)
        if (!hasExplicitTime && atMatcher.find()) {
            val hour = atMatcher.group(1)?.toIntOrNull() ?: 0
            val minute = atMatcher.group(2)?.toIntOrNull() ?: 0

            var resolvedHour = hour
            val isEveningContext = lower.contains("dinner") || lower.contains("night") ||
                    lower.contains("evening") || lower.contains("supper") ||
                    lower.contains("drinks") || lower.contains("beer") || lower.contains("wine") || lower.contains("bar")
            val isExplicitMorning = lower.contains("breakfast") || lower.contains("morning") || lower.contains("brunch")

            if (isEveningContext && hour < 12) {
                resolvedHour = hour + 12
            } else if (isExplicitMorning) {
                resolvedHour = if (hour == 12) 0 else hour
            } else if (hour in 1..6) {
                // Daytime spending at 1..6 is afternoon (1:00 PM to 6:00 PM)
                resolvedHour = hour + 12
            } else if (hour in 7..11) {
                val currentHour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
                if (currentHour >= 18) {
                    resolvedHour = hour + 12
                }
            }

            if (resolvedHour in 0..23 && minute in 0..59) {
                cal.set(Calendar.HOUR_OF_DAY, resolvedHour)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                hasExplicitTime = true
                atMatcher.group(0)?.let { matchedStrings.add(it.trim()) }
            }
        }

        // 5E. Broad Period of Day: "this morning", "in the morning", "in the evening", "in the afternoon", "at noon", "at night"
        if (!hasExplicitTime) {
            when {
                lower.contains(Regex("\\b(?:in\\s+the\\s+morning|this\\s+morning)\\b")) -> {
                    cal.set(Calendar.HOUR_OF_DAY, 9)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    hasExplicitTime = true
                    matchedStrings.add(if (lower.contains("this morning")) "this morning" else "in the morning")
                }
                lower.contains(Regex("\\b(?:at\\s+noon|in\\s+the\\s+afternoon|this\\s+afternoon)\\b")) -> {
                    cal.set(Calendar.HOUR_OF_DAY, 13)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    hasExplicitTime = true
                    matchedStrings.add(
                        if (lower.contains("at noon")) "at noon"
                        else if (lower.contains("this afternoon")) "this afternoon"
                        else "in the afternoon"
                    )
                }
                lower.contains(Regex("\\b(?:in\\s+the\\s+evening|this\\s+evening)\\b")) -> {
                    cal.set(Calendar.HOUR_OF_DAY, 19)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    hasExplicitTime = true
                    matchedStrings.add(if (lower.contains("this evening")) "this evening" else "in the evening")
                }
                lower.contains(Regex("\\b(?:at\\s+night)\\b")) -> {
                    cal.set(Calendar.HOUR_OF_DAY, 21)
                    cal.set(Calendar.MINUTE, 30)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    hasExplicitTime = true
                    matchedStrings.add("at night")
                }
            }
        }

        return ParsedDateTime(
            epochMillis = cal.timeInMillis,
            hasExplicitDate = hasExplicitDate,
            hasExplicitTime = hasExplicitTime,
            matchedStrings = matchedStrings.distinct()
        )
    }

    private fun calculateDayDiff(currentDow: Int, targetDow: Int, isExplicitLast: Boolean): Int {
        var diff = currentDow - targetDow
        if (diff < 0) {
            diff += 7
        } else if (diff == 0 && isExplicitLast) {
            diff = 7
        }
        if (isExplicitLast && diff < 7) {
            diff += 7
        }
        if (diff == 0) {
            diff = 7 // Default to last occurrence if same day
        }
        return diff
    }
}

