package com.ticklog.ui.feature_onboarding

import androidx.compose.runtime.Immutable

/**
 * A single in-progress item in the onboarding checklist builder.
 *
 * @property id a stable, builder-local identifier used as a LazyColumn key and
 *   for reordering — independent of any database id (none exists yet).
 * @property title the task title (always non-blank once added).
 * @property note an optional note.
 */
@Immutable
data class BuilderItem(
    val id: Long,
    val title: String,
    val note: String?,
)

/**
 * Immutable UI state for the checklist builder (onboarding step 2).
 *
 * @property items the ordered tasks the user is assembling.
 * @property isSaving true while the checklist is being created.
 */
@Immutable
data class ChecklistBuilderUiState(
    val items: List<BuilderItem> = emptyList(),
    val isSaving: Boolean = false,
) {
    /** True once there is at least one item and nothing is in flight. */
    val canCreate: Boolean get() = items.isNotEmpty() && !isSaving
}
