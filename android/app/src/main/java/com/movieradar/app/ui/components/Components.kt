package com.movieradar.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.movieradar.app.domain.AgeBucket
import com.movieradar.app.domain.Provider
import com.movieradar.app.domain.Shelf
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.ui.theme.RadarColors

private val CardShape = RoundedCornerShape(10.dp)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PosterCard(
    item: TitleSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 116.dp,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .clip(CardShape)
            .background(RadarColors.SurfaceHigh)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics { contentDescription = item.title },
    ) {
        if (item.posterPath != null) {
            AsyncImage(
                model = TmdbImage.poster(item.posterPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(
                Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Outlined.Movie, null, tint = RadarColors.TextSecondary)
                Spacer(Modifier.height(6.dp))
                Text(item.title, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 3)
            }
        }
    }
}

@Composable
fun ShelfRow(shelf: Shelf, onItemClick: (TitleSummary) -> Unit) {
    Column(Modifier.padding(vertical = 10.dp)) {
        Text(
            shelf.title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(shelf.items, key = { "${it.type}-${it.id}" }) { item ->
                PosterCard(item, onClick = { onItemClick(item) })
            }
        }
    }
}

@Composable
fun ProviderLogo(provider: Provider, size: Dp = 28.dp, onClick: (() -> Unit)? = null) {
    val m = Modifier
        .size(size)
        .clip(RoundedCornerShape(size / 4))
        .background(RadarColors.SurfaceHigh)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .semantics { contentDescription = provider.name }
    AsyncImage(model = TmdbImage.logo(provider.logoPath), contentDescription = null, modifier = m)
}

@Composable
fun AgeBadge(cert: String?, bucket: AgeBucket?) {
    val text = cert ?: bucket?.label ?: "NR"
    val color = when (bucket) {
        AgeBucket.EIGHTEEN -> Color(0xFFFF5A5F)
        AgeBucket.SIXTEEN -> Color(0xFFFF9F43)
        AgeBucket.THIRTEEN -> RadarColors.Gold
        AgeBucket.SEVEN, AgeBucket.ALL -> RadarColors.Free
        null -> RadarColors.TextSecondary
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
fun Pill(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = RadarColors.TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(RadarColors.SurfaceHigh)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Outlined.CloudOff, null, tint = RadarColors.TextSecondary, modifier = Modifier.size(40.dp))
        Text(message, color = RadarColors.TextSecondary, textAlign = TextAlign.Center)
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = RadarColors.TextPrimary, contentColor = Color.Black),
        ) { Text("Retry") }
    }
}

@Composable
fun shimmerAlpha(): Float {
    val t = rememberInfiniteTransition(label = "shimmer")
    val a by t.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "a")
    return a
}

@Composable
fun ShelfSkeleton() {
    val a = shimmerAlpha()
    Column(Modifier.padding(vertical = 10.dp)) {
        Box(
            Modifier.padding(horizontal = 16.dp, vertical = 6.dp).width(140.dp).height(16.dp)
                .clip(RoundedCornerShape(4.dp)).alpha(a).background(RadarColors.SurfaceHigh),
        )
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) {
                Box(Modifier.width(116.dp).aspectRatio(2f / 3f).clip(CardShape).alpha(a).background(RadarColors.SurfaceHigh))
            }
        }
    }
}

@Composable
fun MetaLine(parts: List<String>) {
    Text(
        parts.filter { it.isNotBlank() }.joinToString("  •  "),
        style = MaterialTheme.typography.labelMedium,
        color = RadarColors.TextSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

fun userMessage(t: Throwable): String = when (t) {
    is java.net.UnknownHostException -> "No internet connection, or the proxy URL is not set."
    is java.io.IOException -> "Network problem. Check your connection and try again."
    is retrofit2.HttpException -> when (t.code()) {
        401 -> "The app token doesn't match the proxy."
        403 -> "That request isn't allowed by the proxy."
        else -> "Server error (${t.code()}). Try again."
    }
    else -> "Something went wrong. Try again."
}

/** Round actor photo with a person icon when TMDB has no picture. */
@Composable
fun ProfilePhoto(path: String?, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape).background(RadarColors.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (path != null) {
            AsyncImage(
                model = TmdbImage.profile(path), contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(androidx.compose.material.icons.Icons.Outlined.Person, null, tint = RadarColors.TextSecondary)
        }
    }
}
