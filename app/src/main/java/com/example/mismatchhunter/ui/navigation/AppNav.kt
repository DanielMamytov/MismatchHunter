package com.example.mismatchhunter.ui.navigation

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
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
            val vm: PreloaderViewModel = viewModel(factory = AppViewModelFactory(appContainer.settingsRepository, appContainer.sessionRepository, appContainer.episodeRepository, appContainer.noteRepository))
            val state by vm.state.collectAsState()
            LaunchedEffect(state.route) {
                if (state.route != null) navController.navigate(state.route!!) { popUpTo("preloader") { inclusive = true } }
            }
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.loading) CircularProgressIndicator()
                Text("Инициализация базы, зависимостей и темы", modifier = Modifier.padding(top = 16.dp))
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
        composable("onboarding1") {
            val vm: OnboardingViewModel = viewModel(factory = AppViewModelFactory(appContainer.settingsRepository, appContainer.sessionRepository, appContainer.episodeRepository, appContainer.noteRepository))
            OnboardingScreen(
                title = "Mismatch Hunter",
                text = "Фиксируйте и анализируйте каждый размен, находите слабые места соперника.",
                button = "Далее"
            ) { navController.navigate("onboarding2") }
        }
        composable("onboarding2") {
            val vm: OnboardingViewModel = viewModel(factory = AppViewModelFactory(appContainer.settingsRepository, appContainer.sessionRepository, appContainer.episodeRepository, appContainer.noteRepository))
            OnboardingScreen(
                title = "Пошаговый ввод эпизодов",
                text = "Позиция, тип размена, зона, решение и результат — все в одном потоке.",
                button = "Начать"
            ) {
                vm.complete()
                navController.navigate("main") { popUpTo("onboarding1") { inclusive = true } }
            }
        }
        composable("main") { MainTabs(appContainer, navController) }
        composable("create_session") {
            val vm: CreateSessionViewModel = viewModel(factory = AppViewModelFactory(appContainer.settingsRepository, appContainer.sessionRepository, appContainer.episodeRepository, appContainer.noteRepository))
            CreateSessionScreen(vm) { id -> navController.navigate("session/$id") }
        }
        composable("session/{id}") { backStack ->
            val sessionId = backStack.arguments?.getString("id")?.toLong() ?: 0L
            val vm: SessionDetailViewModel = viewModel(factory = AppViewModelFactory(appContainer.settingsRepository, appContainer.sessionRepository, appContainer.episodeRepository, appContainer.noteRepository, sessionId = sessionId))
            SessionDetailScreen(vm,
                openAddEpisode = { navController.navigate("session/$sessionId/add_episode") },
                openEpisode = { episodeId -> navController.navigate("episode/$episodeId") })
        }
        composable("session/{id}/add_episode") { backStack ->
            val sessionId = backStack.arguments?.getString("id")?.toLong() ?: 0L
            val vm: EpisodeEntryViewModel = viewModel(factory = AppViewModelFactory(appContainer.settingsRepository, appContainer.sessionRepository, appContainer.episodeRepository, appContainer.noteRepository))
            EpisodeEntryScreen(vm) { vm.save(sessionId) { navController.popBackStack() } }
        }
        composable("episode/{id}") { backStack ->
            val episodeId = backStack.arguments?.getString("id")?.toLong() ?: 0L
            val vm: EpisodeDetailViewModel = viewModel(factory = AppViewModelFactory(appContainer.settingsRepository, appContainer.sessionRepository, appContainer.episodeRepository, appContainer.noteRepository, episodeId = episodeId))
            EpisodeDetailScreen(vm)
        }
    }
}

