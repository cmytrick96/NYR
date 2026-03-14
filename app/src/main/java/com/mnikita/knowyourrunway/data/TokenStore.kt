package com.mnikita.knowyourrunway.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nyr_store")

data class UserProfile(
    val imageUri: String? = null,     // local persisted URI
    val avatarUrl: String? = null,    // server URL
    val name: String = "",
    val username: String = "",
    val country: String = "",
    val birthDate: String = "",       // dd-MM-yyyy (UI)
    val gender: String = "Prefer not to say",
    val tags: List<String> = emptyList()
)

class TokenStore(private val context: Context) {
    private val KEY_TOKEN = stringPreferencesKey("token")

    private val KEY_PROFILE_COMPLETED = booleanPreferencesKey("profile_completed")
    private val KEY_PROFILE_IMAGE_URI = stringPreferencesKey("profile_image_uri")
    private val KEY_PROFILE_AVATAR_URL = stringPreferencesKey("profile_avatar_url")
    private val KEY_PROFILE_NAME = stringPreferencesKey("profile_name")
    private val KEY_PROFILE_USERNAME = stringPreferencesKey("profile_username")
    private val KEY_PROFILE_COUNTRY = stringPreferencesKey("profile_country")
    private val KEY_PROFILE_BIRTHDATE = stringPreferencesKey("profile_birthdate") // dd-MM-yyyy
    private val KEY_PROFILE_GENDER = stringPreferencesKey("profile_gender")
    private val KEY_PROFILE_TAGS = stringPreferencesKey("profile_tags")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_TOKEN] }

    val profileCompletedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PROFILE_COMPLETED] ?: true
    }

    val profileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        val tagsRaw = prefs[KEY_PROFILE_TAGS].orEmpty()
        val tags = tagsRaw
            .split("||")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(5)

        UserProfile(
            imageUri = prefs[KEY_PROFILE_IMAGE_URI],
            avatarUrl = prefs[KEY_PROFILE_AVATAR_URL],
            name = prefs[KEY_PROFILE_NAME].orEmpty(),
            username = prefs[KEY_PROFILE_USERNAME].orEmpty(),
            country = prefs[KEY_PROFILE_COUNTRY].orEmpty(),
            birthDate = prefs[KEY_PROFILE_BIRTHDATE].orEmpty(),
            gender = prefs[KEY_PROFILE_GENDER] ?: "Prefer not to say",
            tags = tags
        )
    }

    suspend fun setToken(token: String) {
        context.dataStore.edit { it[KEY_TOKEN] = token }
    }

    suspend fun clearToken() {
        context.dataStore.edit { it.remove(KEY_TOKEN) }
    }

    suspend fun setProfileCompleted(completed: Boolean) {
        context.dataStore.edit { it[KEY_PROFILE_COMPLETED] = completed }
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            profile.imageUri?.let { prefs[KEY_PROFILE_IMAGE_URI] = it } ?: prefs.remove(KEY_PROFILE_IMAGE_URI)
            profile.avatarUrl?.let { prefs[KEY_PROFILE_AVATAR_URL] = it } ?: prefs.remove(KEY_PROFILE_AVATAR_URL)

            prefs[KEY_PROFILE_NAME] = profile.name
            prefs[KEY_PROFILE_USERNAME] = profile.username
            prefs[KEY_PROFILE_COUNTRY] = profile.country
            prefs[KEY_PROFILE_BIRTHDATE] = profile.birthDate
            prefs[KEY_PROFILE_GENDER] = profile.gender
            prefs[KEY_PROFILE_TAGS] = profile.tags
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .take(5)
                .joinToString("||")

            prefs[KEY_PROFILE_COMPLETED] = true
        }
    }

    suspend fun clearProfile() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_PROFILE_IMAGE_URI)
            prefs.remove(KEY_PROFILE_AVATAR_URL)
            prefs.remove(KEY_PROFILE_NAME)
            prefs.remove(KEY_PROFILE_USERNAME)
            prefs.remove(KEY_PROFILE_COUNTRY)
            prefs.remove(KEY_PROFILE_BIRTHDATE)
            prefs.remove(KEY_PROFILE_GENDER)
            prefs.remove(KEY_PROFILE_TAGS)
            prefs.remove(KEY_PROFILE_COMPLETED)
        }
    }
}