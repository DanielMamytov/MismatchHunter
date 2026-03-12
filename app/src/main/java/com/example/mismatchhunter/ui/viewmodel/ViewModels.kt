package com.example.mismatchhunter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.mismatchhunter.data.local.EpisodeDraft
import com.example.mismatchhunter.data.local.EpisodeEntity
import com.example.mismatchhunter.data.local.NoteEntity
import com.example.mismatchhunter.data.local.PlaybookDraft
import com.example.mismatchhunter.data.local.SessionDraft
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PreloaderUiState(val loading: Boolean = true, val error: String? = null, val route: String? = null)

class PreloaderViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    private val _state = MutableStateFlow(PreloaderUiState())
    val state: StateFlow<PreloaderUiState> = _state

    init {
        initialize()
    }

    private fun initialize() {
        viewModelScope.launch {
            runCatching {
                delay(1400)
                settingsRepository.settings.first()
            }.onSuccess { settings ->
                _state.value = PreloaderUiState(loading = false, route = if (settings.onboardingCompleted) "main" else "onboarding1")
            }.onFailure {
                _state.value = PreloaderUiState(loading = false, error = "Initialization error")
            }
        }
    }

    fun retry() {
        _state.value = PreloaderUiState()
        initialize()
    }
}

class OnboardingViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    fun complete(onDone: () -> Unit) = viewModelScope.launch {
        settingsRepository.completeOnboarding()
        onDone()
    }
}

