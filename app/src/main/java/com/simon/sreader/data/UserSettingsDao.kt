package com.simon.sreader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {

    /** 获取用户设置（Flow 实时观察） */
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getSettings(): Flow<UserSettings?>

    /** 获取用户设置（一次性） */
    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettingsOnce(): UserSettings?

    /** 插入或更新设置 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: UserSettings)
}
