package com.example.mismatchhunter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mismatchhunter.data.local.EpisodeEntity
import com.example.mismatchhunter.data.local.NoteEntity
import com.example.mismatchhunter.data.local.SessionEntity
import com.example.mismatchhunter.data.repository.EpisodeRepository
import com.example.mismatchhunter.data.repository.NoteRepository
import com.example.mismatchhunter.data.repository.SessionRepository
import com.example.mismatchhunter.data.repository.SettingsRepository
import com.example.mismatchhunter.utils.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PreloaderUiState(val loading: Boolean = true, val error: String? = null, val route: String? = null)

class PreloaderViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow(PreloaderUiState())
    val state: StateFlow<PreloaderUiState> = _state

    init {
        viewModelScope.launch {
            runCatching {
                delay(1400)
                settingsRepository.settings.stateIn(viewModelScope).value
            }.onSuccess { settings ->
                _state.value = PreloaderUiState(loading = false, route = if (settings.onboardingCompleted) "main" else "onboarding1")
            }.onFailure {
                _state.value = PreloaderUiState(loading = false, error = "Initialization failed")
            }
        }
    }

    fun retry() {
        _state.value = PreloaderUiState()
    }
}

class OnboardingViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    fun complete() = viewModelScope.launch { settingsRepository.completeOnboarding() }
}

data class HomeUiState(val sessions: List<SessionEntity> = emptyList(), val recent: List<EpisodeEntity> = emptyList())
class HomeViewModel(sessionRepository: SessionRepository, episodeRepository: EpisodeRepository) : ViewModel() {
    val state = combine(sessionRepository.observeSessions(), episodeRepository.observeAllEpisodes()) { s, e ->
        HomeUiState(s, e.take(5))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}

class CreateSessionViewModel(private val sessionRepository: SessionRepository) : ViewModel() {
    val title = MutableStateFlow("")
    val matchType = MutableStateFlow("Regular")
    val description = MutableStateFlow("")
    val error = MutableStateFlow<String?>(null)

    fun save(onSaved: (Long) -> Unit) = viewModelScope.launch {
        if (title.value.length < 3) {
            error.value = "Название минимум 3 символа"
            return@launch
        }
        val id = sessionRepository.createSession(
            SessionEntity(
                title = title.value,
                matchType = matchType.value,
                description = description.value,
                dateEpochDay = DateUtils.epochDayNow()
            )
        )
        onSaved(id)
    }
}

data class SessionDetailUiState(
    val session: SessionEntity? = null,
    val episodes: List<EpisodeEntity> = emptyList(),
    val positionFilter: String = "All",
    val resultFilter: String = "All"
)

class SessionDetailViewModel(
    sessionId: Long,
    sessionRepository: SessionRepository,
    episodeRepository: EpisodeRepository
) : ViewModel() {
    private val positionFilter = MutableStateFlow("All")
    private val resultFilter = MutableStateFlow("All")

    val state = combine(
        sessionRepository.observeSession(sessionId),
        episodeRepository.observeEpisodes(sessionId),
        positionFilter,
        resultFilter
    ) { session, episodes, pos, result ->
        val filtered = episodes.filter { (pos == "All" || it.opponentPosition == pos) && (result == "All" || it.result == result) }
        SessionDetailUiState(session, filtered, pos, result)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionDetailUiState())

    fun setPositionFilter(value: String) {
        positionFilter.value = value
    }

    fun setResultFilter(value: String) {
        resultFilter.value = value
    }
}

class EpisodeEntryViewModel(private val repository: EpisodeRepository) : ViewModel() {
    val position = MutableStateFlow("")
    val switchType = MutableStateFlow("")
    val zone = MutableStateFlow("")
    val decision = MutableStateFlow("")
    val result = MutableStateFlow("")
    val error = MutableStateFlow<String?>(null)

    fun save(sessionId: Long, onSaved: () -> Unit) = viewModelScope.launch {
        val required = listOf(position.value, switchType.value, zone.value, decision.value, result.value)
        if (required.any { it.isBlank() }) {
            error.value = "Заполните все обязательные шаги"
            return@launch
        }
        repository.createEpisode(
            EpisodeEntity(
                sessionId = sessionId,
                opponentPosition = position.value,
                switchType = switchType.value,
                courtZone = zone.value,
                decision = decision.value,
                result = result.value
            )
        )
        onSaved()
    }
}

class EpisodeDetailViewModel(private val episodeId: Long, private val repository: EpisodeRepository) : ViewModel() {
    val episode = repository.observeEpisode(episodeId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveNote(note: String) = viewModelScope.launch {
        val current = episode.value ?: return@launch
        repository.updateEpisode(current.copy(tacticalNote = note))
    }
}

data class AnalyticsUiState(val byPosition: Map<String, Int> = emptyMap(), val byZone: Map<String, Int> = emptyMap(), val successRate: Int = 0)
class AnalyticsViewModel(episodeRepository: EpisodeRepository) : ViewModel() {
    val state = episodeRepository.observeAllEpisodes().combine(MutableStateFlow(Unit)) { episodes, _ ->
        val total = episodes.size.coerceAtLeast(1)
        val successful = episodes.count { it.result == "Score" || it.result == "Foul Drawn" }
        AnalyticsUiState(
            byPosition = episodes.groupingBy { it.opponentPosition }.eachCount(),
            byZone = episodes.groupingBy { it.courtZone }.eachCount(),
            successRate = (successful * 100) / total
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())
}

class PlaybookViewModel(private val noteRepository: NoteRepository) : ViewModel() {
    val notes = noteRepository.observeNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val title = MutableStateFlow("")
    val body = MutableStateFlow("")

    fun save() = viewModelScope.launch {
        if (title.value.isBlank() || body.value.isBlank()) return@launch
        noteRepository.save(NoteEntity(title = title.value, body = body.value))
        title.value = ""
        body.value = ""
    }
}

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.mismatchhunter.data.local.AppSettings())
    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDarkTheme(enabled) }
    fun setAccent(accent: String) = viewModelScope.launch { settingsRepository.setAccent(accent) }
    fun reset() = viewModelScope.launch { settingsRepository.reset() }
}

class AppViewModelFactory(
    private val settingsRepository: SettingsRepository,
    private val sessionRepository: SessionRepository,
    private val episodeRepository: EpisodeRepository,
    private val noteRepository: NoteRepository,
    private val sessionId: Long? = null,
    private val episodeId: Long? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val vm = when {
            modelClass.isAssignableFrom(PreloaderViewModel::class.java) -> PreloaderViewModel(settingsRepository)
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> OnboardingViewModel(settingsRepository)
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(sessionRepository, episodeRepository)
            modelClass.isAssignableFrom(CreateSessionViewModel::class.java) -> CreateSessionViewModel(sessionRepository)
            modelClass.isAssignableFrom(SessionDetailViewModel::class.java) -> SessionDetailViewModel(sessionId ?: 0, sessionRepository, episodeRepository)
            modelClass.isAssignableFrom(EpisodeEntryViewModel::class.java) -> EpisodeEntryViewModel(episodeRepository)
            modelClass.isAssignableFrom(EpisodeDetailViewModel::class.java) -> EpisodeDetailViewModel(episodeId ?: 0, episodeRepository)
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> AnalyticsViewModel(episodeRepository)
            modelClass.isAssignableFrom(PlaybookViewModel::class.java) -> PlaybookViewModel(noteRepository)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(settingsRepository)
            else -> error("Unknown model class: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return vm as T
    }
}
