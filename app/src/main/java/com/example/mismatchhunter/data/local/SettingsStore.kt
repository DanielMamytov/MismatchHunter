package com.example.mismatchhunter.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val onboardingCompleted: Boolean = false,
    val darkTheme: Boolean = true,
    val accentColor: String = "blue"
)

class SettingsStore(private val context: Context) {
    private object Keys {
        val onboarding = booleanPreferencesKey("onboarding_completed")
        val darkTheme = booleanPreferencesKey("dark_theme")
        val accent = stringPreferencesKey("accent")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs: Preferences ->
        AppSettings(
            onboardingCompleted = prefs[Keys.onboarding] ?: false,
            darkTheme = prefs[Keys.darkTheme] ?: true,
            accentColor = prefs[Keys.accent] ?: "blue"
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

    suspend fun resetSettings() {
        context.dataStore.edit { it.clear() }
    }
}
