package com.akinalpfdn.typewall.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Define the DataStore
val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

// Theme options enum
enum class AppTheme(val value: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromValue(value: String): AppTheme {
            return values().find { it.value == value } ?: SYSTEM
        }
    }
}

// Theme preferences class
class ThemePreferences(private val context: Context) {
    private val dataStore = context.themeDataStore

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val USE_DYNAMIC_COLOR_KEY = booleanPreferencesKey("use_dynamic_color")
    }

    // Flow to observe theme changes
    val themeMode: Flow<AppTheme> = dataStore.data.map { preferences ->
        preferences[THEME_MODE_KEY]?.let { AppTheme.fromValue(it) } ?: AppTheme.SYSTEM
    }

    val useDynamicColor: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[USE_DYNAMIC_COLOR_KEY] ?: true
    }

    // Save theme preference
    suspend fun setThemeMode(themeMode: AppTheme) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.value
        }
    }

    // Save dynamic color preference
    suspend fun setUseDynamicColor(useDynamic: Boolean) {
        dataStore.edit { preferences ->
            preferences[USE_DYNAMIC_COLOR_KEY] = useDynamic
        }
    }
}