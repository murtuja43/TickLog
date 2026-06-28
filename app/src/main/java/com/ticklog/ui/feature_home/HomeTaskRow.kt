package com.ticklog.ui.feature_home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ticklog.R
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.domain.model.DailyTask

/**
 * One tickable task: a large, ripple-backed row with a checkbox, title and
 * optional note.
 *
 * The entire row is the touch target (well above the 48dp minimum). Tapping
 * toggles completion; a long press opens the actions sheet. Completion is
 * animated — the text strikes through and softly fades — so ticking feels
 * tactile. Accessibility is handled explicitly: the row exposes a single merged
 * node with the task title as its label, a checkbox role, and a spoken
 * completed/not-completed state.
 *
 * @param task the task to render.
 * @param onToggle invoked when the row is tapped.
 * @param onLongPress invoked on long press (opens the actions sheet).
 * @param modifier external layout modifier.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: DailyTask,
    onToggle: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val completed = task.isCompleted

    // Completed tasks recede: their content fades toward a muted tone.
    val contentAlpha by animateFloatAsState(
        targetValue = if (completed) 0.55f else 1f,
        label = "taskContentAlpha",
    )
    val titleColor by animateColorAsState(
        targetValue = if (completed) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "taskTitleColor",
    )

    val toggleLabel = stringResource(R.string.home_toggle_task)
    val stateCompleted = stringResource(R.string.cd_state_completed)
    val stateIncomplete = stringResource(R.string.cd_state_incomplete)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .combinedClickable(
                onClick = onToggle,
                onLongClick = onLongPress,
                onClickLabel = toggleLabel,
            )
            .padding(
                horizontal = TickLogTheme.spacing.small,
                vertical = TickLogTheme.spacing.small,
            )
            // Merge into one node TalkBack reads as "<title>, <state>, checkbox".
            .clearAndSetSemantics {
                role = Role.Checkbox
                contentDescription = task.title
                stateDescription = if (completed) stateCompleted else stateIncomplete
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = completed,
            onCheckedChange = null, // the whole row owns the toggle
            modifier = Modifier.size(40.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary,
            ),
        )

        Column(
            modifier = Modifier
                .padding(start = TickLogTheme.spacing.small)
                .alpha(contentAlpha),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                textDecoration = if (completed) TextDecoration.LineThrough else null,
            )
            if (!task.note.isNullOrBlank()) {
                Text(
                    text = task.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = if (completed) TextDecoration.LineThrough else null,
                    modifier = Modifier.padding(top = TickLogTheme.spacing.extraSmall),
                )
            }
        }
    }
}
