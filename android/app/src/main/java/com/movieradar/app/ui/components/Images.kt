package com.movieradar.app.ui.components

object TmdbImage {
    private const val BASE = "https://image.tmdb.org/t/p/"
    fun poster(path: String?) = path?.let { "${BASE}w342$it" }
    fun backdrop(path: String?) = path?.let { "${BASE}w1280$it" }
    fun logo(path: String?) = path?.let { "${BASE}w92$it" }
    fun profile(path: String?) = path?.let { "${BASE}w185$it" }
}
