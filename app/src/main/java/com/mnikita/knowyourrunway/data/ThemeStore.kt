package com.mnikita.knowyourrunway.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "nyr_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AccentPreset { COFFEE, RED, BLUE, PURPLE, GREEN, PINK }

object ThemeStore {
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_ACCENT = stringPreferencesKey("accent_preset")

    fun themeModeFlow(context: Context): Flow<ThemeMode> =
        context.settingsDataStore.data.map { prefs ->
            val raw = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
            runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.SYSTEM)
        }

    fun accentFlow(context: Context): Flow<AccentPreset> =
        context.settingsDataStore.data.map { prefs ->
            val raw = prefs[KEY_ACCENT] ?: AccentPreset.COFFEE.name
            runCatching { AccentPreset.valueOf(raw) }.getOrDefault(AccentPreset.COFFEE)
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.settingsDataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setAccent(context: Context, accent: AccentPreset) {
        context.settingsDataStore.edit { it[KEY_ACCENT] = accent.name }
    }
}