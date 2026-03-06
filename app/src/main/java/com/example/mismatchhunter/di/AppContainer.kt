package com.example.mismatchhunter.di

import android.content.Context
import com.example.mismatchhunter.data.local.AppDatabase
import com.example.mismatchhunter.data.local.SettingsStore
import com.example.mismatchhunter.data.repository.EpisodeRepository
import com.example.mismatchhunter.data.repository.NoteRepository
import com.example.mismatchhunter.data.repository.SessionRepository
import com.example.mismatchhunter.data.repository.SettingsRepository

class AppContainer(context: Context) {
    private val db = AppDatabase.build(context)
    private val settingsStore = SettingsStore(context)

    val sessionRepository = SessionRepository(db.sessionDao())
    val episodeRepository = EpisodeRepository(db.episodeDao())
    val noteRepository = NoteRepository(db.noteDao())
    val settingsRepository = SettingsRepository(settingsStore)
}