@Composable private fun OnboardingScreen(title: String, text: String, button: String, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
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
    val items = listOf("sessions", "analytics", "playbook", "settings")
    val entry by tabsNav.currentBackStackEntryAsState()
    Scaffold(bottomBar = {
        NavigationBar {
            items.forEach { tab ->
                NavigationBarItem(
                    selected = entry?.destination?.route == tab,
                    onClick = { tabsNav.navigate(tab) { popUpTo(tabsNav.graph.findStartDestination().id) { saveState = true }; launchSingleTop = true } },
                    label = { Text(tab.replaceFirstChar { it.uppercase() }) },
                    icon = {
                        Icon(
                            painter = painterResource(
                                id = when (tab) {
                                    "sessions" -> R.drawable.ic_sessions
                                    "analytics" -> R.drawable.ic_analytics
                                    "playbook" -> R.drawable.ic_playbook
                                    else -> R.drawable.ic_settings
                                }
                            ),
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }) { p ->
        NavHost(tabsNav, startDestination = "sessions", modifier = Modifier.padding(p)) {
            composable("sessions") {
                val vm: HomeViewModel = viewModel(factory = AppViewModelFactory(appContainer.settingsRepository, appContainer.sessionRepository, appContainer.episodeRepository, appContainer.noteRepository))
                val state by vm.state.collectAsState()
                HomeScreen(state.recent, onCreateSession = { rootNav.navigate("create_session") }, onOpenSession = { rootNav.navigate("session/$it") })
            }
            composable("analytics") {
                val vm: AnalyticsViewModel = viewModel(factory = AppViewModelFactory(appContainer.settingsRepository, appContainer.sessionRepository, appContainer.episodeRepository, appContainer.noteRepository))
                val state by vm.state.collectAsState()
                AnalyticsScreen(state.byPosition, state.byZone, state.successRate)
            }
            composable("playbook") {
                val vm: PlaybookViewModel = viewModel(factory = AppViewModelFactory(appContainer.settingsRepository, appContainer.sessionRepository, appContainer.episodeRepository, appContainer.noteRepository))
                PlaybookScreen(vm)
            }
            composable("settings") {
                val vm: SettingsViewModel = viewModel(factory = AppViewModelFactory(appContainer.settingsRepository, appContainer.sessionRepository, appContainer.episodeRepository, appContainer.noteRepository))
                SettingsScreen(vm)
            }
        }
    }
}

@Composable
private fun HomeScreen(recent: List<EpisodeEntity>, onCreateSession: () -> Unit, onOpenSession: (Long) -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Сессии", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = onCreateSession) { Text("Новая") }
        }
        Text("Последние эпизоды: ${recent.size}")
        if (recent.isEmpty()) Text("Пока нет эпизодов. Создайте сессию.")
        LazyColumn {
            items(recent) { ep ->
                Card(onClick = { onOpenSession(ep.sessionId) }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
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
    val title by vm.title.collectAsState(); val match by vm.matchType.collectAsState(); val description by vm.description.collectAsState(); val error by vm.error.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Создать сессию", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = title, onValueChange = { vm.title.value = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = match, onValueChange = { vm.matchType.value = it }, label = { Text("Тип матча") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = description, onValueChange = { vm.description.value = it }, label = { Text("Описание") }, modifier = Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { vm.save(onSaved) }, modifier = Modifier.padding(top = 12.dp)) { Text("Сохранить") }
    }
}

@Composable
private fun SessionDetailScreen(vm: SessionDetailViewModel, openAddEpisode: () -> Unit, openEpisode: (Long) -> Unit) {
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(state.session?.title ?: "Сессия", style = MaterialTheme.typography.headlineSmall)
        Text(state.session?.let { DateUtils.formatEpochDay(it.dateEpochDay) } ?: "")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FilterChipLike("Позиция: ${state.positionFilter}") { vm.setPositionFilter(if (state.positionFilter == "All") "PG" else "All") }
            FilterChipLike("Результат: ${state.resultFilter}") { vm.setResultFilter(if (state.resultFilter == "All") "Score" else "All") }
        }
        Button(onClick = openAddEpisode, modifier = Modifier.padding(vertical = 8.dp)) { Text("Добавить эпизод") }
        if (state.episodes.isEmpty()) Text("Нет данных для фильтра")
        LazyColumn { items(state.episodes) { ep -> Card(onClick = { openEpisode(ep.id) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text("${ep.opponentPosition} • ${ep.result}", Modifier.padding(12.dp)) } } }
    }
}

@Composable
private fun EpisodeEntryScreen(vm: EpisodeEntryViewModel, onSave: () -> Unit) {
    val fields = listOf(vm.position, vm.switchType, vm.zone, vm.decision, vm.result)
    val labels = listOf("Позиция", "Тип размена", "Зона", "Решение", "Результат")
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Фиксация эпизода", style = MaterialTheme.typography.headlineSmall)
        fields.forEachIndexed { index, state ->
            OutlinedTextField(value = state.collectAsState().value, onValueChange = { state.value = it }, label = { Text("${index + 1}/5 ${labels[index]}") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
        }
        vm.error.collectAsState().value?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSave, modifier = Modifier.padding(top = 8.dp)) { Text("Сохранить") }
    }
}

@Composable
private fun EpisodeDetailScreen(vm: EpisodeDetailViewModel) {
    val episode by vm.episode.collectAsState()
    var note by remember { mutableStateOf(episode?.tacticalNote ?: "") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (episode == null) Text("Загрузка...") else {
            Text("${episode?.opponentPosition} / ${episode?.switchType}", style = MaterialTheme.typography.headlineSmall)
            Text("Зона: ${episode?.courtZone}")
            Text("Решение: ${episode?.decision}")
            Text("Результат: ${episode?.result}")
            OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Тактическая заметка") }, modifier = Modifier.fillMaxWidth())
            Button(onClick = { vm.saveNote(note) }, modifier = Modifier.padding(top = 8.dp)) { Text("Сохранить заметку") }
        }
    }
}

@Composable private fun AnalyticsScreen(byPosition: Map<String, Int>, byZone: Map<String, Int>, successRate: Int) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Аналитика", style = MaterialTheme.typography.headlineSmall)
        Text("Эффективность атакующих решений: $successRate%")
        Text("По позициям")
        byPosition.forEach { (k, v) -> Text("$k: $v") }
        Spacer(Modifier.height(8.dp))
        Text("По зонам")
        byZone.forEach { (k, v) -> Text("$k: $v") }
    }
}

@Composable private fun PlaybookScreen(vm: PlaybookViewModel) {
    val notes by vm.notes.collectAsState(); val title by vm.title.collectAsState(); val body by vm.body.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Тактические заметки", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(value = title, onValueChange = { vm.title.value = it }, label = { Text("Заголовок") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = body, onValueChange = { vm.body.value = it }, label = { Text("Текст") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { vm.save() }, modifier = Modifier.padding(vertical = 8.dp)) { Text("Сохранить") }
        LazyColumn { items(notes) { note -> Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Column(Modifier.padding(12.dp)) { Text(note.title, fontWeight = FontWeight.Bold); Text(note.body) } } } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun SettingsScreen(vm: SettingsViewModel) {
    val settings by vm.settings.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Настройки", style = MaterialTheme.typography.headlineSmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Тёмная тема")
            Switch(checked = settings.darkTheme, onCheckedChange = vm::setDarkTheme)
        }
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(value = settings.accentColor, onValueChange = {}, readOnly = true, label = { Text("Акцент") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                listOf("blue", "lightBlue").forEach {
                    DropdownMenuItem(text = { Text(it) }, onClick = { vm.setAccent(it); expanded = false })
                }
            }
        }
        Button(onClick = vm::reset, modifier = Modifier.padding(top = 12.dp)) { Text("Сбросить настройки") }
        Text("Версия: 1.0", modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable private fun FilterChipLike(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.padding(vertical = 4.dp)) { Text(text) }
}
