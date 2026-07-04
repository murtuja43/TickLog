package com.ticklog.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.ticklog.R
import com.ticklog.core.designsystem.LocalAnimationsEnabled

/**
 * A large, animated date header with optional previous / next navigation.
 *
 * The headline date cross-fades and slides whenever it changes, which makes day
 * navigation (arrows now, swipe later) feel fluid. The component is fully
 * stateless: it renders the [primaryText]/[secondaryText] it is given and
 * forwards navigation intent through callbacks.
 *
 * @param primaryText the prominent line, e.g. "Saturday, 27 June".
 * @param secondaryText the supporting line, e.g. "Today" or the year.
 * @param modifier external layout modifier.
 * @param onPreviousClick if non-null, shows a left chevron that invokes it.
 * @param onNextClick if non-null, shows a right chevron that invokes it.
 */
@Composable
fun DateHeader(
    primaryText: String,
    secondaryText: String,
    modifier: Modifier = Modifier,
    onPreviousClick: (() -> Unit)? = null,
    onNextClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (onPreviousClick != null) {
            IconButton(onClick = onPreviousClick) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = stringResource(R.string.home_previous_day),
                )
            }
        }

        val animate = LocalAnimationsEnabled.current
        AnimatedContent(
            targetState = primaryText to secondaryText,
            transitionSpec = {
                if (animate) {
                    (slideInVertically { height -> height / 2 } + fadeIn())
                        .togetherWith(slideOutVertically { height -> -height / 2 } + fadeOut())
                } else {
                    EnterTransition.None togetherWith ExitTransition.None
                }
            },
            label = "dateHeaderContent",
        ) { (primary, secondary) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = primary,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (onNextClick != null) {
            IconButton(onClick = onNextClick) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = stringResource(R.string.home_next_day),
                )
            }
        }
    }
}
