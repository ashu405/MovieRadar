package com.movieradar.app

import com.movieradar.app.data.local.AlertEntity
import com.movieradar.app.data.local.LibraryDao
import com.movieradar.app.data.local.LibraryDb
import com.movieradar.app.data.local.LibraryRepository
import com.movieradar.app.data.local.ProgressEntity
import com.movieradar.app.data.local.WatchedEntity
import com.movieradar.app.data.local.WatchlistEntity
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.TitleSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory DAO; [markWatched] is NOT overridden, so the real interface logic runs. */
private class FakeDao : LibraryDao {
    val list = MutableStateFlow<Map<String, WatchlistEntity>>(emptyMap())
    val seen = MutableStateFlow<Map<String, WatchedEntity>>(emptyMap())

    override fun watchlist(): Flow<List<WatchlistEntity>> = list.map { it.values.sortedByDescending { e -> e.addedAt } }
    override fun isSaved(key: String): Flow<Boolean> = list.map { key in it }
    override suspend fun save(item: WatchlistEntity) { list.value += item.key to item }
    override suspend fun remove(key: String) { list.value -= key }
    override fun watched(): Flow<List<WatchedEntity>> = seen.map { it.values.sortedByDescending { e -> e.watchedAt } }
    override fun watchedKeys(): Flow<List<String>> = seen.map { it.keys.toList() }
    override fun isWatched(key: String): Flow<Boolean> = seen.map { key in it }
    override suspend fun saveWatched(w: WatchedEntity) { seen.value += w.key to w }
    override suspend fun removeWatched(key: String) { seen.value -= key }

    override fun continueWatching(): Flow<List<ProgressEntity>> = MutableStateFlow(emptyList())
    override suspend fun progress(id: String): ProgressEntity? = null
    override suspend fun saveProgress(p: ProgressEntity) {}
    override suspend fun removeProgress(id: String) {}
    override fun alerts(): Flow<List<AlertEntity>> = MutableStateFlow(emptyList())
    override suspend fun alertsOnce(): List<AlertEntity> = emptyList()
    override fun hasAlert(key: String): Flow<Boolean> = MutableStateFlow(false)
    override suspend fun saveAlert(a: AlertEntity) {}
    override suspend fun updateKnown(key: String, ids: String) {}
    override suspend fun removeAlert(key: String) {}
}

class WatchedTest {
    private val dune = TitleSummary(438631, MediaType.MOVIE, "Dune", "2021", "/p.jpg", null, "", 0.0, emptyList())
    private val got = TitleSummary(1399, MediaType.TV, "Game of Thrones", "2011", null, null, "", 0.0, emptyList())

    @Test fun `marking watched moves a title off the watchlist`() = runTest {
        val dao = FakeDao(); val repo = LibraryRepository(dao)
        repo.setSaved(dune, true)
        repo.setWatched(dune, true)
        assertFalse(repo.isSaved(dune).first())
        assertTrue(repo.isWatched(dune).first())
        assertEquals(listOf("Dune"), repo.watched.first().map { it.title })
    }

    @Test fun `any title can be marked watched, and unmarking does not re-add it to the watchlist`() = runTest {
        val repo = LibraryRepository(FakeDao())
        repo.setWatched(got, true)
        assertEquals(setOf("tv-1399"), repo.watchedKeys.first())
        repo.setWatched(got, false)
        assertTrue(repo.watchedKeys.first().isEmpty())
        assertFalse(repo.isSaved(got).first())
    }

    @Test fun `watched keeps the type and poster for the badge and library`() = runTest {
        val repo = LibraryRepository(FakeDao())
        repo.setWatched(dune, true)
        val back = repo.watched.first().single()
        assertEquals(MediaType.MOVIE, back.type)
        assertEquals("/p.jpg", back.posterPath)
        assertEquals("2021", back.year)
    }

    @Test fun `migration 2 to 3 only adds the watched table`() {
        assertEquals(2, LibraryDb.MIGRATION_2_3.startVersion)
        assertEquals(3, LibraryDb.MIGRATION_2_3.endVersion)
    }
}
