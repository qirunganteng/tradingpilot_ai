package com.tradepilot.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tradepilot.core.database.entity.NotificationLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationLogDao {
    @Insert
    suspend fun insert(entity: NotificationLogEntity): Long

    @Query("SELECT * FROM notification_log ORDER BY timestamp DESC LIMIT 200")
    fun observeRecent(): Flow<List<NotificationLogEntity>>
}
