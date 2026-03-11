package com.example.mismatchhunter.ui.navigation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mismatchhunter.R
import com.example.mismatchhunter.data.local.EpisodeEntity
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
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
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
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
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
                    state.recent,
                    onCreateSession = { rootNav.navigate("create_session") },
                    onOpenSession = { rootNav.navigate("session/$it") })
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
                AnalyticsScreen(state.byPosition, state.byZone, state.successRate)
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
    "sessions" -> "Сессии"
    "analytics" -> "Аналитика"
    "playbook" -> "Заметки"
    "settings" -> "Настройки"
    else -> tab
}

@Composable
private fun HomeScreen(
    recent: List<EpisodeEntity>,
    onCreateSession: () -> Unit,
    onOpenSession: (Long) -> Unit
) {
    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Sessions", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onCreateSession) { Text("New") }
        }
        Text("Recent episodes: ${recent.size}")
        if (recent.isEmpty()) Text("No episodes yet. Create a session.")
        LazyColumn {
            items(recent) { ep ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    onClick = { onOpenSession(ep.sessionId) }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${ep.opponentPosition} • ${ep.switchType}")
                        Text("${ep.result} • ${DateUtils.formatMillis(ep.createdAt)}")
                    }
                }
            }
        }
    }
}
@Composable
private fun CreateSessionScreen(vm: CreateSessionViewModel, onSaved: (Long) -> Unit) {
    val title by vm.title.collectAsState()
    val match by vm.matchType.collectAsState()
    val description by vm.description.collectAsState()
    val error by vm.error.collectAsState()
    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Spacer(Modifier.height(15.dp))

        Text("Create session", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = title,
            onValueChange = { vm.title.value = it },
            label = { Text("Title") },
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
private fun SessionDetailScreen(
    vm: SessionDetailViewModel,
    openAddEpisode: () -> Unit,
    openEpisode: (Long) -> Unit
) {
    val state by vm.state.collectAsState()
    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Spacer(Modifier.height(15.dp))
        Text(state.session?.title ?: "Session", style = MaterialTheme.typography.headlineSmall)
        Text(state.session?.let { DateUtils.formatEpochDay(it.dateEpochDay) } ?: "")

        FilterDropdown(
            label = "Opponent position",
            selected = state.positionFilter,
            options = state.availablePositions,
            onSelected = vm::setPositionFilter
        )
        FilterDropdown(
            label = "Mismatch type",
            selected = state.switchTypeFilter,
            options = state.availableSwitchTypes,
            onSelected = vm::setSwitchTypeFilter
        )
        FilterDropdown(
            label = "Result",
            selected = state.resultFilter,
            options = state.availableResults,
            onSelected = vm::setResultFilter
        )

        Button(
            onClick = openAddEpisode,
            modifier = Modifier.padding(vertical = 8.dp)
        ) { Text("Add episode") }
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

@Composable
private fun EpisodeEntryScreen(vm: EpisodeEntryViewModel, onSave: () -> Unit) {
    val fields = listOf(vm.position, vm.switchType, vm.zone, vm.decision, vm.result)
    val labels = listOf("Position", "Mismatch type", "Zone", "Decision", "Result")
    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Episode entry", style = MaterialTheme.typography.headlineSmall)
        fields.forEachIndexed { index, state ->
            OutlinedTextField(
                value = state.collectAsState().value,
                onValueChange = { state.value = it },
                label = { Text("${index + 1}/5 ${labels[index]}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }

        vm.error.collectAsState().value?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSave, modifier = Modifier.padding(top = 8.dp)) { Text("Save") }
    }
}

@Composable
private fun EpisodeDetailScreen(vm: EpisodeDetailViewModel, onSaved: () -> Unit) {
    val episode by vm.episode.collectAsState()
    var note by remember { mutableStateOf("") }

    LaunchedEffect(episode?.id) {
        note = episode?.tacticalNote.orEmpty()
    }

    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        if (episode == null) Text("Loading...") else {
            Text(
                "${episode?.opponentPosition} / ${episode?.switchType}",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 32.dp)  // Added top padding here
            )
            Text("Zone: ${episode?.courtZone}")
            Text("Decision: ${episode?.decision}")
            Text("Result: ${episode?.result}")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Tactical note") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = { vm.saveNote(note, onSaved) }, modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    "Save note"
                )
            }
        }
    }
}


@Composable
private fun AnalyticsScreen(
    byPosition: Map<String, Int>,
    byZone: Map<String, Int>,
    successRate: Int
) {
    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Analytics", style = MaterialTheme.typography.headlineSmall)
        Text("Offensive decision success rate: $successRate%")
        Text("By position")
        byPosition.forEach { (k, v) -> Text("$k: $v") }
        Spacer(Modifier.height(8.dp))
        Text("By zone")
        byZone.forEach { (k, v) -> Text("$k: $v") }
    }
}

@Composable
private fun PlaybookScreen(vm: PlaybookViewModel) {
    val notes by vm.notes.collectAsState();
    val title by vm.title.collectAsState();
    val body by vm.body.collectAsState()
    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("Playbook notes", style = MaterialTheme.typography.headlineSmall)
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
                    }
                }
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
    Column(Modifier
        .fillMaxSize()
        .padding(16.dp)) {
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
            Text("Сезонные уведомления")
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
        Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp)) {
                TextButton(onClick = vm::clearLocalData, modifier = Modifier.fillMaxWidth()) { Text("Очистить локальные данные") }
                TextButton(onClick = { vm.reset(onResetDone) }, modifier = Modifier.fillMaxWidth()) { Text("Сбросить настройки") }
                TextButton(onClick = ::openRateApp, modifier = Modifier.fillMaxWidth()) { Text("Оценить приложение") }
                TextButton(onClick = ::shareApp, modifier = Modifier.fillMaxWidth()) { Text("Поделиться приложением") }

                Text("Версия 1.0", modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
