package com.example.mismatchhunter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mismatchhunter.data.local.EpisodeEntity
import com.example.mismatchhunter.data.local.NoteEntity
import com.example.mismatchhunter.data.local.SessionEntity
import com.example.mismatchhunter.data.local.EpisodeDraft
import com.example.mismatchhunter.data.local.PlaybookDraft
import com.example.mismatchhunter.data.local.SessionDraft
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
import kotlinx.coroutines.flow.first
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
                settingsRepository.settings.first()
            }.onSuccess { settings ->
                _state.value = PreloaderUiState(loading = false, route = if (settings.onboardingCompleted) "main" else "onboarding1")
            }.onFailure {
                _state.value = PreloaderUiState(loading = false, error = "Ошибка инициализации")
            }
        }
    }

    fun retry() {
        _state.value = PreloaderUiState()
    }

}

class OnboardingViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    fun complete(onDone: () -> Unit) = viewModelScope.launch {
        settingsRepository.completeOnboarding()
        onDone()
    }}

data class HomeUiState(val sessions: List<SessionEntity> = emptyList(), val recent: List<EpisodeEntity> = emptyList())
class HomeViewModel(sessionRepository: SessionRepository, episodeRepository: EpisodeRepository) : ViewModel() {
    val state = combine(sessionRepository.observeSessions(), episodeRepository.observeAllEpisodes()) { s, e ->
        HomeUiState(s, e.take(5))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}

class CreateSessionViewModel(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val title = MutableStateFlow("")
    val matchType = MutableStateFlow("Обычный")
    val description = MutableStateFlow("")
    val error = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val draft = settingsRepository.getSessionDraft()
            title.value = draft.title
            matchType.value = draft.matchType
            description.value = draft.description
        }
        viewModelScope.launch {
            combine(title, matchType, description) { title, matchType, description ->
                SessionDraft(title = title, matchType = matchType, description = description)
            }.collect { draft ->
                settingsRepository.saveSessionDraft(draft)
            }
        }
    }

    fun save(onSaved: (Long) -> Unit) = viewModelScope.launch {
        if (title.value.length < 3) {
            error.value = "Title must be at least 3 characters"
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
        settingsRepository.clearSessionDraft()
        onSaved(id)
    }
}

data class SessionDetailUiState(
    val session: SessionEntity? = null,
    val episodes: List<EpisodeEntity> = emptyList(),
    val positionFilter: String = "All",
    val switchTypeFilter: String = "All",
    val resultFilter: String = "All",
    val availablePositions: List<String> = listOf("All"),
    val availableSwitchTypes: List<String> = listOf("All"),
    val availableResults: List<String> = listOf("All")
)

class SessionDetailViewModel(
    sessionId: Long,
    sessionRepository: SessionRepository,
    episodeRepository: EpisodeRepository
) : ViewModel() {
    private val positionFilter = MutableStateFlow("All")
    private val switchTypeFilter = MutableStateFlow("All")
    private val resultFilter = MutableStateFlow("All")

    val state = combine(
        sessionRepository.observeSession(sessionId),
        episodeRepository.observeEpisodes(sessionId),
        positionFilter,
        switchTypeFilter,
        resultFilter
    ) { session, episodes, pos, switchType, result ->
        val filtered = episodes.filter {
            (pos == "All" || it.opponentPosition == pos) &&
                (switchType == "All" || it.switchType == switchType) &&
                (result == "All" || it.result == result)
        }
        SessionDetailUiState(
            session = session,
            episodes = filtered,
            positionFilter = pos,
            switchTypeFilter = switchType,
            resultFilter = result,
            availablePositions = listOf("All") + episodes.map { it.opponentPosition }.distinct().sorted(),
            availableSwitchTypes = listOf("All") + episodes.map { it.switchType }.distinct().sorted(),
            availableResults = listOf("All") + episodes.map { it.result }.distinct().sorted()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionDetailUiState())

    fun setPositionFilter(value: String) {
        positionFilter.value = value
    }

    fun setResultFilter(value: String) {
        resultFilter.value = value
    }

    fun setSwitchTypeFilter(value: String) {
        switchTypeFilter.value = value
    }
}

class EpisodeEntryViewModel(
    private val repository: EpisodeRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val position = MutableStateFlow("")
    val switchType = MutableStateFlow("")
    val zone = MutableStateFlow("")
    val decision = MutableStateFlow("")
    val result = MutableStateFlow("")
    val error = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val draft = settingsRepository.getEpisodeDraft()
            position.value = draft.position
            switchType.value = draft.switchType
            zone.value = draft.zone
            decision.value = draft.decision
            result.value = draft.result
        }
        viewModelScope.launch {
            combine(position, switchType, zone, decision, result) { position, switchType, zone, decision, result ->
                EpisodeDraft(position, switchType, zone, decision, result)
            }.collect { draft ->
                settingsRepository.saveEpisodeDraft(draft)
            }
        }
    }

    fun save(sessionId: Long, onSaved: () -> Unit) = viewModelScope.launch {
        val required = listOf(position.value, switchType.value, zone.value, decision.value, result.value)
        if (required.any { it.isBlank() }) {
            error.value = "Fill in all required steps"
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
        settingsRepository.clearEpisodeDraft()
        onSaved()
    }
}

