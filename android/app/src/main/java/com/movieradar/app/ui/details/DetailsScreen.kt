package com.movieradar.app.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.LocalContentColor
import com.movieradar.app.ui.components.rememberNotifyToggle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.movieradar.app.domain.ArchiveItem
import com.movieradar.app.domain.Franchise
import com.movieradar.app.domain.FranchiseRef
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.key
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.Provider
import com.movieradar.app.domain.TitleDetails
import com.movieradar.app.ui.components.AgeBadge
import com.movieradar.app.ui.components.ErrorState
import com.movieradar.app.ui.components.Loadable
import com.movieradar.app.ui.components.MetaLine
import com.movieradar.app.ui.components.OttOpener
import com.movieradar.app.ui.components.Pill
import com.movieradar.app.ui.components.PosterCard
import com.movieradar.app.ui.components.ProviderLogo
import com.movieradar.app.ui.components.TmdbImage
import com.movieradar.app.ui.theme.RadarColors

@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    onPlay: (identifier: String, title: String) -> Unit,
    onOpenPerson: (id: Int) -> Unit,
    onOpenTitle: (TitleSummary) -> Unit = {},
    onOpenFranchise: (FranchiseRef) -> Unit = {},
    vm: DetailsViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(RadarColors.Black)) {
        when (val d = s.details) {
            Loadable.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = RadarColors.Accent)
            is Loadable.Failed -> ErrorState(d.message, vm::retry, Modifier.align(Alignment.Center))
            is Loadable.Ready -> DetailsContent(
                d = d.value,
                region = s.region,
                saved = s.saved,
                watched = s.watched,
                onToggleWatched = vm::toggleWatched,
                freeCopy = s.freeCopy,
                links = s.links,
                onToggleSaved = vm::toggleSaved,
                onPlay = onPlay,
                onOpenPerson = onOpenPerson,
                alertOn = s.alertOn,
                onAlertChange = vm::setAlert,
                franchise = s.franchise,
                franchiseStreaming = s.franchiseStreaming,
                onOpenTitle = onOpenTitle,
                onOpenFranchise = onOpenFranchise,
            )
        }
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(8.dp),
        ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsContent(
    d: TitleDetails,
    region: String,
    saved: Boolean,
    watched: Boolean,
    onToggleWatched: () -> Unit,
    freeCopy: ArchiveItem?,
    links: Map<Int, String>,
    onToggleSaved: () -> Unit,
    onPlay: (identifier: String, title: String) -> Unit,
    onOpenPerson: (id: Int) -> Unit,
    alertOn: Boolean,
    onAlertChange: (Boolean) -> Unit,
    franchise: Franchise?,
    franchiseStreaming: Map<String, List<Provider>>,
    onOpenTitle: (TitleSummary) -> Unit,
    onOpenFranchise: (FranchiseRef) -> Unit,
) {
    val context = LocalContext.current
    val toggleAlert = rememberNotifyToggle(alertOn, onAlertChange)
    val t = d.summary
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(300.dp)) {
            AsyncImage(
                model = TmdbImage.backdrop(t.backdropPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.4f to Color.Transparent, 1f to Color.Black)))
        }
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PosterCard(t, onClick = {}, width = 104.dp)
            Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(t.title, style = MaterialTheme.typography.headlineSmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                MetaLine(
                    listOfNotNull(
                        t.year,
                        if (t.type == MediaType.TV) d.seasonCount?.let { "$it season${if (it == 1) "" else "s"}" } ?: "Series"
                        else d.runtimeMinutes?.let { "${it / 60}h ${it % 60}m" },
                        if (t.rating > 0) "★ %.1f".format(t.rating) else null,
                    ),
                )
                AgeBadge(d.certification, d.ageBucket)
            }
        }
        if (d.genres.isNotEmpty()) {
            FlowRow(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { d.genres.forEach { Pill(it) } }
        }
        freeCopy?.let { item ->
            Button(
                onClick = { onPlay(item.identifier, item.title) },
                colors = ButtonDefaults.buttonColors(containerColor = RadarColors.Free, contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Outlined.PlayArrow, null)
                Spacer(Modifier.width(6.dp))
                Text("Play free · Internet Archive (public domain)")
            }
            Spacer(Modifier.height(8.dp))
        }
        FlowRow(
            Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (watched) {
                Button(
                    onClick = onToggleWatched,
                    colors = ButtonDefaults.buttonColors(containerColor = RadarColors.Free, contentColor = Color.Black),
                ) {
                    Icon(Icons.Outlined.Check, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Watched")
                }
            } else {
                OutlinedButton(onClick = onToggleWatched) {
                    Icon(Icons.Outlined.Check, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Mark as watched")
                }
            }
            OutlinedButton(onClick = onToggleSaved) {
                Icon(if (saved) Icons.Outlined.Check else Icons.Outlined.Add, null)
                Spacer(Modifier.width(6.dp))
                Text(if (saved) "In watchlist" else "Watchlist")
            }
            OutlinedButton(onClick = toggleAlert) {
                Icon(if (alertOn) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsNone, null,
                    tint = if (alertOn) RadarColors.Accent else LocalContentColor.current)
                Spacer(Modifier.width(6.dp))
                Text(if (alertOn) "Notifying" else "Notify me")
            }
            d.trailerYoutubeKey?.let { key ->
                OutlinedButton(onClick = { OttOpener.openYoutube(context, key) }) {
                    Icon(Icons.Outlined.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Trailer")
                }
            }
        }
        d.tagline?.let {
            Text("“$it”", style = MaterialTheme.typography.bodyMedium, color = RadarColors.TextSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
        }
        if (t.overview.isNotBlank()) Overview(t.overview)

        SectionTitle("Where to watch in $region")
        val w = d.whereToWatch
        if (w.isEmpty) {
            Text(
                "Not available on any streaming service in $region right now.",
                color = RadarColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        } else {
            val open: (Provider) -> Unit = {
                OttOpener.open(context, it, t.title, w.tmdbLink, exactUrl = links[it.id], isMovie = t.type == MediaType.MOVIE)
            }
            ProviderGroup("Stream", w.stream, open)
            ProviderGroup("Free", w.free, open)
            ProviderGroup("Rent", w.rent, open)
            ProviderGroup("Buy", w.buy, open)
        }

        franchise?.let { f ->
            val parts = f.ordered
            Row(
                Modifier.fillMaxWidth().clickable { onOpenFranchise(FranchiseRef(f.id, f.name)) }
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Part of the ${f.name}", style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${parts.size} films · release order", style = MaterialTheme.typography.labelMedium, color = RadarColors.TextSecondary)
                }
                Text("See all ›", style = MaterialTheme.typography.labelLarge, color = RadarColors.Accent)
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(parts, key = { it.key }) { p ->
                    val current = p.id == t.id && p.type == t.type
                    Column(Modifier.width(104.dp)) {
                        PosterCard(p, onClick = { if (!current) onOpenTitle(p) }, width = 104.dp)
                        Text(
                            if (current) "You're viewing" else (p.year ?: "Upcoming"),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (current) RadarColors.Accent else RadarColors.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        franchiseStreaming[p.key]?.takeIf { it.isNotEmpty() }?.let { logos ->
                            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                logos.take(3).forEach { ProviderLogo(it, size = 20.dp) }
                            }
                        }
                    }
                }
            }
        }

        if (d.cast.isNotEmpty()) {
            SectionTitle("Cast")
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(d.cast) { c ->
                    Column(
                        Modifier.width(76.dp).clickable(enabled = c.id > 0) { onOpenPerson(c.id) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AsyncImage(
                            model = TmdbImage.profile(c.profilePath),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(64.dp).clip(CircleShape).background(RadarColors.SurfaceHigh),
                        )
                        Text(c.name, style = MaterialTheme.typography.labelMedium, maxLines = 2, textAlign = TextAlign.Center)
                        c.character?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = RadarColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        if (d.seasons.isNotEmpty()) {
            SectionTitle("Seasons")
            d.seasons.forEach { s ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(s.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        listOfNotNull("${s.episodeCount} ep", s.airDate?.take(4)).joinToString(" • "),
                        color = RadarColors.TextSecondary, style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }

        Text(
            "Streaming data by JustWatch via TMDB",
            style = MaterialTheme.typography.labelSmall,
            color = RadarColors.TextSecondary,
            modifier = Modifier.padding(16.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Overview(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = if (expanded) Int.MAX_VALUE else 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).clickable { expanded = !expanded },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 10.dp))
}

@Composable
private fun ProviderGroup(label: String, providers: List<Provider>, onOpen: (Provider) -> Unit) {
    if (providers.isEmpty()) return
    Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = RadarColors.TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(providers, key = { it.id }) { p ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(RadarColors.SurfaceHigh)
                    .clickable { onOpen(p) }
                    .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProviderLogo(p, size = 36.dp)
                Text(p.name, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = RadarColors.TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}
