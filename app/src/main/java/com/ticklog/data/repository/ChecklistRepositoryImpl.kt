package com.ticklog.data.repository

import com.ticklog.data.database.dao.ChecklistTemplateDao
import com.ticklog.data.database.dao.DailyChecklistDao
import com.ticklog.data.model.toDomain
import com.ticklog.domain.model.ChecklistTemplate
import com.ticklog.domain.model.DailyChecklist
import com.ticklog.domain.repository.ChecklistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDate

/**
 * Room-backed implementation of [ChecklistRepository] (read paths for Phase 1).
 *
 * [observeChecklistForDate] composes two reactive sources: it follows the active
 * template and, whenever that changes, switches to observing the requested day
 * under it. In Phase 1 there is no template yet, so both streams naturally emit
 * null and the Home screen renders its empty state — exercising the full
 * data → domain → UI path end to end.
 */
@Singleton
class ChecklistRepositoryImpl @Inject constructor(
    private val templateDao: ChecklistTemplateDao,
    private val dailyChecklistDao: DailyChecklistDao,
) : ChecklistRepository {

    override fun observeActiveTemplate(): Flow<ChecklistTemplate?> =
        templateDao.observeActiveTemplate().map { it?.toDomain() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeChecklistForDate(date: LocalDate): Flow<DailyChecklist?> =
        templateDao.observeActiveTemplate().flatMapLatest { template ->
            if (template == null) {
                flowOf(null)
            } else {
                dailyChecklistDao
                    .observeChecklistWithItems(templateId = template.id, date = date)
                    .map { relation -> relation?.toDomain() }
            }
        }
}
