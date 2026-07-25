package com.tradepilot.data.trading

import com.tradepilot.core.database.dao.TradeHistoryDao
import com.tradepilot.domain.model.TradeEntry
import com.tradepilot.domain.repository.TradeJournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Fase 5: implementasi nyata memakai Room (menggantikan in-memory Fase 0).
 * Semua data tersimpan lokal sesuai requirement versi 3.
 */
class TradeJournalRepositoryImpl @Inject constructor(
    private val dao: TradeHistoryDao
) : TradeJournalRepository {

    override suspend fun save(entry: TradeEntry) {
        dao.insert(entry.toEntity())
    }

    override fun observeHistory(): Flow<List<TradeEntry>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }
}
