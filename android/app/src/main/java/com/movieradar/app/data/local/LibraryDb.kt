package com.movieradar.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val key: String, // "movie-123" / "tv-456"
    val tmdbId: Int,
    val type: String,
    val title: String,
    val posterPath: String?,
    val year: String?,
    val addedAt: Long,
)

@Entity(tableName = "progress")
data class ProgressEntity(
    @PrimaryKey val identifier: String, // Internet Archive identifier
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
)

/** A title with 🔔 Notify me on. [knownProviderIds] = comma-separated services it was already on. */
@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val key: String, // "movie-123" / "tv-456"
    val tmdbId: Int,
    val type: String,
    val title: String,
    val posterPath: String?,
    val knownProviderIds: String,
    val createdAt: Long,
)

/** A title you've ✅ watched (any title, not only Watchlist ones). */
@Entity(tableName = "watched")
data class WatchedEntity(
    @PrimaryKey val key: String, // "movie-123" / "tv-456"
    val tmdbId: Int,
    val type: String,
    val title: String,
    val posterPath: String?,
    val year: String?,
    val watchedAt: Long,
)

@Dao
interface LibraryDao {
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun watchlist(): Flow<List<WatchlistEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE `key` = :key)")
    fun isSaved(key: String): Flow<Boolean>

    @Upsert suspend fun save(item: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE `key` = :key")
    suspend fun remove(key: String)

    /** Started but not finished (between 30 s in and 95 % through). */
    @Query("SELECT * FROM progress WHERE positionMs > 30000 AND (durationMs = 0 OR positionMs < durationMs * 0.95) ORDER BY updatedAt DESC")
    fun continueWatching(): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE identifier = :id")
    suspend fun progress(id: String): ProgressEntity?

    @Upsert suspend fun saveProgress(p: ProgressEntity)

    @Query("DELETE FROM progress WHERE identifier = :id")
    suspend fun removeProgress(id: String)

    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun alerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts")
    suspend fun alertsOnce(): List<AlertEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM alerts WHERE `key` = :key)")
    fun hasAlert(key: String): Flow<Boolean>

    @Upsert suspend fun saveAlert(a: AlertEntity)

    @Query("UPDATE alerts SET knownProviderIds = :ids WHERE `key` = :key")
    suspend fun updateKnown(key: String, ids: String)

    @Query("DELETE FROM alerts WHERE `key` = :key")
    suspend fun removeAlert(key: String)

    @Query("SELECT * FROM watched ORDER BY watchedAt DESC")
    fun watched(): Flow<List<WatchedEntity>>

    @Query("SELECT `key` FROM watched")
    fun watchedKeys(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM watched WHERE `key` = :key)")
    fun isWatched(key: String): Flow<Boolean>

    @Upsert suspend fun saveWatched(w: WatchedEntity)

    @Query("DELETE FROM watched WHERE `key` = :key")
    suspend fun removeWatched(key: String)

    /** ✅ Mark watched: add to Watched and take it off the Watchlist, atomically. */
    @Transaction
    suspend fun markWatched(w: WatchedEntity) {
        saveWatched(w)
        remove(w.key)
    }
}

@Database(entities = [WatchlistEntity::class, ProgressEntity::class, AlertEntity::class, WatchedEntity::class], version = 3, exportSchema = false)
abstract class LibraryDb : RoomDatabase() {
    abstract fun dao(): LibraryDao

    companion object {
        /** v1 → v2 adds the alerts table; watchlist and progress are untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `alerts` (`key` TEXT NOT NULL, `tmdbId` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `posterPath` TEXT, `knownProviderIds` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`key`))",
                )
            }
        }

        /** v2 → v3 adds the watched table; everything else is untouched. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `watched` (`key` TEXT NOT NULL, `tmdbId` INTEGER NOT NULL, `type` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `posterPath` TEXT, `year` TEXT, `watchedAt` INTEGER NOT NULL, PRIMARY KEY(`key`))",
                )
            }
        }
    }
}
