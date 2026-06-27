package com.ticklog.core.designsystem.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ticklog.R

/**
 * The shared top app bar for every screen.
 *
 * Wrapping Material 3's [TopAppBar] lets us standardise the title style, the
 * optional back affordance and the (transparent, monochrome) colours in one
 * place. Actions are provided through a [RowScope] slot so each screen supplies
 * its own icon buttons without re-implementing the bar.
 *
 * @param title the screen title.
 * @param modifier external layout modifier.
 * @param onNavigateUp if non-null, renders a back arrow that invokes it.
 * @param actions trailing action icons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TickTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    // All experimental Material 3 usage (TopAppBar + its colours) is kept inside
    // this component so its public signature exposes only stable types — callers
    // never inherit an opt-in requirement.
    val colors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.background,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
    )

    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            if (onNavigateUp != null) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.cd_navigate_up),
                    )
                }
            }
        },
        actions = actions,
        colors = colors,
    )
}
