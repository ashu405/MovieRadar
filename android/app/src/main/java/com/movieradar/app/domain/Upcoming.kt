package com.movieradar.app.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException

data class ReleaseDay(val date: LocalDate, val titles: List<TitleSummary>)

object UpcomingRules {
    /** Keeps titles released inside [month], de-duplicated, grouped by day in date order. */
    fun groupByDay(month: YearMonth, titles: List<TitleSummary>): List<ReleaseDay> =
        titles.distinctBy { it.key }
            .mapNotNull { t -> parse(t.releaseDate)?.takeIf { YearMonth.from(it) == month }?.let { it to t } }
            .groupBy({ it.first }, { it.second })
            .toSortedMap()
            .map { (day, list) -> ReleaseDay(day, list) }

    private fun parse(s: String?): LocalDate? = try {
        s?.let(LocalDate::parse)
    } catch (_: DateTimeParseException) { null }
}
