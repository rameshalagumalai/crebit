package com.example.pam.util

import com.example.pam.data.model.DateRangeType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    private fun getFormatter(pattern: String): SimpleDateFormat {
        return SimpleDateFormat(pattern, Locale.getDefault())
    }

    fun formatDateTime(millis: Long): String {
        return getFormatter("dd MMM yyyy, hh:mm a").format(Date(millis))
    }

    fun formatDate(millis: Long): String {
        return getFormatter("dd MMM yyyy").format(Date(millis))
    }

    fun formatTime(millis: Long): String {
        return getFormatter("hh:mm a").format(Date(millis))
    }

    fun formatMonthYear(millis: Long): String {
        return getFormatter("MMMM yyyy").format(Date(millis))
    }

    fun formatDayMonth(millis: Long): String {
        return getFormatter("dd MMM").format(Date(millis))
    }

    fun formatRelativeDate(millis: Long): String {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply { timeInMillis = millis }

        val isSameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
        val isToday = isSameYear && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
        val isYesterday = (isSameYear && now.get(Calendar.DAY_OF_YEAR) - target.get(Calendar.DAY_OF_YEAR) == 1) ||
                (now.get(Calendar.YEAR) - target.get(Calendar.YEAR) == 1 &&
                        now.get(Calendar.DAY_OF_YEAR) == 1 &&
                        target.get(Calendar.DAY_OF_YEAR) == target.getActualMaximum(Calendar.DAY_OF_YEAR))

        return when {
            isToday -> "Today"
            isYesterday -> "Yesterday"
            else -> formatDate(millis)
        }
    }

    fun formatDateRange(startMillis: Long, endMillis: Long): String {
        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }

        val sameYear = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)
        val sameDay = sameYear && startCal.get(Calendar.DAY_OF_YEAR) == endCal.get(Calendar.DAY_OF_YEAR)

        return when {
            sameDay -> formatDate(startMillis)
            sameYear -> "${formatDayMonth(startMillis)} - ${formatDate(endMillis)}"
            else -> "${formatDate(startMillis)} - ${formatDate(endMillis)}"
        }
    }

    fun getDateRange(type: DateRangeType, customStart: Long? = null, customEnd: Long? = null): Pair<Long, Long> {
        val cal = Calendar.getInstance()

        return when (type) {
            DateRangeType.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateRangeType.THIS_WEEK -> {
                cal.firstDayOfWeek = Calendar.MONDAY
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateRangeType.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateRangeType.THIS_YEAR -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                val maxDay = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
                cal.set(Calendar.DAY_OF_YEAR, maxDay)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            DateRangeType.ALL_TIME -> {
                Pair(0L, Long.MAX_VALUE)
            }
            DateRangeType.CUSTOM -> {
                val start = customStart ?: 0L
                val end = customEnd ?: Long.MAX_VALUE
                Pair(start, end)
            }
        }
    }
}
