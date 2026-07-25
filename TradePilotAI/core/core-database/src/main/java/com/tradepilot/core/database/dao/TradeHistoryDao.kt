package com.tradepilot.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tradepilot.core.database.entity.TradeHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TradeHistoryEntity): Long

    @Query("SELECT * FROM trade_history ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TradeHistoryEntity>>

    @Query("SELECT COUNT(*) FROM trade_history")
    suspend fun count(): Int
}
