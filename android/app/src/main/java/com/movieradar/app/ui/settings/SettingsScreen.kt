package com.movieradar.app.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.movieradar.app.domain.OttService
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.movieradar.app.BuildConfig
import com.movieradar.app.ui.components.ErrorState
import com.movieradar.app.ui.components.Loadable
import com.movieradar.app.ui.theme.RadarColors

@Composable
fun SettingsScreen(onBack: () -> Unit, vm: SettingsViewModel = hiltViewModel()) {
    val current by vm.region.collectAsStateWithLifecycle()
    val regions by vm.regions.collectAsStateWithLifecycle()
    val showAdult by vm.showAdult.collectAsStateWithLifecycle()
    val alertsEnabled by vm.alertsEnabled.collectAsStateWithLifecycle()
    val myServices by vm.myServices.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(RadarColors.Black).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Streaming alerts", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Daily check (around 9 AM) for titles with 🔔 Notify me. Turning this off keeps your list.",
                    style = MaterialTheme.typography.bodyMedium, color = RadarColors.TextSecondary,
                )
            }
            Switch(
                checked = alertsEnabled,
                onCheckedChange = vm::setAlertsEnabled,
                colors = SwitchDefaults.colors(checkedTrackColor = RadarColors.Accent),
            )
        }
        if (BuildConfig.ADULT_ENABLED) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Show 18+ titles", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Adds 18+ rows on Home and includes 18+ titles in Search and Steamy. These rarely have a streaming service.",
                        style = MaterialTheme.typography.bodyMedium, color = RadarColors.TextSecondary,
                    )
                }
                Switch(
                    checked = showAdult,
                    onCheckedChange = vm::setShowAdult,
                    colors = SwitchDefaults.colors(checkedTrackColor = RadarColors.Accent),
                )
            }
        }
        Text("My services", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
        Text(
            "Tick what you subscribe to. 🎲 Surprise me uses these for \"Any of mine\" (all five if none ticked).",
            style = MaterialTheme.typography.bodyMedium, color = RadarColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OttService.entries.forEach { svc ->
                FilterChip(
                    selected = svc in myServices, onClick = { vm.toggleService(svc) }, label = { Text(svc.label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RadarColors.Accent, selectedLabelColor = Color.White),
                )
            }
        }
        Text("Region", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp))
        Text(
            "Streaming services, age ratings and release dates follow this country.",
            style = MaterialTheme.typography.bodyMedium, color = RadarColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        TextField(
            value = filter,
            onValueChange = { filter = it.take(40) },
            placeholder = { Text("Find a country") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = RadarColors.SurfaceHigh, unfocusedContainerColor = RadarColors.SurfaceHigh,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        Column(Modifier.weight(1f)) {
            when (val r = regions) {
                Loadable.Loading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(24.dp))
                is Loadable.Failed -> ErrorState(r.message, { vm.load() })
                is Loadable.Ready -> {
                    val list = r.value.filter { filter.isBlank() || it.name.contains(filter, true) || it.code.equals(filter, true) }
                        .sortedBy { if (it.code == current) 0 else 1 }
                    LazyColumn {
                        items(list, key = { it.code }) { region ->
                            Row(
                                Modifier.fillMaxWidth().clickable { vm.select(region.code) }.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(flag(region.code) + "  " + region.name, modifier = Modifier.weight(1f))
                                if (region.code == current) Icon(Icons.Outlined.Check, contentDescription = "Selected", tint = RadarColors.Accent)
                            }
                        }
                    }
                }
            }
        }
        Text(
            "This application is designed and developed by Ashish Puthran",
            style = MaterialTheme.typography.labelSmall,
            color = RadarColors.TextSecondary,
            modifier = Modifier.padding(16.dp),
        )
    }
}

private fun flag(code: String): String =
    if (code.length != 2) "" else code.uppercase().map { Character.toChars(0x1F1E6 + (it - 'A')).concatToString() }.joinToString("")
