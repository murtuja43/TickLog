package com.ticklog.data.database.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.ticklog.data.database.entity.DailyChecklistEntity
import com.ticklog.data.database.entity.DailyChecklistItemEntity

/**
 * A daily checklist together with all of its tasks, resolved by Room in a single
 * observed query.
 *
 * This is the natural read shape for the Home screen (one day + its items) and
 * keeps the join logic in the persistence layer instead of leaking manual
 * stitching into ViewModels.
 */
data class DailyChecklistWithItems(
    @Embedded
    val checklist: DailyChecklistEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "daily_checklist_id",
    )
    val items: List<DailyChecklistItemEntity>,
)