class EpisodeDetailViewModel(
    private val episodeId: Long,
    private val repository: EpisodeRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {
    val episode = repository.observeEpisode(episodeId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun saveNote(note: String, onSaved: () -> Unit) = viewModelScope.launch {
        val current = episode.value ?: return@launch
        val normalizedNote = note.trim()
        repository.updateEpisode(current.copy(tacticalNote = normalizedNote))
        if (normalizedNote.isNotBlank()) {
            val title = "Эпизод: ${current.opponentPosition} / ${current.switchType}"
            noteRepository.saveEpisodeNote(
                episodeId = current.id,
                title = title,
                body = normalizedNote,
                sessionId = current.sessionId
            )
        }
        onSaved()
    }
}

data class AnalyticsUiState(val byPosition: Map<String, Int> = emptyMap(), val byZone: Map<String, Int> = emptyMap(), val successRate: Int = 0)
class AnalyticsViewModel(episodeRepository: EpisodeRepository) : ViewModel() {
    val state = episodeRepository.observeAllEpisodes().combine(MutableStateFlow(Unit)) { episodes, _ ->
        val total = episodes.size.coerceAtLeast(1)
        val successful = episodes.count { it.result == "Гол" || it.result == "Заработанный фол" }
        AnalyticsUiState(
            byPosition = episodes.groupingBy { it.opponentPosition }.eachCount(),
            byZone = episodes.groupingBy { it.courtZone }.eachCount(),
            successRate = (successful * 100) / total
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())
}

class PlaybookViewModel(
    private val noteRepository: NoteRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val notes = noteRepository.observeNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val title = MutableStateFlow("")
    val body = MutableStateFlow("")

    init {
        viewModelScope.launch {
            val draft = settingsRepository.getPlaybookDraft()
            title.value = draft.title
            body.value = draft.body
        }
        viewModelScope.launch {
            combine(title, body) { title, body -> PlaybookDraft(title, body) }
                .collect { draft -> settingsRepository.savePlaybookDraft(draft) }
        }
    }

    fun save() = viewModelScope.launch {
        if (title.value.isBlank() || body.value.isBlank()) return@launch
        noteRepository.save(NoteEntity(title = title.value, body = body.value))
        title.value = ""
        body.value = ""
        settingsRepository.clearPlaybookDraft()
    }
}

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.mismatchhunter.data.local.AppSettings())
    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDarkTheme(enabled) }
    fun setAccent(accent: String) = viewModelScope.launch { settingsRepository.setAccent(accent) }
    fun setSeasonalNotifications(enabled: Boolean) = viewModelScope.launch { settingsRepository.setSeasonalNotifications(enabled) }
    fun reset(onDone: () -> Unit = {}) = viewModelScope.launch {
        settingsRepository.reset()
        onDone()
    }    fun clearLocalData() = viewModelScope.launch { settingsRepository.clearLocalData() }
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
            modelClass.isAssignableFrom(CreateSessionViewModel::class.java) -> CreateSessionViewModel(sessionRepository, settingsRepository)
            modelClass.isAssignableFrom(SessionDetailViewModel::class.java) -> SessionDetailViewModel(sessionId ?: 0, sessionRepository, episodeRepository)
            modelClass.isAssignableFrom(EpisodeEntryViewModel::class.java) -> EpisodeEntryViewModel(episodeRepository, settingsRepository)
            modelClass.isAssignableFrom(EpisodeDetailViewModel::class.java) -> EpisodeDetailViewModel(episodeId ?: 0, episodeRepository, noteRepository)
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> AnalyticsViewModel(episodeRepository)
            modelClass.isAssignableFrom(PlaybookViewModel::class.java) -> PlaybookViewModel(noteRepository, settingsRepository)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(settingsRepository)
            else -> error("Неизвестный класс модели: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return vm as T
    }
}
