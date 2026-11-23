package com.movieradar.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.movieradar.app.R
import com.movieradar.app.domain.TitleSummary
import com.movieradar.app.ui.browse.BrowseScreen
import com.movieradar.app.ui.details.DetailsScreen
import com.movieradar.app.ui.franchise.FranchiseListScreen
import com.movieradar.app.ui.franchise.FranchiseScreen
import com.movieradar.app.ui.library.LibraryScreen
import com.movieradar.app.ui.person.PersonScreen
import com.movieradar.app.ui.player.PlayerScreen
import com.movieradar.app.ui.upcoming.UpcomingScreen
import com.movieradar.app.ui.home.HomeScreen
import com.movieradar.app.ui.search.SearchScreen
import com.movieradar.app.ui.settings.SettingsScreen
import com.movieradar.app.ui.theme.RadarColors
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object SearchRoute
@Serializable object BrowseRoute
@Serializable object UpcomingRoute
@Serializable object LibraryRoute
@Serializable object SettingsRoute
@Serializable data class DetailsRoute(val type: String, val id: Int)
@Serializable data class PlayerRoute(val identifier: String, val title: String)
@Serializable data class PersonRoute(val id: Int)
@Serializable data class FranchiseRoute(val id: Int, val name: String)
@Serializable object FranchisesRoute

private data class Tab(val route: Any, @StringRes val label: Int, val icon: ImageVector)

private val Tabs = listOf(
    Tab(HomeRoute, R.string.tab_home, Icons.Outlined.Home),
    Tab(SearchRoute, R.string.tab_search, Icons.Outlined.Search),
    Tab(BrowseRoute, R.string.tab_browse, Icons.Outlined.Explore),
    Tab(UpcomingRoute, R.string.tab_upcoming, Icons.Outlined.CalendarMonth),
    Tab(LibraryRoute, R.string.tab_library, Icons.Outlined.VideoLibrary),
)

private fun NavHostController.openDetails(t: TitleSummary) = navigate(DetailsRoute(t.type.path, t.id))
private fun NavHostController.openPerson(id: Int) = navigate(PersonRoute(id))
private fun NavHostController.play(identifier: String, title: String) = navigate(PlayerRoute(identifier, title))

@Composable
fun MovieRadarRoot(openRequest: DetailsRoute? = null, onOpenHandled: () -> Unit = {}) {
    val nav = rememberNavController()
    // Opened from a "now streaming" notification.
    LaunchedEffect(openRequest) {
        openRequest?.let { nav.navigate(it); onOpenHandled() }
    }
    val entry by nav.currentBackStackEntryAsState()
    val onTab = Tabs.any { entry?.destination?.hasRoute(it.route::class) == true }
    Scaffold(
        containerColor = RadarColors.Black,
        bottomBar = {
            if (onTab) NavigationBar(containerColor = RadarColors.Black, tonalElevation = 0.dp) {
                Tabs.forEach { tab ->
                    val selected = entry?.destination?.hasRoute(tab.route::class) == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RadarColors.TextPrimary,
                            selectedTextColor = RadarColors.TextPrimary,
                            unselectedIconColor = RadarColors.TextSecondary,
                            unselectedTextColor = RadarColors.TextSecondary,
                            indicatorColor = RadarColors.SurfaceHigh,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = HomeRoute, modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            composable<HomeRoute> {
                HomeScreen(onOpen = nav::openDetails, onSettings = { nav.navigate(SettingsRoute) }, onPlay = nav::play)
            }
            composable<SearchRoute> { SearchScreen(onOpen = nav::openDetails, onOpenPerson = { nav.openPerson(it.id) }) }
            composable<BrowseRoute> { BrowseScreen(onOpen = nav::openDetails, onOpenFranchises = { nav.navigate(FranchisesRoute) }) }
            composable<FranchisesRoute> {
                FranchiseListScreen(onBack = { nav.popBackStack() }, onOpenFranchise = { nav.navigate(FranchiseRoute(it.id, it.name)) })
            }
            composable<FranchiseRoute> { FranchiseScreen(onBack = { nav.popBackStack() }, onOpen = nav::openDetails) }
            composable<UpcomingRoute> { UpcomingScreen(onOpen = nav::openDetails) }
            composable<LibraryRoute> { LibraryScreen(onOpen = nav::openDetails, onPlay = nav::play) }
            composable<DetailsRoute> {
                DetailsScreen(
                    onBack = { nav.popBackStack() }, onPlay = nav::play, onOpenPerson = nav::openPerson,
                    onOpenTitle = nav::openDetails, onOpenFranchise = { nav.navigate(FranchiseRoute(it.id, it.name)) },
                )
            }
            composable<PersonRoute> { PersonScreen(onBack = { nav.popBackStack() }, onOpen = nav::openDetails) }
            composable<PlayerRoute> { PlayerScreen(onBack = { nav.popBackStack() }) }
            composable<SettingsRoute> { SettingsScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
