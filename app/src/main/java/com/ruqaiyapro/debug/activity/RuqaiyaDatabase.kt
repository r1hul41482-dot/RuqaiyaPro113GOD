package com.ruqaiyapro.debug.activity

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ruqaiyapro.debug.feedback.FeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity): Long

    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 150")
    fun getAllLogs(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE eventType = :type ORDER BY timestamp DESC LIMIT 100")
    fun getLogsByType(type: String): Flow<List<ActivityLogEntity>>

    @Query("DELETE FROM activity_logs")
    suspend fun clearAll()
}

@Dao
interface FeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackEntity): Long

    @Query("SELECT * FROM user_feedbacks ORDER BY timestamp DESC LIMIT 50")
    fun getAllFeedbacks(): Flow<List<FeedbackEntity>>

    @Query("SELECT * FROM user_feedbacks WHERE rating = 1 ORDER BY timestamp DESC LIMIT 10")
    suspend fun getTopLikedPrompts(): List<FeedbackEntity>

    @Query("SELECT * FROM user_feedbacks WHERE rating = -1 ORDER BY timestamp DESC LIMIT 10")
    suspend fun getDislikedPrompts(): List<FeedbackEntity>
}

@Dao
interface KhataDao {
    @Insert
    suspend fun insert(entry: KhataEntity): Long

    @Query("SELECT * FROM khata ORDER BY timestamp DESC")
    fun all(): Flow<List<KhataEntity>>

    @Query("DELETE FROM khata WHERE id = :id")
    suspend fun delete(id: Long)
}

@Database(entities = [ActivityLogEntity::class, FeedbackEntity::class, KhataEntity::class], version = 2, exportSchema = false)
abstract class RuqaiyaDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityDao
    abstract fun feedbackDao(): FeedbackDao
    abstract fun khataDao(): KhataDao

    companion object {
        @Volatile
        private var INSTANCE: RuqaiyaDatabase? = null

        fun getDatabase(context: Context): RuqaiyaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RuqaiyaDatabase::class.java,
                    "ruqaiya_god.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
