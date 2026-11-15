package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetDao {
    @Query("SELECT * FROM assets ORDER BY scoreTotal DESC, updatedAtMillis DESC")
    fun observeAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets ORDER BY scoreTotal DESC, updatedAtMillis DESC")
    suspend fun getAllAssets(): List<AssetEntity>

    @Query("SELECT * FROM assets WHERE uid = :uid LIMIT 1")
    suspend fun getAssetByUid(uid: String): AssetEntity?

    @Query("SELECT * FROM assets WHERE boundGameSlot IS NOT NULL")
    fun observeBoundAssets(): Flow<List<AssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(asset: AssetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(assets: List<AssetEntity>)

    @Query("UPDATE assets SET boundGameSlot = NULL WHERE boundGameSlot = :slot")
    suspend fun clearBindingForSlot(slot: String)

    @Query("DELETE FROM assets WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}
