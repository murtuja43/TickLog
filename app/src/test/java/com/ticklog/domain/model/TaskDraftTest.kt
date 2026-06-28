package com.ticklog.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [TaskDraft.from] — the single point of task-input validation.
 */
class TaskDraftTest {

    @Test
    fun `trims surrounding whitespace from title and note`() {
        val draft = TaskDraft.from("  Drink water  ", "  two litres  ")

        assertThat(draft).isNotNull()
        assertThat(draft!!.title).isEqualTo("Drink water")
        assertThat(draft.note).isEqualTo("two litres")
    }

    @Test
    fun `a blank title is rejected`() {
        assertThat(TaskDraft.from("", null)).isNull()
        assertThat(TaskDraft.from("    ", "note")).isNull()
    }

    @Test
    fun `a blank or empty note becomes null`() {
        assertThat(TaskDraft.from("Title", "")?.note).isNull()
        assertThat(TaskDraft.from("Title", "   ")?.note).isNull()
        assertThat(TaskDraft.from("Title", null)?.note).isNull()
    }

    @Test
    fun `a title at the limit is accepted but over the limit is rejected`() {
        val atLimit = "a".repeat(MAX_TASK_TITLE_LENGTH)
        val overLimit = "a".repeat(MAX_TASK_TITLE_LENGTH + 1)

        assertThat(TaskDraft.from(atLimit, null)).isNotNull()
        assertThat(TaskDraft.from(overLimit, null)).isNull()
    }

    @Test
    fun `a note over the limit is rejected`() {
        val overLimit = "a".repeat(MAX_TASK_NOTE_LENGTH + 1)

        assertThat(TaskDraft.from("Title", overLimit)).isNull()
    }
}
