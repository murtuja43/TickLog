package com.ticklog.ui.feature_home

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.ticklog.domain.model.ChecklistTemplate
import com.ticklog.domain.model.CompletionRecord
import com.ticklog.domain.model.DailyChecklist
import com.ticklog.domain.model.DailyTask
import com.ticklog.domain.model.DateFormat
import com.ticklog.domain.model.DateRange
import com.ticklog.domain.model.DeletedTask
import com.ticklog.domain.model.TaskDraft
import com.ticklog.domain.model.TaskOccurrence
import com.ticklog.domain.model.TaskScope
import com.ticklog.domain.model.ThemeMode
import com.ticklog.domain.model.UserPreferences
import com.ticklog.domain.model.WeekStart
import com.ticklog.domain.repository.ChecklistRepository
import com.ticklog.domain.repository.PreferencesRepository
import com.ticklog.domain.usecase.ObserveUserPreferencesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Verifies that persistence failures never crash the Home screen: a failing write
 * surfaces a one-shot [HomeEvent.ActionFailed] and an optimistic toggle is rolled
 * back to the stored state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val today: LocalDate = LocalDate.now()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a failing toggle emits ActionFailed and rolls the checkbox back`() = runTest(dispatcher) {
        val viewModel = buildViewModel(FakeChecklistRepository(failWrites = true))
        val events = collectEvents(viewModel)

        viewModel.onToggleTask(taskId = 1L, currentlyCompleted = false)
        advanceUntilIdle()

        assertThat(events).contains(HomeEvent.ActionFailed)
        // The optimistic "true" was reverted, so the task reads as not completed.
        val content = viewModel.checklistFlow(today).first { it is DayUiState.Content }
        assertThat((content as DayUiState.Content).tasks.first().isCompleted).isFalse()
    }

    @Test
    fun `a failing write surfaces ActionFailed instead of crashing`() = runTest(dispatcher) {
        val viewModel = buildViewModel(FakeChecklistRepository(failWrites = true))
        val events = collectEvents(viewModel)

        viewModel.duplicateTask(1L)
        advanceUntilIdle()

        assertThat(events).contains(HomeEvent.ActionFailed)
    }

    @Test
    fun `a successful write emits no failure`() = runTest(dispatcher) {
        val viewModel = buildViewModel(FakeChecklistRepository(failWrites = false))
        val events = collectEvents(viewModel)

        viewModel.duplicateTask(1L)
        advanceUntilIdle()

        assertThat(events).doesNotContain(HomeEvent.ActionFailed)
    }

    // --- Helpers ------------------------------------------------------------

    private fun buildViewModel(repository: ChecklistRepository): HomeViewModel =
        HomeViewModel(
            checklistRepository = repository,
            observeUserPreferences = ObserveUserPreferencesUseCase(FakePreferencesRepository(today)),
            savedStateHandle = SavedStateHandle(),
        )

    private fun kotlinx.coroutines.test.TestScope.collectEvents(
        viewModel: HomeViewModel,
    ): List<HomeEvent> {
        val events = mutableListOf<HomeEvent>()
        backgroundScope.launch { viewModel.events.toList(events) }
        return events
    }

    /** A checklist repository whose writes optionally fail, for error-path tests. */
    private inner class FakeChecklistRepository(
        private val failWrites: Boolean,
    ) : ChecklistRepository {

        private val task = DailyTask(
            id = 1L, title = "Read", note = null, position = 0,
            isCompleted = false, completedAt = null, isLinkedToTemplate = false,
        )
        private val checklist = DailyChecklist(id = 1L, templateId = 1L, date = today, tasks = listOf(task))

        override fun observeChecklistForDate(date: LocalDate): Flow<DailyChecklist?> =
            flowOf(if (date == today) checklist else null)

        override suspend fun setTaskCompleted(dailyTaskId: Long, completed: Boolean) = failIfConfigured()
        override suspend fun addTask(date: LocalDate, draft: TaskDraft, scope: TaskScope) = failIfConfigured()
        override suspend fun renameTask(dailyTaskId: Long, newTitle: String, scope: TaskScope) = failIfConfigured()
        override suspend fun updateTaskNote(dailyTaskId: Long, newNote: String?, scope: TaskScope) = failIfConfigured()
        override suspend fun restoreTasks(deleted: List<DeletedTask>) = failIfConfigured()

        override suspend fun duplicateTask(dailyTaskId: Long): Long {
            failIfConfigured()
            return 2L
        }

        override suspend fun deleteTask(dailyTaskId: Long, scope: TaskScope): List<DeletedTask> {
            failIfConfigured()
            return emptyList()
        }

        private fun failIfConfigured() {
            if (failWrites) throw IllegalStateException("simulated persistence failure")
        }

        // --- Unused by HomeViewModel -----------------------------------------
        override fun observeActiveTemplate(): Flow<ChecklistTemplate?> = TODO()
        override fun observeDaySummaries(): Flow<List<CompletionRecord>> = TODO()
        override fun observeTaskOccurrences(): Flow<List<TaskOccurrence>> = TODO()
        override fun searchDates(query: String): Flow<List<LocalDate>> = TODO()
        override suspend fun getChecklistsInRange(start: LocalDate, end: LocalDate): List<DailyChecklist> = TODO()
        override suspend fun createChecklist(name: String, range: DateRange, drafts: List<TaskDraft>): Long = TODO()
    }

    /** A preferences repository exposing an onboarded state whose range covers [anchor]. */
    private class FakePreferencesRepository(anchor: LocalDate) : PreferencesRepository {
        private val prefs = UserPreferences(
            onboardingCompleted = true,
            themeMode = ThemeMode.SYSTEM,
            dateFormat = DateFormat.SYSTEM,
            weekStart = WeekStart.MONDAY,
            animationsEnabled = true,
            includeNotesInExport = true,
            scheduleStartDate = anchor.minusDays(1),
            scheduleEndDate = anchor.plusDays(1),
        )

        override val preferences: Flow<UserPreferences> = flowOf(prefs)

        override suspend fun completeOnboarding(startDate: LocalDate, endDate: LocalDate) = Unit
        override suspend fun setThemeMode(themeMode: ThemeMode) = Unit
        override suspend fun setDateFormat(dateFormat: DateFormat) = Unit
        override suspend fun setWeekStart(weekStart: WeekStart) = Unit
        override suspend fun setAnimationsEnabled(enabled: Boolean) = Unit
        override suspend fun setIncludeNotesInExport(enabled: Boolean) = Unit
        override suspend fun resetOnboarding() = Unit
    }
}
