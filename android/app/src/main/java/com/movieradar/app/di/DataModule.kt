package com.movieradar.app.di

import android.content.Context
import androidx.room.Room
import com.movieradar.app.data.archive.ArchiveApi
import com.movieradar.app.data.local.LibraryDao
import com.movieradar.app.data.local.LibraryDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides @Singleton
    fun libraryDb(@ApplicationContext context: Context): LibraryDb =
        Room.databaseBuilder(context, LibraryDb::class.java, "library.db")
            .addMigrations(LibraryDb.MIGRATION_1_2, LibraryDb.MIGRATION_2_3)
            .build()

    @Provides fun libraryDao(db: LibraryDb): LibraryDao = db.dao()

    /**
     * Separate client for archive.org: it must NOT carry the proxy's X-App-Token header,
     * so it's built fresh instead of reusing the proxy client.
     */
    @Provides @Singleton
    fun archiveApi(json: Json): ArchiveApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .addInterceptor(RetryOnNetworkError(maxRetries = 2))
            .build()
        return Retrofit.Builder()
            .baseUrl("https://archive.org/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ArchiveApi::class.java)
    }
}
