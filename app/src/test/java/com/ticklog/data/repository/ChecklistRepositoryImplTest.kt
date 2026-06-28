package com.ticklog.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ticklog.data.database.TickLogDatabase
import com.ticklog.domain.model.DailyTask
import com.ticklog.domain.model.DateRange
import com.ticklog.domain.model.TaskDraft
import com.ticklog.domain.model.TaskScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Repository tests for the checklist engine, run against a real (in-memory) Room
 * database via Robolectric. These exercise the actual generated DAO code and the
 * transactional, history-preserving logic — date generation, task generation,
 * rename/delete scoping, undo, add and duplicate.
 *
 * A plain [Application] is used (not the @HiltAndroidApp app) so the tests stay
 * independent of dependency injection.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChecklistRepositoryImplTest {

    private lateinit var database: TickLogDatabase
    private lateinit var repository: ChecklistRepositoryImpl

    private val base: LocalDate = LocalDate.of(2026, 1, 10)
    private fun day(offset: Int): LocalDate = base.plusDays(offset.toLong())

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TickLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ChecklistRepositoryImpl(
            database = database,
            templateDao = database.checklistTemplateDao(),
            checklistItemDao = database.checklistItemDao(),
            dailyChecklistDao = database.dailyChecklistDao(),
            dailyChecklistItemDao = database.dailyChecklistItemDao(),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- Generation ---------------------------------------------------------

    @Test
    fun `createChecklist generates one independent day per date with copies of every item`() =
        runTest {
            seed(dayCount = 3, "A", "B")

            assertThat(titlesOn(day(0))).containsExactly("A", "B").inOrder()
            assertThat(titlesOn(day(1))).containsExactly("A", "B").inOrder()
            assertThat(titlesOn(day(2))).containsExactly("A", "B").inOrder()
            // A day outside the range has no checklist at all.
            assertThat(tasksOn(day(3))).isEmpty()
        }

    @Test
    fun `each generated day is independent - completing one does not affect another`() = runTest {
        seed(dayCount = 2, "A")
        val taskOnDay0 = taskOn(day(0), "A")

        repository.setTaskCompleted(taskOnDay0.id, true)

        assertThat(taskOn(day(0), "A").isCompleted).isTrue()
        assertThat(taskOn(day(1), "A").isCompleted).isFalse()
    }

    // --- Completion ---------------------------------------------------------

    @Test
    fun `setTaskCompleted persists and toggles back`() = runTest {
        seed(dayCount = 1, "A")
        val task = taskOn(day(0), "A")

        repository.setTaskCompleted(task.id, true)
        assertThat(taskOn(day(0), "A").isCompleted).isTrue()

        repository.setTaskCompleted(task.id, false)
        assertThat(taskOn(day(0), "A").isCompleted).isFalse()
    }

    // --- Rename -------------------------------------------------------------

    @Test
    fun `renameTask today-only changes only that day`() = runTest {
        seed(dayCount = 3, "A", "B")

        repository.renameTask(taskOn(day(1), "A").id, "A!", TaskScope.TODAY_ONLY)

        assertThat(titlesOn(day(0))).contains("A")
        assertThat(titlesOn(day(1))).contains("A!")
        assertThat(titlesOn(day(2))).contains("A")
    }

    @Test
    fun `renameTask future changes this day and later days but never the past`() = runTest {
        seed(dayCount = 3, "A", "B")

        repository.renameTask(taskOn(day(1), "A").id, "A-future", TaskScope.TODAY_AND_FUTURE)

        assertThat(titlesOn(day(0))).contains("A") // past untouched
        assertThat(titlesOn(day(1))).contains("A-future")
        assertThat(titlesOn(day(2))).contains("A-future")
    }

    // --- Delete + undo ------------------------------------------------------

    @Test
    fun `deleteTask today-only removes one row and restore brings it back`() = runTest {
        seed(dayCount = 3, "A", "B")

        val removed = repository.deleteTask(taskOn(day(1), "A").id, TaskScope.TODAY_ONLY)

        assertThat(removed).hasSize(1)
        assertThat(titlesOn(day(1))).containsExactly("B")
        assertThat(titlesOn(day(0))).contains("A")

        repository.restoreTasks(removed)
        assertThat(titlesOn(day(1))).contains("A")
    }

    @Test
    fun `deleteTask future removes from this day onward, preserves the past, and restores`() =
        runTest {
            seed(dayCount = 3, "A", "B")

            val removed = repository.deleteTask(taskOn(day(1), "A").id, TaskScope.TODAY_AND_FUTURE)

            assertThat(removed).hasSize(2) // day 1 and day 2
            assertThat(titlesOn(day(0))).contains("A") // past untouched
            assertThat(titlesOn(day(1))).doesNotContain("A")
            assertThat(titlesOn(day(2))).doesNotContain("A")

            repository.restoreTasks(removed)
            assertThat(titlesOn(day(1))).contains("A")
            assertThat(titlesOn(day(2))).contains("A")
        }

    // --- Add ----------------------------------------------------------------

    @Test
    fun `addTask today-only adds to that day only`() = runTest {
        seed(dayCount = 3, "A")

        repository.addTask(day(1), draft("C"), TaskScope.TODAY_ONLY)

        assertThat(titlesOn(day(0))).doesNotContain("C")
        assertThat(titlesOn(day(1))).contains("C")
        assertThat(titlesOn(day(2))).doesNotContain("C")
    }

    @Test
    fun `addTask future adds to this day and later days only`() = runTest {
        seed(dayCount = 3, "A")

        repository.addTask(day(1), draft("D"), TaskScope.TODAY_AND_FUTURE)

        assertThat(titlesOn(day(0))).doesNotContain("D")
        assertThat(titlesOn(day(1))).contains("D")
        assertThat(titlesOn(day(2))).contains("D")
    }

    // --- Duplicate ----------------------------------------------------------

    @Test
    fun `duplicateTask adds a second copy on the same day only`() = runTest {
        seed(dayCount = 2, "A")

        repository.duplicateTask(taskOn(day(0), "A").id)

        assertThat(titlesOn(day(0)).count { it == "A" }).isEqualTo(2)
        assertThat(titlesOn(day(1)).count { it == "A" }).isEqualTo(1)
    }

    // --- Helpers ------------------------------------------------------------

    private suspend fun seed(dayCount: Int, vararg titles: String) {
        repository.createChecklist(
            name = "My Checklist",
            range = DateRange(day(0), day(dayCount - 1)),
            drafts = titles.map { draft(it) },
        )
    }

    private fun draft(title: String): TaskDraft = TaskDraft.from(title, null)!!

    private suspend fun tasksOn(date: LocalDate): List<DailyTask> =
        repository.observeChecklistForDate(date).first()?.tasks ?: emptyList()

    private suspend fun titlesOn(date: LocalDate): List<String> = tasksOn(date).map { it.title }

    private suspend fun taskOn(date: LocalDate, title: String): DailyTask =
        tasksOn(date).first { it.title == title }
}
