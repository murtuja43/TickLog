package com.ticklog.core.designsystem.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ticklog.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * A reusable Material 3 date-picker dialog that speaks [LocalDate].
 *
 * The Material picker works in UTC millis; the conversion is kept here so every
 * caller (onboarding, export, jump-to-date) shares one correct implementation.
 *
 * @param initialDate the date shown when opened.
 * @param onDismiss invoked when dismissed without choosing.
 * @param onDateSelected invoked with the chosen date on confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}
