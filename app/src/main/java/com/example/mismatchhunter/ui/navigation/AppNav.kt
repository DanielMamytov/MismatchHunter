package com.example.mismatchhunter.ui.navigation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.compose.material3.TextButton
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledTonalButton
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mismatchhunter.R
import com.example.mismatchhunter.data.local.SessionEntity
import com.example.mismatchhunter.di.AppContainer
import com.example.mismatchhunter.ui.viewmodel.AnalyticsViewModel
import com.example.mismatchhunter.ui.viewmodel.AppViewModelFactory
import com.example.mismatchhunter.ui.viewmodel.CreateSessionViewModel
import com.example.mismatchhunter.ui.viewmodel.EpisodeDetailViewModel
import com.example.mismatchhunter.ui.viewmodel.EpisodeEntryViewModel
import com.example.mismatchhunter.ui.viewmodel.HomeViewModel
import com.example.mismatchhunter.ui.viewmodel.OnboardingViewModel
import com.example.mismatchhunter.ui.viewmodel.PlaybookViewModel
import com.example.mismatchhunter.ui.viewmodel.PreloaderViewModel
import com.example.mismatchhunter.ui.viewmodel.SessionDetailViewModel
import com.example.mismatchhunter.ui.viewmodel.SettingsViewModel
import com.example.mismatchhunter.utils.DateUtils

