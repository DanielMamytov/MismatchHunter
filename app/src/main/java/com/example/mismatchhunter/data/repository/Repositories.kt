package com.example.mismatchhunter.data.repository

import com.example.mismatchhunter.data.local.AppSettings
import com.example.mismatchhunter.data.local.EpisodeDao
import com.example.mismatchhunter.data.local.EpisodeEntity
import com.example.mismatchhunter.data.local.NoteDao
import com.example.mismatchhunter.data.local.NoteEntity
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

    suspend fun delete(note: NoteEntity) = dao.deleteNote(note)
}

class SettingsRepository(private val store: SettingsStore) {
    val settings: Flow<AppSettings> = store.settings
    suspend fun completeOnboarding() = store.completeOnboarding()
    suspend fun setDarkTheme(enabled: Boolean) = store.setDarkTheme(enabled)
    suspend fun setAccent(accent: String) = store.setAccentColor(accent)
    suspend fun reset() = store.resetSettings()
}
