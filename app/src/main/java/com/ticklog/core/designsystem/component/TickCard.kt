package com.ticklog.core.designsystem.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ticklog.core.designsystem.theme.TickLogTheme

/**
 * The foundational surface of the app: a rounded, low-elevation card.
 *
 * In a monochrome design we cannot lean on colour to separate content, so cards
 * carry that weight — a soft outline in light mode and a subtly raised surface
 * in dark mode. The component supports both a static and a clickable variant
 * through a single nullable [onClick]; the content slot uses a [ColumnScope] so
 * callers compose freely inside.
 *
 * @param modifier external layout modifier.
 * @param onClick optional click handler; when non-null the card becomes a button.
 * @param content card body, laid out in a padded column.
 */
@Composable
fun TickCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val border = CardDefaults.outlinedCardBorder()
    val contentPadding = TickLogTheme.spacing.large

    if (onClick != null) {
        OutlinedCard(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            border = border,
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        OutlinedCard(
            modifier = modifier,
            shape = shape,
            border = border,
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

/**
 * A filled (container-tinted) variant for areas that need stronger grouping than
 * an outline provides, such as grouped settings rows.
 */
@Composable
fun TickFilledCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(TickLogTheme.spacing.small),
            content = content,
        )
    }
}