@Composable
fun AppNav(appContainer: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "preloader") {
        composable("preloader") {
            val vm: PreloaderViewModel = viewModel(
                factory = AppViewModelFactory(
                    appContainer.settingsRepository,
                    appContainer.sessionRepository,
                    appContainer.episodeRepository,
                    appContainer.noteRepository
                )
            )
            val state by vm.state.collectAsState()
            LaunchedEffect(state.route) {
                if (state.route != null) navController.navigate(state.route!!) {
                    popUpTo("preloader") {
                        inclusive = true
                    }
                }
            }
            Column(
                Modifier.screenContainerPadding(horizontal = 24.dp, top = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (state.loading) CircularProgressIndicator()
                Text(
                    "Initializing database, dependencies, and theme",
                    modifier = Modifier.padding(top = 16.dp)
                )
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        composable("onboarding1") {
            val vm: OnboardingViewModel = viewModel(
                factory = AppViewModelFactory(
                    appContainer.settingsRepository,
                    appContainer.sessionRepository,
                    appContainer.episodeRepository,
                    appContainer.noteRepository
                )
            )
            OnboardingScreen(
                title = "Mismatch Hunter",
                text = "Track and analyze every possession to find the opponent's weak spots.",
                button = "Next"
            ) { navController.navigate("onboarding2") }
        }
        composable("onboarding2") {
            val vm: OnboardingViewModel = viewModel(
                factory = AppViewModelFactory(
                    appContainer.settingsRepository,
                    appContainer.sessionRepository,
                    appContainer.episodeRepository,
                    appContainer.noteRepository
                )
            )
            OnboardingScreen(
                title = "Step-by-step episode logging",
                text = "Position, mismatch type, zone, decision, and result — all in one flow.",
                button = "Get started"
            ) {
                vm.complete {
                    navController.navigate("main") { popUpTo("onboarding1") { inclusive = true } }
                }
            }
        }
        composable("main") { MainTabs(appContainer, navController) }
        composable("create_session") {
            val vm: CreateSessionViewModel = viewModel(
                factory = AppViewModelFactory(
                    appContainer.settingsRepository,
                    appContainer.sessionRepository,
                    appContainer.episodeRepository,
                    appContainer.noteRepository
                )
            )
            CreateSessionScreen(vm) { id -> navController.navigate("session/$id") }
        }
        composable("session/{id}") { backStack ->
            val sessionId = backStack.arguments?.getString("id")?.toLong() ?: 0L
            val vm: SessionDetailViewModel = viewModel(
                factory = AppViewModelFactory(
                    appContainer.settingsRepository,
                    appContainer.sessionRepository,
                    appContainer.episodeRepository,
                    appContainer.noteRepository,
                    sessionId = sessionId
                )
            )
            SessionDetailScreen(
                vm,
                openAddEpisode = { navController.navigate("session/$sessionId/add_episode") },
                openEpisode = { episodeId -> navController.navigate("episode/$episodeId") })
        }
        composable("session/{id}/add_episode") { backStack ->
            val sessionId = backStack.arguments?.getString("id")?.toLong() ?: 0L
            val vm: EpisodeEntryViewModel = viewModel(
                factory = AppViewModelFactory(
                    appContainer.settingsRepository,
                    appContainer.sessionRepository,
                    appContainer.episodeRepository,
                    appContainer.noteRepository
                )
            )
            EpisodeEntryScreen(vm) { vm.save(sessionId) { navController.popBackStack() } }
        }
        composable("episode/{id}") { backStack ->
            val episodeId = backStack.arguments?.getString("id")?.toLong() ?: 0L
            val vm: EpisodeDetailViewModel = viewModel(
                factory = AppViewModelFactory(
                    appContainer.settingsRepository,
                    appContainer.sessionRepository,
                    appContainer.episodeRepository,
                    appContainer.noteRepository,
                    episodeId = episodeId
                )
            )
            EpisodeDetailScreen(vm) { navController.popBackStack() }
        }
    }
}

@Composable
private fun OnboardingScreen(title: String, text: String, button: String, onNext: () -> Unit) {
    Column(
        Modifier
            .screenContainerPadding(horizontal = 24.dp, top = 24.dp, bottom = 24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(text)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onNext) { Text(button) }
    }
}

@Composable
private fun MainTabs(appContainer: AppContainer, rootNav: NavHostController) {
    val tabsNav = rememberNavController()
    val tabs = listOf(
        TabItem(route = "sessions", label = "Sessions", iconRes = R.drawable.ic_sessions),
        TabItem(route = "analytics", label = "Analytics", iconRes = R.drawable.ic_analytics),
        TabItem(route = "playbook", label = "Playbook", iconRes = R.drawable.ic_playbook),
        TabItem(route = "settings", label = "Settings", iconRes = R.drawable.ic_settings)
    )
    val entry by tabsNav.currentBackStackEntryAsState()

    Scaffold(bottomBar = {
        NavigationBar {
            tabs.forEach { tab ->
                NavigationBarItem(
                    selected = entry?.destination?.route == tab.route,
                    onClick = {
                        tabsNav.navigate(tab.route) {
                            popUpTo(tabsNav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    label = { Text(tab.label) },
                    icon = { Icon(painter = painterResource(id = tab.iconRes), contentDescription = tab.label) }
                )
            }
        }
    }) { p ->
        NavHost(tabsNav, startDestination = "sessions", modifier = Modifier.padding(p)) {
            composable("sessions") {
                val vm: HomeViewModel = viewModel(
                    factory = AppViewModelFactory(
                        appContainer.settingsRepository,
                        appContainer.sessionRepository,
                        appContainer.episodeRepository,
                        appContainer.noteRepository
                    )
                )
                val state by vm.state.collectAsState()
                HomeScreen(
                    state = state,
                    onCreateSession = { rootNav.navigate("create_session") },
                    onOpenSession = { rootNav.navigate("session/$it") },
                    onRetry = vm::retry)
            }
            composable("analytics") {
                val vm: AnalyticsViewModel = viewModel(
                    factory = AppViewModelFactory(
                        appContainer.settingsRepository,
                        appContainer.sessionRepository,
                        appContainer.episodeRepository,
                        appContainer.noteRepository
                    )
                )
                val state by vm.state.collectAsState()
                AnalyticsScreen(state = state, onSessionSelect = vm::selectSession, onPeriodSelect = vm::selectPeriod, onRetry = vm::retry)
            }
            composable("playbook") {
                val vm: PlaybookViewModel = viewModel(
                    factory = AppViewModelFactory(
                        appContainer.settingsRepository,
                        appContainer.sessionRepository,
                        appContainer.episodeRepository,
                        appContainer.noteRepository
                    )
                )
                PlaybookScreen(vm)
            }
            composable("settings") {
                val vm: SettingsViewModel = viewModel(
                    factory = AppViewModelFactory(
                        appContainer.settingsRepository,
                        appContainer.sessionRepository,
                        appContainer.episodeRepository,
                        appContainer.noteRepository
                    )
                )
                SettingsScreen(vm) {
                    rootNav.navigate("onboarding1") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            }
        }
    }
}


private data class TabItem(val route: String, val label: String, val iconRes: Int)

private fun tabTitle(tab: String): String = when (tab) {
    "sessions" -> "Sessions"
    "analytics" -> "Analytics"
    "playbook" -> "Notes"
    "settings" -> "Settings"
    else -> tab
}

private fun Modifier.screenContainerPadding(
    horizontal: androidx.compose.ui.unit.Dp = 16.dp,
    top: androidx.compose.ui.unit.Dp = 15.dp,
    bottom: androidx.compose.ui.unit.Dp = 16.dp
): Modifier = this
    .fillMaxSize()
    .statusBarsPadding()
    .padding(start = horizontal, end = horizontal, top = top, bottom = bottom)

@Composable
private fun HomeScreen(
    state: com.example.mismatchhunter.ui.viewmodel.HomeUiState,
    onCreateSession: () -> Unit,
    onOpenSession: (Long) -> Unit,
    onRetry: () -> Unit
) {
    Column(Modifier.screenContainerPadding()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Sessions", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onCreateSession) { Text("New") }
        }
        if (state.loading) CircularProgressIndicator()
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
        if (state.recent.isNotEmpty()) {
            Text("Recent episodes: ${state.recent.size}", modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        }
        if (!state.loading && state.sessions.isEmpty()) Text("No sessions yet. Create a session.")
        LazyColumn {
            items(state.sessions) { session ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    onClick = { onOpenSession(session.id) }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(session.title)
                        Text("${session.matchType} • ${DateUtils.formatEpochDay(session.dateEpochDay)}")
                        if (session.description.isNotBlank()) {
                            Text(session.description)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateSessionScreen(vm: CreateSessionViewModel, onSaved: (Long) -> Unit) {
    val title by vm.title.collectAsState()
    val date by vm.date.collectAsState()
    val match by vm.matchType.collectAsState()
    val description by vm.description.collectAsState()
    val error by vm.error.collectAsState()
    Column(Modifier.screenContainerPadding()) {
        Spacer(Modifier.height(15.dp))

        Text("Create session", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = title,
            onValueChange = { vm.title.value = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = date,
            onValueChange = { vm.date.value = it },
            label = { Text("Date (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = match,
            onValueChange = { vm.matchType.value = it },
            label = { Text("Match type") },
            placeholder = { Text("Regular") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { vm.description.value = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { vm.save(onSaved) },
            modifier = Modifier.padding(top = 12.dp)
        ) { Text("Save") }
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.padding(top = 10.dp)) {
        FilledTonalButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "$label: $value",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SessionDetailScreen(
    vm: SessionDetailViewModel,
    openAddEpisode: () -> Unit,
    openEpisode: (Long) -> Unit
) {
    val state by vm.state.collectAsState()
    Column(Modifier.screenContainerPadding()) {
        Spacer(Modifier.height(15.dp))
        Text(state.session?.title ?: "Session", style = MaterialTheme.typography.headlineSmall)
        Text(state.session?.let { DateUtils.formatEpochDay(it.dateEpochDay) } ?: "")

        val positionOptions = remember(state.availablePositions) { state.availablePositions }
        val switchTypeOptions = remember(state.availableSwitchTypes) { state.availableSwitchTypes }
        val resultOptions = remember(state.availableResults) { state.availableResults }

        Text("Position", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
        DropdownFilter(
            selectedValue = state.positionFilter,
            options = positionOptions,
            onSelect = vm::setPositionFilter
        )

        Text("Mismatch type", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
        DropdownFilter(
            selectedValue = state.switchTypeFilter,
            options = switchTypeOptions,
            onSelect = vm::setSwitchTypeFilter
        )

        Text("Result", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
        DropdownFilter(
            selectedValue = state.resultFilter,
            options = resultOptions,
            onSelect = vm::setResultFilter
        )

        Button(
            onClick = openAddEpisode,
            modifier = Modifier.padding(vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Add episode", color = MaterialTheme.colorScheme.onPrimary)
        }

        if (state.episodes.isEmpty()) Text("No data for this filter")

        LazyColumn {
            items(state.episodes) { ep ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    onClick = { openEpisode(ep.id) }
                ) {
                    Text(
                        "${ep.opponentPosition} • ${ep.result}",
                        Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownFilter(
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Filter") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeEntryScreen(vm: EpisodeEntryViewModel, onSave: () -> Unit) {
    val fields = listOf(vm.position, vm.switchType, vm.zone, vm.decision)
    val labels = listOf("Position", "Mismatch type", "Zone", "Decision")
    val fieldOptions = remember {
        listOf(
            listOf("PG", "SG", "SF", "PF", "C"),
            listOf("On-ball screen", "Off-ball screen", "Isolation", "Early offense", "Transition"),
            listOf("Top", "Left wing", "Right wing", "Left corner", "Right corner", "Paint"),
            listOf("Attack mismatch", "Pass out", "Reset", "Post-up", "Drive and kick")
        )
    }
    val selectedResult = vm.result.collectAsState().value
    val resultOptions = remember {
        listOf("Goal", "Drawn foul", "Miss", "Turnover", "No shot")
    }

    Column(Modifier.screenContainerPadding()) {
        Text("Episode entry", style = MaterialTheme.typography.headlineSmall)

        fields.forEachIndexed { index, state ->
            EntryDropdownField(
                selectedValue = state.collectAsState().value,
                options = fieldOptions[index],
                label = "${index + 1}/5 ${labels[index]}",
                onSelect = { state.value = it }
            )
        }

        EntryDropdownField(
            selectedValue = selectedResult,
            options = resultOptions,
            label = "5/5 Result",
            onSelect = { vm.result.value = it }
        )

        vm.error.collectAsState().value?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = onSave,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text("Save")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryDropdownField(
    selectedValue: String,
    options: List<String>,
    label: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
@Composable
private fun EpisodeDetailScreen(vm: EpisodeDetailViewModel, onSaved: () -> Unit) {
    val state by vm.state.collectAsState()
    val episode = state.episode
    var note by remember { mutableStateOf("") }
    var position by remember { mutableStateOf("") }
    var switchType by remember { mutableStateOf("") }
    var zone by remember { mutableStateOf("") }
    var decision by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    LaunchedEffect(episode?.id) {
        note = episode?.tacticalNote.orEmpty()
        position = episode?.opponentPosition.orEmpty()
        switchType = episode?.switchType.orEmpty()
        zone = episode?.courtZone.orEmpty()
        decision = episode?.decision.orEmpty()
        result = episode?.result.orEmpty()
    }

    Column(
        Modifier.screenContainerPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.loading) CircularProgressIndicator() else if (episode == null) { Text(state.error ?: "Loading...")
            TextButton(onClick = vm::retry) { Text("Retry") }
        } else {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Episode details",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${episode?.opponentPosition} / ${episode?.switchType}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EpisodeBadge("Zone", episode?.courtZone.orEmpty())
                        EpisodeBadge("Result", episode?.result.orEmpty())
                    }

                    EpisodeDetailRow("Decision", episode?.decision.orEmpty())
                }
            }

            OutlinedTextField(value = position, onValueChange = { position = it }, label = { Text("Position") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = switchType, onValueChange = { switchType = it }, label = { Text("Mismatch type") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = zone, onValueChange = { zone = it }, label = { Text("Zone") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = decision, onValueChange = { decision = it }, label = { Text("Decision") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = result, onValueChange = { result = it }, label = { Text("Result") }, modifier = Modifier.fillMaxWidth())

            Button(onClick = { vm.saveEpisodeFields(position, switchType, zone, decision, result, onSaved) }, modifier = Modifier.fillMaxWidth()) { Text("Save episode") }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Tactical note") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = { vm.saveNote(note, onSaved) }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Save note"
                )
            }
        }
    }
}

@Composable
private fun EpisodeBadge(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EpisodeDetailRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}


@Composable
private fun AnalyticsScreen(
    state: com.example.mismatchhunter.ui.viewmodel.AnalyticsUiState,
    onSessionSelect: (Long?) -> Unit,
    onPeriodSelect: (String) -> Unit,
    onRetry: () -> Unit
) {
    Column(Modifier.screenContainerPadding(horizontal = 20.dp, top = 24.dp, bottom = 24.dp)) {
        Text(
            "Analytics",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(Modifier.height(12.dp))
        FilterDropdown(
            label = "Session",
            value = state.sessions.firstOrNull { it.id == state.selectedSessionId }?.title ?: "All sessions",
            options = listOf("All sessions") + state.sessions.map { it.title },
            onSelect = { selected -> onSessionSelect(state.sessions.firstOrNull { it.title == selected }?.id) }
        )
        FilterDropdown(
            label = "Period",
            value = state.selectedPeriod,
            options = listOf("All time", "Last 7 days", "Last 30 days"),
            onSelect = onPeriodSelect
        )
        if (state.loading) CircularProgressIndicator()
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Retry") }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Offensive decision success rate",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${state.successRate}%",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    "By position",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                MiniBarChart(state.byPosition)
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    "By zone",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(10.dp))
                MiniBarChart(state.byZone)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybookScreen(vm: PlaybookViewModel) {
    val notes by vm.notes.collectAsState();
    val sessions by vm.sessions.collectAsState()
    val episodes by vm.availableEpisodes.collectAsState()
    val selectedSessionId by vm.selectedSessionId.collectAsState()
    val selectedEpisodeId by vm.selectedEpisodeId.collectAsState()
    val title by vm.title.collectAsState();
    val body by vm.body.collectAsState()
    var sessionExpanded by remember { mutableStateOf(false) }
    var episodeExpanded by remember { mutableStateOf(false) }

    val sessionLabel = sessions.firstOrNull { it.id == selectedSessionId }?.title ?: "No session"
    val episodeLabel = episodes.firstOrNull { it.id == selectedEpisodeId }?.let {
        "#${it.id}: ${it.opponentPosition} / ${it.switchType}"
    } ?: "No episode"

    Column(Modifier.screenContainerPadding()) {
        Text("Playbook notes", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = vm.searchQuery.collectAsState().value,
            onValueChange = { vm.searchQuery.value = it },
            label = { Text("Search notes") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = title,
            onValueChange = { vm.title.value = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = body,
            onValueChange = { vm.body.value = it },
            label = { Text("Body") },
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenuBox(expanded = sessionExpanded, onExpandedChange = { sessionExpanded = it }) {
            OutlinedTextField(
                value = sessionLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Link to session") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sessionExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            ExposedDropdownMenu(expanded = sessionExpanded, onDismissRequest = { sessionExpanded = false }) {
                DropdownMenuItem(text = { Text("No session") }, onClick = {
                    vm.selectSession(null)
                    sessionExpanded = false
                })
                sessions.forEach { session ->
                    DropdownMenuItem(text = { Text(session.title) }, onClick = {
                        vm.selectSession(session.id)
                        sessionExpanded = false
                    })
                }
            }
        }
        ExposedDropdownMenuBox(expanded = episodeExpanded, onExpandedChange = { episodeExpanded = it }) {
            OutlinedTextField(
                value = episodeLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Link to episode") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = episodeExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
            ExposedDropdownMenu(expanded = episodeExpanded, onDismissRequest = { episodeExpanded = false }) {
                DropdownMenuItem(text = { Text("No episode") }, onClick = {
                    vm.selectEpisode(null)
                    episodeExpanded = false
                })
                episodes.forEach { episode ->
                    DropdownMenuItem(text = { Text("#${episode.id}: ${episode.opponentPosition} / ${episode.switchType}") }, onClick = {
                        vm.selectEpisode(episode.id)
                        episodeExpanded = false
                    })
                }
            }
        }
        Button(
            onClick = { vm.save() },
            modifier = Modifier.padding(vertical = 8.dp)
        ) { Text("Save") }
        LazyColumn {
            items(notes) { note ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(note.title, fontWeight = FontWeight.Bold); Text(
                        note.body
                    )
                        if (note.sessionId != null || note.episodeId != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                buildString {
                                    if (note.sessionId != null) append("Session #${note.sessionId}")
                                    if (note.sessionId != null && note.episodeId != null) append(" · ")
                                    if (note.episodeId != null) append("Episode #${note.episodeId}")
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun MiniBarChart(data: Map<String, Int>) {
    if (data.isEmpty()) {
        Text("No data", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val maxValue = data.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        data.forEach { (label, value) ->
            Text("$label: $value", style = MaterialTheme.typography.bodyMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(value.toFloat() / maxValue.toFloat())
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(vm: SettingsViewModel, onResetDone: () -> Unit = {}) {
    val settings by vm.settings.collectAsState()

    val context = LocalContext.current
    val packageName = context.packageName
    val lifecycleOwner = LocalLifecycleOwner.current


    fun openRateApp() {
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))

        runCatching { context.startActivity(marketIntent) }
            .recoverCatching {
                context.startActivity(webIntent)
            }
            .onFailure {
                Toast.makeText(context, "Could not open the store", Toast.LENGTH_SHORT).show()
            }
    }

    fun shareApp() {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=$packageName")
        }
        val chooser = Intent.createChooser(sendIntent, "Share app")
        context.startActivity(chooser)
    }


    fun hasNotificationPermission(): Boolean {
        val areNotificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!areNotificationsEnabled) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    var notificationsAllowed by remember { mutableStateOf(hasNotificationPermission()) }

    DisposableEffect(lifecycleOwner, settings.seasonalNotifications) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsAllowed = hasNotificationPermission()
                if (!notificationsAllowed && settings.seasonalNotifications) {
                    vm.setSeasonalNotifications(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsAllowed = granted && hasNotificationPermission()
        vm.setSeasonalNotifications(notificationsAllowed)
    }

    var showClearConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var privateMode by remember { mutableStateOf(true) }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear local data?") },
            text = { Text("This action removes all sessions, episodes, and notes from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    vm.clearLocalData { Toast.makeText(context, "Local data cleared", Toast.LENGTH_SHORT).show() }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset settings?") },
            text = { Text("Theme, accent, and onboarding state will be reset.") },
            confirmButton = {
                TextButton(onClick = {
                    showResetConfirm = false
                    vm.reset { Toast.makeText(context, "Settings reset", Toast.LENGTH_SHORT).show(); onResetDone() }
                }) { Text("Reset") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } }
        )
    }

    Column(Modifier.screenContainerPadding()) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("Dark theme")
            Switch(checked = settings.darkTheme, onCheckedChange = vm::setDarkTheme)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Seasonal notifications")
            Switch(
                checked = settings.seasonalNotifications && notificationsAllowed,
                onCheckedChange = { enabled ->
                    if (!enabled) {
                        vm.setSeasonalNotifications(false)
                        return@Switch
                    }
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        val allowed = hasNotificationPermission()
                        notificationsAllowed = allowed
                        vm.setSeasonalNotifications(allowed)
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        val allowed = hasNotificationPermission()
                        notificationsAllowed = allowed
                        vm.setSeasonalNotifications(allowed)
                    } else {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
        }
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = settings.accentColor,
                onValueChange = {},
                readOnly = true,
                label = { Text("Accent") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth().padding(top = 12.dp)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("blue", "lightBlue").forEach { accentValue ->
                    DropdownMenuItem(
                        text = { Text(if (accentValue == "blue") "Blue" else "Light blue") },
                        onClick = { vm.setAccent(accentValue); expanded = false })
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text("Private mode")
            Switch(
                checked = privateMode,
                onCheckedChange = {
                    privateMode = it
                    Toast.makeText(context, if (it) "Private mode enabled" else "Private mode disabled", Toast.LENGTH_SHORT).show()
                }
            )
        }

        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp)) {
                TextButton(onClick = { showClearConfirm = true }, modifier = Modifier.fillMaxWidth()) { Text("Clear local data") }
                TextButton(onClick = { showResetConfirm = true }, modifier = Modifier.fillMaxWidth()) { Text("Reset settings") }
                TextButton(onClick = ::openRateApp, modifier = Modifier.fillMaxWidth()) { Text("Rate app") }
                TextButton(onClick = ::shareApp, modifier = Modifier.fillMaxWidth()) { Text("Share app") }

                Text("Version 1.0", modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp))
            }
        }
    }
}

@Composable
private fun FilterChipLike(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.padding(vertical = 4.dp)) { Text(text) }
}
