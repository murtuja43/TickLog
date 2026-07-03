package com.ticklog.domain.usecase

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ticklog.data.database.TickLogDatabase
import com.ticklog.data.repository.ChecklistRepositoryImpl
import com.ticklog.domain.model.DateRange
import com.ticklog.domain.model.ReportScope
import com.ticklog.domain.model.TaskDraft
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/**
 * Tests the PDF report's data assembly (scope resolution, day filtering and
 * summary statistics) against a real in-memory Room database. The Canvas
 * rendering itself is verified on-device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BuildReportDataUseCaseTest {

    private lateinit var database: TickLogDatabase
    private lateinit var buildReportData: BuildReportDataUseCase

    private val base: LocalDate = LocalDate.of(2026, 3, 10)
    private fun day(offset: Int) = base.plusDays(offset.toLong())

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TickLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = ChecklistRepositoryImpl(
            database, database.checklistTemplateDao(), database.checklistItemDao(),
            database.dailyChecklistDao(), database.dailyChecklistItemDao(),
        )
        buildReportData = BuildReportDataUseCase(repository)
    }

    @After
    fun tearDown() = database.close()

    private suspend fun seed() {
        ChecklistRepositoryImpl(
            database, database.checklistTemplateDao(), database.checklistItemDao(),
            database.dailyChecklistDao(), database.dailyChecklistItemDao(),
        ).createChecklist(
            name = "My Checklist",
            range = DateRange(day(0), day(4)), // 5 days
            drafts = listOf(TaskDraft.from("A", null)!!, TaskDraft.from("B", null)!!),
        )
    }

    @Test
    fun `single day scope yields exactly that day`() = runTest {
        seed()
        val data = buildReportData(ReportScope.SingleDay(day(1)))

        assertThat(data.days).hasSize(1)
        assertThat(data.days.single().date).isEqualTo(day(1))
        assertThat(data.rangeStart).isEqualTo(day(1))
        assertThat(data.rangeEnd).isEqualTo(day(1))
    }

    @Test
    fun `entire history spans every generated day with matching statistics`() = runTest {
        seed()
        val data = buildReportData(ReportScope.EntireHistory)

        assertThat(data.days).hasSize(5)
        assertThat(data.rangeStart).isEqualTo(day(0))
        assertThat(data.rangeEnd).isEqualTo(day(4))
        assertThat(data.statistics.totalChecklistDays).isEqualTo(5)
        assertThat(data.statistics.totalCompletedTasks).isEqualTo(0)
    }

    @Test
    fun `month scope only includes days in that month`() = runTest {
        seed()
        val data = buildReportData(ReportScope.Month(YearMonth.of(2026, 3)))

        assertThat(data.days).hasSize(5)
        assertThat(data.days.all { it.date.month == java.time.Month.MARCH }).isTrue()
    }

    @Test
    fun `a scope with no data produces an empty report`() = runTest {
        seed()
        val data = buildReportData(ReportScope.Year(2020))

        assertThat(data.isEmpty).isTrue()
        assertThat(data.days).isEmpty()
    }
}
