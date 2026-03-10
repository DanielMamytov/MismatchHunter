package com.example.mismatchhunter.data.repository

import com.example.mismatchhunter.data.local.AppSettings
import com.example.mismatchhunter.data.local.EpisodeDao
import com.example.mismatchhunter.data.local.EpisodeDraft
import com.example.mismatchhunter.data.local.EpisodeEntity
import com.example.mismatchhunter.data.local.NoteDao
import com.example.mismatchhunter.data.local.NoteEntity
import com.example.mismatchhunter.data.local.PlaybookDraft
import com.example.mismatchhunter.data.local.SessionDraft
import com.example.mismatchhunter.data.local.SessionDao
import com.example.mismatchhunter.data.local.SessionEntity
import com.example.mismatchhunter.data.local.SettingsStore
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val dao: SessionDao) {
    fun observeSessions(): Flow<List<SessionEntity>> = dao.observeSessions()
    fun observeSession(sessionId: Long): Flow<SessionEntity?> = dao.observeSession(sessionId)
    suspend fun createSession(session: SessionEntity): Long = dao.insertSession(session)
}

class EpisodeRepository(private val dao: EpisodeDao) {
    fun observeEpisodes(sessionId: Long): Flow<List<EpisodeEntity>> = dao.observeEpisodes(sessionId)
    fun observeAllEpisodes(): Flow<List<EpisodeEntity>> = dao.observeAllEpisodes()
    fun observeEpisode(episodeId: Long): Flow<EpisodeEntity?> = dao.observeEpisode(episodeId)
    suspend fun createEpisode(episode: EpisodeEntity): Long = dao.insertEpisode(episode)
    suspend fun updateEpisode(episode: EpisodeEntity) = dao.updateEpisode(episode)
}

class NoteRepository(private val dao: NoteDao) {
    fun observeNotes(): Flow<List<NoteEntity>> = dao.observeNotes()
    suspend fun save(note: NoteEntity) {
        if (note.id == 0L) dao.insertNote(note) else dao.updateNote(note)
    }

    suspend fun saveEpisodeNote(episodeId: Long, title: String, body: String, sessionId: Long) {
        val existing = dao.findByEpisodeId(episodeId)
        if (existing == null) {
            dao.insertNote(
                NoteEntity(
                    title = title,
                    body = body,
                    sessionId = sessionId,
                    episodeId = episodeId
                )
            )
            return
        }
        dao.updateNote(existing.copy(title = title, body = body, sessionId = sessionId))
    }

    suspend fun delete(note: NoteEntity) = dao.deleteNote(note)
}

class SettingsRepository(
    private val store: SettingsStore,
    private val sessionDao: SessionDao,
    private val episodeDao: EpisodeDao,
    private val noteDao: NoteDao
) {    val settings: Flow<AppSettings> = store.settings
    suspend fun completeOnboarding() = store.completeOnboarding()
    suspend fun setDarkTheme(enabled: Boolean) = store.setDarkTheme(enabled)
    suspend fun setAccent(accent: String) = store.setAccentColor(accent)
    suspend fun setSeasonalNotifications(enabled: Boolean) = store.setSeasonalNotifications(enabled)
    suspend fun reset() = store.resetSettings()

    suspend fun getSessionDraft(): SessionDraft = store.getSessionDraft()
    suspend fun saveSessionDraft(draft: SessionDraft) = store.saveSessionDraft(draft)
    suspend fun clearSessionDraft() = store.clearSessionDraft()

    suspend fun getEpisodeDraft(): EpisodeDraft = store.getEpisodeDraft()
    suspend fun saveEpisodeDraft(draft: EpisodeDraft) = store.saveEpisodeDraft(draft)
    suspend fun clearEpisodeDraft() = store.clearEpisodeDraft()

    suspend fun getPlaybookDraft(): PlaybookDraft = store.getPlaybookDraft()
    suspend fun savePlaybookDraft(draft: PlaybookDraft) = store.savePlaybookDraft(draft)
    suspend fun clearPlaybookDraft() = store.clearPlaybookDraft()

    suspend fun clearLocalData() {
        episodeDao.clearAll()
        noteDao.clearAll()
        sessionDao.clearAll()
    }
}


