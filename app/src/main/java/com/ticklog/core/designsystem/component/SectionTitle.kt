package com.ticklog.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ticklog.core.designsystem.theme.TickLogTheme

/**
 * A consistent heading for a group of content, with an optional supporting line.
 *
 * Centralising the title style here means every section across the app shares
 * the same weight, casing and spacing rhythm.
 *
 * @param title the heading text.
 * @param modifier external layout modifier.
 * @param subtitle optional secondary line shown beneath the title.
 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = TickLogTheme.spacing.extraSmall),
            )
        }
    }
}
