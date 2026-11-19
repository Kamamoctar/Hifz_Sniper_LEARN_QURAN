package com.hifzsniper.app.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hifzsniper.app.presentation.AppUiState
import com.hifzsniper.app.presentation.AppViewModel
import com.hifzsniper.app.presentation.screens.DashboardScreen
import com.hifzsniper.app.presentation.screens.DownloadsScreen
import com.hifzsniper.app.presentation.screens.HomeScreen
import com.hifzsniper.app.presentation.screens.LearnScreen
import com.hifzsniper.app.presentation.screens.MutashabihatScreen
import com.hifzsniper.app.presentation.screens.OnboardingScreen
import com.hifzsniper.app.presentation.screens.PassagePickerScreen
import com.hifzsniper.app.presentation.screens.PlayerScreen
import com.hifzsniper.app.presentation.screens.ReviewScreen
import com.hifzsniper.app.presentation.screens.SettingsScreen
import com.hifzsniper.app.presentation.screens.SourcesScreen
import com.hifzsniper.app.presentation.screens.WeakPointsScreen

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LEARN = "learn"
    const val PASSAGE_PICKER = "passage-picker"
    const val PLAYER = "player"
    const val REVIEW = "review"
    const val WEAK_POINTS = "weak-points"
    const val DOWNLOADS = "downloads"
    const val DASHBOARD = "dashboard"
    const val SETTINGS = "settings"
    const val SOURCES = "sources"
    const val MUTASHABIHAT = "mutashabihat"
}

private data class MainDestination(val route: String, val label: String)

private val mainDestinations = listOf(
    MainDestination(Routes.HOME, "Aujourd’hui"),
    MainDestination(Routes.PLAYER, "Mushaf 📖"),
    MainDestination(Routes.LEARN, "Apprendre"),
    MainDestination(Routes.REVIEW, "Réviser"),
    MainDestination(Routes.DASHBOARD, "Progression")
)

