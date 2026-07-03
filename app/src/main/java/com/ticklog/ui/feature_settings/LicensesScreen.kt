package com.ticklog.ui.feature_settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ticklog.R
import com.ticklog.core.designsystem.component.ResponsiveContainer
import com.ticklog.core.designsystem.component.TickCard
import com.ticklog.core.designsystem.component.TickTopAppBar
import com.ticklog.core.designsystem.theme.TickLogTheme

/** A single open-source dependency and the licence it ships under. */
private data class OpenSourceLibrary(val name: String, val license: String)

private val LIBRARIES = listOf(
    OpenSourceLibrary("Jetpack Compose", "Apache License 2.0"),
    OpenSourceLibrary("Material 3 (Compose)", "Apache License 2.0"),
    OpenSourceLibrary("AndroidX Core, Activity & Lifecycle", "Apache License 2.0"),
    OpenSourceLibrary("Navigation Compose", "Apache License 2.0"),
    OpenSourceLibrary("Room", "Apache License 2.0"),
    OpenSourceLibrary("DataStore", "Apache License 2.0"),
    OpenSourceLibrary("Hilt / Dagger", "Apache License 2.0"),
    OpenSourceLibrary("Kotlin Coroutines", "Apache License 2.0"),
    OpenSourceLibrary("kotlinx.serialization", "Apache License 2.0"),
    OpenSourceLibrary("Coil", "Apache License 2.0"),
)

/**
 * Lists the open-source libraries TickLog is built on and their licences.
 *
 * @param onNavigateUp invoked when the user taps the back arrow.
 * @param modifier external layout modifier.
 */
@Composable
fun LicensesScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.settings_licenses),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        ResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(TickLogTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.medium),
            ) {
                items(items = LIBRARIES, key = { it.name }) { library ->
                    TickCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = library.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = library.license,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
