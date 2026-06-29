package com.ticklog.data.repository

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ticklog.data.database.TickLogDatabase
import com.ticklog.domain.model.DateRange
import com.ticklog.domain.model.TaskDraft
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
 * Repository read-path tests (day summaries, task occurrences and search) against
 * a real in-memory Room database via Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ChecklistRepositoryReadsTest {

    private lateinit var database: TickLogDatabase
    private lateinit var repository: ChecklistRepositoryImpl

    private val base: LocalDate = LocalDate.of(2026, 1, 10)
    private fun day(offset: Int) = base.plusDays(offset.toLong())

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
    fun tearDown() = database.close()

    private suspend fun seed() {
        repository.createChecklist(
            name = "My Checklist",
            range = DateRange(day(0), day(2)), // 3 days
            drafts = listOf(
                TaskDraft.from("Apple", "drink water")!!,
                TaskDraft.from("Banana", null)!!,
            ),
        )
    }

    @Test
    fun `day summaries aggregate totals and completions per day`() = runTest {
        seed()
        // Complete "Apple" on the middle day only.
        val apple = repository.observeChecklistForDate(day(1)).first()!!.tasks.first { it.title == "Apple" }
        repository.setTaskCompleted(apple.id, true)

        val summaries = repository.observeDaySummaries().first().associateBy { it.date }

        assertThat(summaries.keys).containsExactly(day(0), day(1), day(2))
        assertThat(summaries.getValue(day(0)).totalItems).isEqualTo(2)
        assertThat(summaries.getValue(day(0)).completedItems).isEqualTo(0)
        assertThat(summaries.getValue(day(1)).completedItems).isEqualTo(1)
        assertThat(summaries.getValue(day(2)).completedItems).isEqualTo(0)
    }

    @Test
    fun `search matches task titles and notes, returning each matching day once`() = runTest {
        seed()

        assertThat(repository.searchDates("Banana").first())
            .containsExactly(day(0), day(1), day(2))
        // "drink water" is Apple's note on every day.
        assertThat(repository.searchDates("water").first())
            .containsExactly(day(0), day(1), day(2))
        assertThat(repository.searchDates("nothing-here").first()).isEmpty()
        assertThat(repository.searchDates("   ").first()).isEmpty()
    }

    @Test
    fun `task occurrences expose one linked row per task per day`() = runTest {
        seed()

        val occurrences = repository.observeTaskOccurrences().first()

        assertThat(occurrences).hasSize(6) // 2 tasks x 3 days
        assertThat(occurrences.map { it.sourceItemId }.toSet()).hasSize(2)
        assertThat(occurrences.all { it.isCompleted }).isFalse()
    }
}
