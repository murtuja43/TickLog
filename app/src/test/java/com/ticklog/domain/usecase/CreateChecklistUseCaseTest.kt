package com.ticklog.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.ticklog.domain.model.ChecklistTemplate
import com.ticklog.domain.model.CompletionRecord
import com.ticklog.domain.model.DailyChecklist
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [CreateChecklistUseCase] — in particular that an over-long range
 * is rejected before any (potentially huge) day generation runs.
 */
class CreateChecklistUseCaseTest {

    private val start: LocalDate = LocalDate.of(2026, 1, 1)
    private val drafts = listOf(TaskDraft.from("Read", null)!!)

    @Test
    fun `a range longer than the maximum is rejected without generating anything`() = runBlocking {
        val repository = RecordingChecklistRepository()
        val useCase = CreateChecklistUseCase(repository, RecordingPreferencesRepository())

        val overLong = DateRange(start, start.plusDays(DateRange.MAX_TRACKING_DAYS)) // MAX + 1 days
        val result = useCase(overLong, drafts)

        assertThat(result.isFailure).isTrue()
        assertThat(repository.createChecklistCalls).isEqualTo(0)
    }

    @Test
    fun `a range at the maximum length is created`() = runBlocking {
        val repository = RecordingChecklistRepository()
        val prefs = RecordingPreferencesRepository()
        val useCase = CreateChecklistUseCase(repository, prefs)

        val maxRange = DateRange(start, start.plusDays(DateRange.MAX_TRACKING_DAYS - 1))
        val result = useCase(maxRange, drafts)

        assertThat(result.isSuccess).isTrue()
        assertThat(repository.createChecklistCalls).isEqualTo(1)
        assertThat(prefs.onboardingCompletedCalls).isEqualTo(1)
    }

    @Test
    fun `an empty task list is rejected`() = runBlocking {
        val repository = RecordingChecklistRepository()
        val useCase = CreateChecklistUseCase(repository, RecordingPreferencesRepository())

        val result = useCase(DateRange(start, start.plusDays(2)), emptyList())

        assertThat(result.isFailure).isTrue()
        assertThat(repository.createChecklistCalls).isEqualTo(0)
    }

    private class RecordingChecklistRepository : ChecklistRepository {
        var createChecklistCalls = 0
            private set

        override suspend fun createChecklist(
            name: String,
            range: DateRange,
            drafts: List<TaskDraft>,
        ): Long {
            createChecklistCalls++
            return 1L
        }

        // --- Unused --------------------------------------------------------
        override fun observeActiveTemplate(): Flow<ChecklistTemplate?> = flowOf(null)
        override fun observeChecklistForDate(date: LocalDate): Flow<DailyChecklist?> = flowOf(null)
        override fun observeDaySummaries(): Flow<List<CompletionRecord>> = flowOf(emptyList())
        override fun observeTaskOccurrences(): Flow<List<TaskOccurrence>> = flowOf(emptyList())
        override fun searchDates(query: String): Flow<List<LocalDate>> = flowOf(emptyList())
        override suspend fun getChecklistsInRange(start: LocalDate, end: LocalDate): List<DailyChecklist> = emptyList()
        override suspend fun setTaskCompleted(dailyTaskId: Long, completed: Boolean) = Unit
        override suspend fun addTask(date: LocalDate, draft: TaskDraft, scope: TaskScope) = Unit
        override suspend fun renameTask(dailyTaskId: Long, newTitle: String, scope: TaskScope) = Unit
        override suspend fun updateTaskNote(dailyTaskId: Long, newNote: String?, scope: TaskScope) = Unit
        override suspend fun duplicateTask(dailyTaskId: Long): Long = 0L
        override suspend fun deleteTask(dailyTaskId: Long, scope: TaskScope): List<DeletedTask> = emptyList()
        override suspend fun restoreTasks(deleted: List<DeletedTask>) = Unit
    }

    private class RecordingPreferencesRepository : PreferencesRepository {
        var onboardingCompletedCalls = 0
            private set

        override val preferences: Flow<UserPreferences> = flowOf(UserPreferences.DEFAULT)

        override suspend fun completeOnboarding(startDate: LocalDate, endDate: LocalDate) {
            onboardingCompletedCalls++
        }

        override suspend fun setThemeMode(themeMode: ThemeMode) = Unit
        override suspend fun setDateFormat(dateFormat: DateFormat) = Unit
        override suspend fun setWeekStart(weekStart: WeekStart) = Unit
        override suspend fun setAnimationsEnabled(enabled: Boolean) = Unit
        override suspend fun setIncludeNotesInExport(enabled: Boolean) = Unit
        override suspend fun resetOnboarding() = Unit
    }
}
