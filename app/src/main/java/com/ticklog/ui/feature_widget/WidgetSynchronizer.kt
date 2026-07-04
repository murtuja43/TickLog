package com.ticklog.ui.feature_widget

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.glance.appwidget.updateAll
import com.ticklog.di.qualifier.DefaultDispatcher
import com.ticklog.domain.repository.ChecklistRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the home-screen widget in step with in-app changes.
 *
 * It observes the shared day-summary feed — the single source that fires on
 * every completion toggle, task edit/delete, undo and restore — and pushes a
 * widget refresh for each change. Observation is tied to
 * [ProcessLifecycleOwner], so it runs only while the app process is in the
 * foreground; background-only mutations (a widget toggle, a date rollover) are
 * already covered by [ToggleTaskAction] and [TickLogWidgetReceiver], so nothing
 * needs to poll and battery is spared. The first emission on start also serves
 * as the "app opened" refresh.
 */
@Singleton
class WidgetSynchronizer @Inject constructor(
    private val repository: ChecklistRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    /** Begins observing. Safe to call once, from [com.ticklog.TickLogApplication]. */
    fun start(context: Context) {
        val appContext = context.applicationContext
        val owner = ProcessLifecycleOwner.get()
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.observeDaySummaries()
                    .conflate()
                    .collect {
                        withContext(dispatcher) {
                            TickLogWidget().updateAll(appContext)
                        }
                    }
            }
        }
    }
}
