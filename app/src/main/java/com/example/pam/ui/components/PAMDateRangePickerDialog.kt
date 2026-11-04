package com.example.pam.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PAMDateRangePickerDialog(
    initialStartDateMillis: Long? = null,
    initialEndDateMillis: Long? = null,
    onConfirm: (startDateMillis: Long, endDateMillis: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initialUtcStart = remember(initialStartDateMillis) {
        localMillisToUtcMidnight(initialStartDateMillis)
    }
    val initialUtcEnd = remember(initialEndDateMillis) {
        localMillisToUtcMidnight(initialEndDateMillis)
    }

    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialUtcStart,
        initialSelectedEndDateMillis = initialUtcEnd
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        confirmButton = {
            TextButton(
                onClick = {
                    val startUtc = state.selectedStartDateMillis
                    if (startUtc != null) {
                        val endUtc = state.selectedEndDateMillis ?: startUtc
                        val (startLocal, endLocal) = if (startUtc <= endUtc) {
                            utcMidnightToLocalDayBounds(startUtc, endUtc)
                        } else {
                            utcMidnightToLocalDayBounds(endUtc, startUtc)
                        }
                        onConfirm(startLocal, endLocal)
                    }
                },
                enabled = state.selectedStartDateMillis != null
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DateRangePicker(
            state = state,
            title = {
                Text(
                    text = "Select date range",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 4.dp)
                )
            },
            headline = {
                DateRangePickerDefaults.DateRangePickerHeadline(
                    selectedStartDateMillis = state.selectedStartDateMillis,
                    selectedEndDateMillis = state.selectedEndDateMillis,
                    displayMode = state.displayMode,
                    dateFormatter = remember { DatePickerDefaults.dateFormatter() },
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 8.dp)
                )
            },
            showModeToggle = false,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        )
    }
}

private fun localMillisToUtcMidnight(localMillis: Long?): Long? {
    if (localMillis == null) return null
    val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localCal.get(Calendar.YEAR),
            localCal.get(Calendar.MONTH),
            localCal.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
    }
    return utcCal.timeInMillis
}

private fun utcMidnightToLocalDayBounds(startUtcMillis: Long, endUtcMillis: Long): Pair<Long, Long> {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = startUtcMillis
    }
    val startYear = utcCal.get(Calendar.YEAR)
    val startMonth = utcCal.get(Calendar.MONTH)
    val startDay = utcCal.get(Calendar.DAY_OF_MONTH)

    val localStartCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, startYear)
        set(Calendar.MONTH, startMonth)
        set(Calendar.DAY_OF_MONTH, startDay)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = endUtcMillis
    }
    val endYear = endCal.get(Calendar.YEAR)
    val endMonth = endCal.get(Calendar.MONTH)
    val endDay = endCal.get(Calendar.DAY_OF_MONTH)

    val localEndCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, endYear)
        set(Calendar.MONTH, endMonth)
        set(Calendar.DAY_OF_MONTH, endDay)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    return Pair(localStartCal.timeInMillis, localEndCal.timeInMillis)
}
