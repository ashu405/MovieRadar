package com.movieradar.app.ui.upcoming

import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import com.movieradar.app.ui.components.rememberNotifyToggle
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.movieradar.app.domain.OttService
import com.movieradar.app.domain.Provider
import com.movieradar.app.ui.components.ProviderLogo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movieradar.app.data.UpcomingTab
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.key
import com.movieradar.app.ui.components.ErrorState
import com.movieradar.app.ui.components.Loadable
import com.movieradar.app.ui.components.MetaLine
import com.movieradar.app.ui.components.PosterCard
import com.movieradar.app.ui.theme.RadarColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MonthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val DayFmt = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

@Composable
fun UpcomingScreen(onOpen: (TitleSummary) -> Unit, vm: UpcomingViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().background(RadarColors.Black).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::previousMonth) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous month") }
            Text(
                s.month.format(MonthFmt),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).clickable(onClick = vm::thisMonth),
            )
            if (s.month != YearMonth.now()) {
                Text("Today", color = RadarColors.Accent, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable(onClick = vm::thisMonth).padding(8.dp))
            }
            IconButton(onClick = vm::nextMonth) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next month") }
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UpcomingTab.entries.forEach { t ->
                FilterChip(
                    selected = s.tab == t,
                    onClick = { vm.setTab(t) },
                    label = { Text(t.label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RadarColors.TextPrimary, selectedLabelColor = Color.Black),
                )
            }
        }
        if (s.tab == UpcomingTab.OTT) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = s.service == null, onClick = { vm.setService(null) }, label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RadarColors.Accent, selectedLabelColor = Color.White),
                    )
                }
                items(OttService.entries) { svc ->
                    FilterChip(
                        selected = s.service == svc,
                        onClick = { vm.setService(if (s.service == svc) null else svc) },
                        label = { Text(svc.label) },
                        leadingIcon = { ServiceLogo(svc, s.logos[svc], 20.dp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RadarColors.Accent, selectedLabelColor = Color.White),
                    )
                }
            }
        }
        when (val d = s.days) {
            Loadable.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = RadarColors.Accent)
            }
            is Loadable.Failed -> ErrorState(d.message, vm::retry)
            is Loadable.Ready -> if (d.value.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        s.service?.let { "Nothing found on ${it.label} this month." } ?: "No releases found for this month.",
                        color = RadarColors.TextSecondary,
                    )
                }
            } else LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                d.value.forEach { day ->
                    item(key = "h-${day.date}") { DayHeader(day.date) }
                    items(day.titles, key = { "${day.date}-${it.key}" }) { t ->
                        ReleaseRow(
                            t, saved = t.key in s.savedKeys, onOpen = { onOpen(t) }, onToggle = { vm.toggleSaved(t) },
                            services = if (s.tab == UpcomingTab.OTT) s.releases.servicesOf(t) else emptySet(),
                            logos = s.logos,
                            alertOn = t.key in s.alertKeys,
                            onAlertChange = { on -> vm.setAlert(t, on) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(date: LocalDate) {
    val today = date == LocalDate.now()
    Text(
        if (today) "Today · ${date.format(DayFmt)}" else date.format(DayFmt),
        style = MaterialTheme.typography.titleMedium,
        color = if (today) RadarColors.Accent else RadarColors.TextPrimary,
        modifier = Modifier.fillMaxWidth().background(RadarColors.Black).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun ReleaseRow(
    t: TitleSummary,
    saved: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    services: Set<OttService> = emptySet(),
    logos: Map<OttService, String?> = emptyMap(),
    alertOn: Boolean = false,
    onAlertChange: (Boolean) -> Unit = {},
) {
    val toggleAlert = rememberNotifyToggle(alertOn, onAlertChange)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterCard(t, onClick = onOpen, width = 56.dp)
        Column(Modifier.weight(1f)) {
            Text(t.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            MetaLine(listOfNotNull(if (t.type == MediaType.TV) "Series" else "Movie", if (t.rating > 0) "★ %.1f".format(t.rating) else null))
            if (services.isNotEmpty()) {
                Row(
                    Modifier.padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    services.sortedBy { it.ordinal }.forEach { svc ->
                        ServiceLogo(svc, logos[svc], 18.dp)
                        Text(svc.label, style = MaterialTheme.typography.labelMedium, color = RadarColors.TextPrimary)
                    }
                }
            }
            if (t.overview.isNotBlank()) {
                Text(t.overview, style = MaterialTheme.typography.bodyMedium, color = RadarColors.TextSecondary,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        IconButton(onClick = toggleAlert, modifier = Modifier.width(40.dp)) {
            Icon(
                if (alertOn) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone,
                contentDescription = if (alertOn) "Stop streaming alert" else "Notify me when streaming",
                tint = if (alertOn) RadarColors.Accent else RadarColors.TextSecondary,
            )
        }
        IconButton(onClick = onToggle, modifier = Modifier.width(40.dp)) {
            Icon(
                if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = if (saved) "Remove from watchlist" else "Add to watchlist",
                tint = if (saved) RadarColors.Accent else RadarColors.TextSecondary,
            )
        }
    }
}

/** Service logo from TMDB, or a small initial badge if TMDB has none for this region. */
@Composable
private fun ServiceLogo(svc: OttService, logoPath: String?, size: androidx.compose.ui.unit.Dp) {
    if (logoPath != null) {
        ProviderLogo(Provider(svc.providerId, svc.label, logoPath), size = size)
    } else {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(size / 4)).background(RadarColors.SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) { Text(svc.label.take(1), style = MaterialTheme.typography.labelSmall) }
    }
}