data class HomeUiState(
    val sessions: List<SessionEntity> = emptyList(),
    val recent: List<EpisodeEntity> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(sessionRepository: SessionRepository, episodeRepository: EpisodeRepository) : ViewModel() {
    val state = combine(sessionRepository.observeSessions(), episodeRepository.observeAllEpisodes()) { s, e ->
        HomeUiState(sessions = s, recent = e.take(5), loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun retry() = Unit
}

class CreateSessionViewModel(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val title = MutableStateFlow("")
    val date = MutableStateFlow(DateUtils.todayIso())
    val matchType = MutableStateFlow("Regular")
    val description = MutableStateFlow("")
    val error = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            val draft = settingsRepository.getSessionDraft()
            title.value = draft.title
            date.value = draft.date
            matchType.value = draft.matchType
            description.value = draft.description
        }
        viewModelScope.launch {
            combine(title, date, matchType, description) { title, date, matchType, description ->
                SessionDraft(title = title, date = date, matchType = matchType, description = description)
            }.collect { draft ->
                settingsRepository.saveSessionDraft(draft)
            }
        }
    }

    private fun validate(): String? {
        if (title.value.length < 3) return "Title must be at least 3 characters"
        if (DateUtils.parseIsoToEpochDay(date.value) == null) return "Date must be in format YYYY-MM-DD"
        if (matchType.value.isBlank()) return "Match type is required"
        return null
    }

    fun save(onSaved: (Long) -> Unit) = viewModelScope.launch {
        val validationError = validate()
        if (validationError != null) {
            error.value = validationError
            return@launch
        }

        val id = sessionRepository.createSession(
            SessionEntity(
                title = title.value,
                matchType = matchType.value,
                description = description.value,
                dateEpochDay = DateUtils.parseIsoToEpochDay(date.value) ?: DateUtils.epochDayNow()
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
    val availableResults: List<String> = listOf("All"),
    val loading: Boolean = true,
    val error: String? = null
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
            availableResults = listOf("All") + episodes.map { it.result }.distinct().sorted(),
            loading = false
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

    fun retry() = Unit
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

data class EpisodeDetailUiState(
    val loading: Boolean = true,
    val episode: EpisodeEntity? = null,
    val error: String? = null
)

class EpisodeDetailViewModel(
    private val episodeId: Long,
    private val repository: EpisodeRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {
    private val reload = MutableStateFlow(0)

    val state = reload.combine(repository.observeEpisode(episodeId)) { _, episode ->
        if (episode == null) EpisodeDetailUiState(loading = false, error = "Failed to load episode")
        else EpisodeDetailUiState(loading = false, episode = episode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EpisodeDetailUiState())

    fun retry() {
        reload.value += 1
    }

    fun saveNote(note: String, onSaved: () -> Unit) = viewModelScope.launch {
        val current = state.value.episode ?: return@launch
        val normalizedNote = note.trim()
        repository.updateEpisode(current.copy(tacticalNote = normalizedNote))
        if (normalizedNote.isNotBlank()) {
            val title = "Episode: ${current.opponentPosition} / ${current.switchType}"
            noteRepository.saveEpisodeNote(
                episodeId = current.id,
                title = title,
                body = normalizedNote,
                sessionId = current.sessionId
            )
        }
        onSaved()
    }

    fun saveEpisodeFields(
        position: String,
        switchType: String,
        zone: String,
        decision: String,
        result: String,
        onSaved: () -> Unit
    ) = viewModelScope.launch {
        val current = state.value.episode ?: return@launch
        repository.updateEpisode(
            current.copy(
                opponentPosition = position,
                switchType = switchType,
                courtZone = zone,
                decision = decision,
                result = result
            )
        )
        onSaved()
    }
}

data class AnalyticsUiState(
    val byPosition: Map<String, Int> = emptyMap(),
    val byZone: Map<String, Int> = emptyMap(),
    val successRate: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,
    val sessions: List<SessionEntity> = emptyList(),
    val selectedSessionId: Long? = null,
    val selectedPeriod: String = "All time"
)

class AnalyticsViewModel(
    private val episodeRepository: EpisodeRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val selectedSessionId = MutableStateFlow<Long?>(null)
    private val selectedPeriod = MutableStateFlow("All time")

    val state = combine(
        episodeRepository.observeAllEpisodes(),
        sessionRepository.observeSessions(),
        selectedSessionId,
        selectedPeriod
    ) { episodes, sessions, sessionId, period ->
        val filteredBySession = if (sessionId == null) episodes else episodes.filter { it.sessionId == sessionId }
        val filtered = when (period) {
            "Last 7 days" -> filteredBySession.filter { it.createdAt >= System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L }
            "Last 30 days" -> filteredBySession.filter { it.createdAt >= System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L }
            else -> filteredBySession
        }
        val total = filtered.size.coerceAtLeast(1)
        val successful = filtered.count { it.result == "Goal" || it.result == "Drawn foul" }
        AnalyticsUiState(
            byPosition = filtered.groupingBy { it.opponentPosition }.eachCount(),
            byZone = filtered.groupingBy { it.courtZone }.eachCount(),
            successRate = (successful * 100) / total,
            loading = false,
            sessions = sessions,
            selectedSessionId = sessionId,
            selectedPeriod = period
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    fun selectSession(sessionId: Long?) {
        selectedSessionId.value = sessionId
    }

    fun selectPeriod(period: String) {
        selectedPeriod.value = period
    }

    fun retry() = Unit
}

class PlaybookViewModel(
    private val noteRepository: NoteRepository,
    private val settingsRepository: SettingsRepository,
    sessionRepository: SessionRepository,
    episodeRepository: EpisodeRepository
) : ViewModel() {
    private val allNotes = noteRepository.observeNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val sessions = sessionRepository.observeSessions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val allEpisodes = episodeRepository.observeAllEpisodes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedSessionId = MutableStateFlow<Long?>(null)
    val selectedEpisodeId = MutableStateFlow<Long?>(null)
    val searchQuery = MutableStateFlow("")
    val notes = combine(allNotes, searchQuery) { notes, query ->
        if (query.isBlank()) notes else notes.filter {
            it.title.contains(query, ignoreCase = true) || it.body.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val availableEpisodes = combine(allEpisodes, selectedSessionId) { episodes, sessionId ->
        if (sessionId == null) episodes else episodes.filter { it.sessionId == sessionId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
        noteRepository.save(
            NoteEntity(
                title = title.value,
                body = body.value,
                sessionId = selectedSessionId.value,
                episodeId = selectedEpisodeId.value
            )
        )
        title.value = ""
        body.value = ""
        selectedSessionId.value = null
        selectedEpisodeId.value = null
        settingsRepository.clearPlaybookDraft()
    }

    fun selectSession(sessionId: Long?) {
        selectedSessionId.value = sessionId
        if (selectedEpisodeId.value != null) {
            val episodeBelongsToSession = allEpisodes.value.any { it.id == selectedEpisodeId.value && (sessionId == null || it.sessionId == sessionId) }
            if (!episodeBelongsToSession) selectedEpisodeId.value = null
        }
    }

    fun selectEpisode(episodeId: Long?) {
        selectedEpisodeId.value = episodeId
        if (episodeId != null) {
            allEpisodes.value.firstOrNull { it.id == episodeId }?.let { episode ->
                selectedSessionId.value = episode.sessionId
            }
        }
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
    }

    fun clearLocalData(onDone: () -> Unit = {}) = viewModelScope.launch {
        settingsRepository.clearLocalData()
        onDone()
    }
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
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) -> AnalyticsViewModel(episodeRepository, sessionRepository)
            modelClass.isAssignableFrom(PlaybookViewModel::class.java) -> PlaybookViewModel(noteRepository, settingsRepository, sessionRepository, episodeRepository)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(settingsRepository)
            else -> error("Unknown model class: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return vm as T
    }
}
