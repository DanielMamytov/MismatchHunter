package com.example.mismatchhunter.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val darkTheme: Boolean = true,
    val accentColor: String = "blue",
    val seasonalNotifications: Boolean = false
)

data class SessionDraft(
    val title: String = "",
    val date: String = "",
    val matchType: String = "Regular",
    val description: String = ""
)

data class EpisodeDraft(
    val position: String = "",
    val switchType: String = "",
    val zone: String = "",
    val decision: String = "",
    val result: String = ""
)

data class PlaybookDraft(
    val title: String = "",
    val body: String = ""
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_completed")
        val darkTheme = booleanPreferencesKey("dark_theme")
        val accent = stringPreferencesKey("accent")
        val seasonalNotifications = booleanPreferencesKey("seasonal_notifications")


        val sessionDraftTitle = stringPreferencesKey("session_draft_title")
        val sessionDraftMatchType = stringPreferencesKey("session_draft_match_type")
        val sessionDraftDate = stringPreferencesKey("session_draft_date")
        val sessionDraftDescription = stringPreferencesKey("session_draft_description")

        val episodeDraftPosition = stringPreferencesKey("episode_draft_position")
        val episodeDraftSwitchType = stringPreferencesKey("episode_draft_switch_type")
        val episodeDraftZone = stringPreferencesKey("episode_draft_zone")
        val episodeDraftDecision = stringPreferencesKey("episode_draft_decision")
        val episodeDraftResult = stringPreferencesKey("episode_draft_result")

        val playbookDraftTitle = stringPreferencesKey("playbook_draft_title")
        val playbookDraftBody = stringPreferencesKey("playbook_draft_body")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs: Preferences ->
        AppSettings(
            onboardingCompleted = prefs[Keys.onboarding] ?: false,
            darkTheme = prefs[Keys.darkTheme] ?: true,
            accentColor = prefs[Keys.accent] ?: "blue",
            seasonalNotifications = prefs[Keys.seasonalNotifications] ?: false
        )
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[Keys.onboarding] = true }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.darkTheme] = enabled }
    }

    suspend fun setAccentColor(accent: String) {
        context.dataStore.edit { it[Keys.accent] = accent }
    }
    suspend fun setSeasonalNotifications(enabled: Boolean) {
        context.dataStore.edit { it[Keys.seasonalNotifications] = enabled }
    }

    suspend fun resetSettings() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun getSessionDraft(): SessionDraft {
        val prefs = context.dataStore.data.first()
        return SessionDraft(
            title = prefs[Keys.sessionDraftTitle] ?: "",
            date = prefs[Keys.sessionDraftDate] ?: java.time.LocalDate.now().toString(),
            matchType = prefs[Keys.sessionDraftMatchType] ?: "",
            description = prefs[Keys.sessionDraftDescription] ?: ""
        )
    }

    suspend fun saveSessionDraft(draft: SessionDraft) {
        context.dataStore.edit {
            it[Keys.sessionDraftTitle] = draft.title
            it[Keys.sessionDraftDate] = draft.date
            it[Keys.sessionDraftMatchType] = draft.matchType
            it[Keys.sessionDraftDescription] = draft.description
        }
    }

    suspend fun clearSessionDraft() {
        context.dataStore.edit {
            it.remove(Keys.sessionDraftTitle)
            it.remove(Keys.sessionDraftDate)
            it.remove(Keys.sessionDraftMatchType)
            it.remove(Keys.sessionDraftDescription)
        }
    }

    suspend fun getEpisodeDraft(): EpisodeDraft {
        val prefs = context.dataStore.data.first()
        return EpisodeDraft(
            position = prefs[Keys.episodeDraftPosition] ?: "",
            switchType = prefs[Keys.episodeDraftSwitchType] ?: "",
            zone = prefs[Keys.episodeDraftZone] ?: "",
            decision = prefs[Keys.episodeDraftDecision] ?: "",
            result = prefs[Keys.episodeDraftResult] ?: ""
        )
    }

    suspend fun saveEpisodeDraft(draft: EpisodeDraft) {
        context.dataStore.edit {
            it[Keys.episodeDraftPosition] = draft.position
            it[Keys.episodeDraftSwitchType] = draft.switchType
            it[Keys.episodeDraftZone] = draft.zone
            it[Keys.episodeDraftDecision] = draft.decision
            it[Keys.episodeDraftResult] = draft.result
        }
    }

    suspend fun clearEpisodeDraft() {
        context.dataStore.edit {
            it.remove(Keys.episodeDraftPosition)
            it.remove(Keys.episodeDraftSwitchType)
            it.remove(Keys.episodeDraftZone)
            it.remove(Keys.episodeDraftDecision)
            it.remove(Keys.episodeDraftResult)
        }
    }

    suspend fun getPlaybookDraft(): PlaybookDraft {
        val prefs = context.dataStore.data.first()
        return PlaybookDraft(
            title = prefs[Keys.playbookDraftTitle] ?: "",
            body = prefs[Keys.playbookDraftBody] ?: ""
        )
    }

    suspend fun savePlaybookDraft(draft: PlaybookDraft) {
        context.dataStore.edit {
            it[Keys.playbookDraftTitle] = draft.title
            it[Keys.playbookDraftBody] = draft.body
        }
    }

    suspend fun clearPlaybookDraft() {
        context.dataStore.edit {
            it.remove(Keys.playbookDraftTitle)
            it.remove(Keys.playbookDraftBody)
        }
    }
}
