package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val ipAddress: String,
    val port: Int,
    val defaultTitleId: String,
    val manualRemotePath: String,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "transfer_logs")
data class TransferLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val ipAddress: String,
    val titleId: String,
    val fileName: String,
    val destinationPath: String,
    val sizeBytes: Long,
    val status: String, // "SUCCESS" or "FAILED"
    val errorMessage: String? = null
)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY lastUsedTimestamp DESC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)
}

@Dao
interface TransferLogDao {
    @Query("SELECT * FROM transfer_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<TransferLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TransferLogEntity): Long

    @Query("DELETE FROM transfer_logs")
    suspend fun clearLogs()
}

@Database(entities = [ProfileEntity::class, TransferLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun transferLogDao(): TransferLogDao
}
