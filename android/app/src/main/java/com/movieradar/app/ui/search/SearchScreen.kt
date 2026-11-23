package com.movieradar.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movieradar.app.domain.MediaType
import com.movieradar.app.domain.PersonSummary
import com.movieradar.app.ui.components.ProfilePhoto
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.domain.WhereToWatch
import com.movieradar.app.ui.components.ErrorState
import com.movieradar.app.ui.components.MetaLine
import com.movieradar.app.ui.components.PosterCard
import com.movieradar.app.ui.components.ProviderLogo
import com.movieradar.app.ui.theme.RadarColors

@Composable
fun SearchScreen(
    onOpen: (TitleSummary) -> Unit,
    onOpenPerson: (PersonSummary) -> Unit,
    vm: SearchViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (s.query.isEmpty()) focus.requestFocus() }

    Column(Modifier.fillMaxSize().background(RadarColors.Black).windowInsetsPadding(WindowInsets.statusBars)) {
        TextField(
            value = s.query,
            onValueChange = vm::onQueryChange,
            placeholder = { Text("Search movies, series & actors") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = {
                if (s.query.isNotEmpty()) IconButton(onClick = { vm.onQueryChange("") }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Clear")
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = RadarColors.SurfaceHigh,
                unfocusedContainerColor = RadarColors.SurfaceHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = RadarColors.Accent,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).focusRequester(focus),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchFilter.entries.forEach { f ->
                FilterChip(
                    selected = s.filter == f,
                    onClick = { vm.onFilterChange(f) },
                    label = { Text(when (f) { SearchFilter.ALL -> "All"; SearchFilter.MOVIES -> "Movies"; SearchFilter.SERIES -> "Series"; SearchFilter.PEOPLE -> "People" }) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RadarColors.TextPrimary,
                        selectedLabelColor = Color.Black,
                    ),
                )
            }
        }
        if (s.loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp), color = RadarColors.Accent)

        when {
            s.error != null -> ErrorState(s.error!!, { vm.retry() })
            s.query.trim().length < 2 -> Hint("Type a movie or series name — e.g. \"Jawan\", \"Panchayat\", \"Dune\"")
            s.filter == SearchFilter.PEOPLE -> when {
                !s.loading && s.people.isEmpty() -> Hint("No actors or actresses found for \"${s.query.trim()}\"")
                else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                    items(s.people, key = { "p-${it.id}" }) { p -> PersonRow(p) { onOpenPerson(p) } }
                }
            }
            !s.loading && s.visible.isEmpty() -> Hint("No results for \"${s.query.trim()}\"")
            else -> LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(s.visible, key = { it.key() }) { item ->
                    ResultRow(item, s.genres, s.providers[item.key()], onClick = { onOpen(item) })
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = RadarColors.TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ResultRow(item: TitleSummary, genres: Map<Int, String>, where: WhereToWatch?, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PosterCard(item, onClick = onClick, width = 72.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            MetaLine(
                listOfNotNull(
                    item.year,
                    if (item.type == MediaType.TV) "Series" else "Movie",
                    if (item.rating > 0) "★ %.1f".format(item.rating) else null,
                ),
            )
            val g = item.genreIds.mapNotNull { genres[it] }.take(3)
            if (g.isNotEmpty()) Text(g.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = RadarColors.TextSecondary)
            when {
                where == null -> {}
                where.headline.isNotEmpty() -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    where.headline.take(5).forEach { ProviderLogo(it, size = 26.dp) }
                }
                where.rent.isNotEmpty() || where.buy.isNotEmpty() ->
                    Text("Rent / Buy", style = MaterialTheme.typography.labelMedium, color = RadarColors.Gold)
                else -> Text("Not streaming in your region", style = MaterialTheme.typography.labelMedium, color = RadarColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun PersonRow(p: PersonSummary, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfilePhoto(p.profilePath, size = 64.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            p.department?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = RadarColors.Accent) }
            if (p.knownFor.isNotEmpty()) {
                Text(
                    p.knownFor.joinToString(" · "), style = MaterialTheme.typography.labelMedium,
                    color = RadarColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