private val titles = mapOf(
    Routes.ONBOARDING to "Guide d'accueil",
    Routes.HOME to "Aujourd’hui",
    Routes.LEARN to "Apprendre",
    Routes.PASSAGE_PICKER to "Choisir un passage",
    Routes.PLAYER to "Mushaf par page",
    Routes.REVIEW to "Réviser",
    Routes.WEAK_POINTS to "Points faibles",
    Routes.DOWNLOADS to "Téléchargements",
    Routes.DASHBOARD to "Progression",
    Routes.SETTINGS to "Réglages",
    Routes.SOURCES to "Sources",
    Routes.MUTASHABIHAT to "Versets similaires"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HifzSniperNavigation(state: AppUiState, viewModel: AppViewModel) {
    if (!state.loaded) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator()
                Text("Préparation locale du corpus coranique…")
                Text("Cette étape peut prendre quelques instants au premier démarrage.")
            }
        }
        return
    }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isFirstLaunch = !state.preferences.onboardingComplete
    val isAtOnboarding = currentRoute == Routes.ONBOARDING || (currentRoute == null && isFirstLaunch)
    val isMainDestination = mainDestinations.any { it.route == currentRoute }
    Scaffold(
        topBar = {
            if (!(isFirstLaunch && isAtOnboarding)) {
                TopAppBar(
                    title = { Text(titles[currentRoute] ?: "Hifz-Sniper") },
                    navigationIcon = {
                        if (!isMainDestination) TextButton(onClick = { navController.popBackStack() }) { Text("Retour") }
                    },
                    actions = {
                        if (currentRoute != Routes.SETTINGS && currentRoute != Routes.ONBOARDING) {
                            IconButton(
                                onClick = { navController.navigate(Routes.SETTINGS) },
                                modifier = Modifier.semantics { contentDescription = "Ouvrir les réglages" }
                            ) {
                                Text("⚙️", fontSize = 18.sp)
                            }
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (!isAtOnboarding && isMainDestination) {
                NavigationBar {
                    mainDestinations.forEach { destination ->
                        val iconEmoji = when (destination.route) {
                            Routes.HOME -> "🏠"
                            Routes.PLAYER -> "📖"
                            Routes.LEARN -> "🎯"
                            Routes.REVIEW -> "🔄"
                            Routes.DASHBOARD -> "📊"
                            else -> if (currentRoute == destination.route) "●" else "○"
                        }
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Text(iconEmoji, fontSize = 18.sp) },
                            label = { Text(destination.label, maxLines = 1) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (state.preferences.onboardingComplete) Routes.HOME else Routes.ONBOARDING,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    state = state,
                    onComplete = { minutes, level, surah, ayah, goal, transliteration, translation, tafsir ->
                        viewModel.completeOnboarding(
                            minutes, level, surah, ayah, goal,
                            transliteration, translation, tafsir
                        )
                    },
                    onFinished = {
                        if (state.preferences.onboardingComplete) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    state = state,
                    onStartReview = { navController.navigate(Routes.REVIEW) },
                    onChoosePassage = { navController.navigate(Routes.PASSAGE_PICKER) },
                    onOpenMushaf = { navController.navigate(Routes.PLAYER) },
                    onOpenWeakPoints = { navController.navigate(Routes.WEAK_POINTS) },
                    onOpenMutashabihat = { navController.navigate(Routes.MUTASHABIHAT) },
                    onInstallContent = { navController.navigate(Routes.SOURCES) }
                )
            }
            composable(Routes.LEARN) {
                LearnScreen(
                    state = state,
                    onChoosePassage = { navController.navigate(Routes.PASSAGE_PICKER) },
                    onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                    onAddPassage = viewModel::activateSelectedPassage,
                    onOpenSources = { navController.navigate(Routes.SOURCES) },
                    onRate = viewModel::review,
                    onCreateBridge = viewModel::createBridgeToNextVerse,
                    onCreateChunkCard = viewModel::createChunkReinforcementCard,
                    onToggleTransliteration = viewModel::setTransliteration,
                    onToggleTranslation = viewModel::setTranslation,
                    onToggleTafsir = viewModel::setTafsir,
                    onFlagWeakPoint = viewModel::recordWeakPoint,
                    onNextPassage = { viewModel.movePassage(1) }
                )
            }
            composable(Routes.PASSAGE_PICKER) {
                PassagePickerScreen(
                    state = state,
                    onSelect = viewModel::setStartPoint,
                    onSelectSet = viewModel::setVerseSet,
                    onSelectPage = viewModel::goToPage,
                    onSelected = {
                        navController.navigate(Routes.PLAYER) {
                            popUpTo(Routes.HOME)
                        }
                    }
                )
            }
            composable(Routes.PLAYER) {
                PlayerScreen(
                    state = state,
                    onChoosePassage = { navController.navigate(Routes.PASSAGE_PICKER) },
                    onPrevious = { viewModel.movePassage(-1) },
                    onNext = { viewModel.movePassage(1) },
                    onPreviousPage = { viewModel.movePage(-1) },
                    onNextPage = { viewModel.movePage(1) },
                    onGoToPage = viewModel::goToPage,
                    onSelectAyah = { surah, ayah -> viewModel.setCurrentAyah(surah, ayah) },
                    onSelectVerseSet = viewModel::setVerseSet,
                    onDownloadAudio = viewModel::downloadCurrentAudio,
                    onDownloadPageAudio = viewModel::downloadPageAudio,
                    onDownloadSurahAudio = viewModel::downloadSurahAudio,
                    onToggleTransliteration = viewModel::setTransliteration,
                    onToggleTranslation = viewModel::setTranslation,
                    onToggleTafsir = viewModel::setTafsir,
                    onOpenDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onOpenMutashabihat = { navController.navigate(Routes.MUTASHABIHAT) },
                    onFlagWeakRange = viewModel::recordWeakRange
                )
            }
            composable(Routes.MUTASHABIHAT) {
                MutashabihatScreen(
                    state = state,
                    onSelectAyah = { surah, ayah ->
                        viewModel.setStartPoint(surah, ayah)
                        navController.navigate(Routes.PLAYER)
                    },
                    onChoosePassage = { navController.navigate(Routes.PASSAGE_PICKER) }
                )
            }
            composable(Routes.REVIEW) {
                ReviewScreen(
                    state = state,
                    onRate = viewModel::review,
                    onChoosePassage = { navController.navigate(Routes.PASSAGE_PICKER) }
                )
            }
            composable(Routes.WEAK_POINTS) {
                WeakPointsScreen(
                    state = state,
                    onResolve = viewModel::resolveWeakPoint,
                    onOpenContext = { verseKey ->
                        val parts = verseKey.split(':')
                        val surah = parts.getOrNull(0)?.toIntOrNull()
                        val ayah = parts.getOrNull(1)?.toIntOrNull()
                        if (surah != null && ayah != null) {
                            viewModel.setStartPoint(surah, ayah)
                            navController.navigate(Routes.PLAYER)
                        }
                    }
                )
            }
            composable(Routes.DOWNLOADS) {
                DownloadsScreen(
                    state = state,
                    onStart = viewModel::startDownload,
                    onCancel = viewModel::cancelDownload,
                    onDelete = viewModel::deleteDownload,
                    onDownloadCurrentPage = { state.playerPassage?.pageNumber?.let(viewModel::downloadPageAudio) },
                    onOpenSources = { navController.navigate(Routes.SOURCES) }
                )
            }
            composable(Routes.DASHBOARD) { DashboardScreen(state) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    state = state,
                    onMinutes = viewModel::setDailyMinutes,
                    onLevel = viewModel::setLevel,
                    onDailyGoal = viewModel::setDailyAyahGoal,
                    onChoosePassage = { navController.navigate(Routes.PASSAGE_PICKER) },
                    onTransliteration = viewModel::setTransliteration,
                    onTranslation = viewModel::setTranslation,
                    onTafsir = viewModel::setTafsir,
                    onRestartOnboarding = { navController.navigate(Routes.ONBOARDING) },
                    onDownloads = { navController.navigate(Routes.DOWNLOADS) },
                    onSources = { navController.navigate(Routes.SOURCES) },
                    onWeakPoints = { navController.navigate(Routes.WEAK_POINTS) },
                    onExportBackup = viewModel::exportBackupJson,
                    onInspectBackup = viewModel::inspectBackup,
                    onRestoreBackup = viewModel::restoreBackup
                )
            }
            composable(Routes.SOURCES) {
                SourcesScreen(state = state, onInstallCorpus = viewModel::importBundledCorpus)
            }
        }
    }
}
