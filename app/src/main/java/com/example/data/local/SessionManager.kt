package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "zm_bazaar_session")

class SessionManager(private val context: Context) {
    private val keyUserId = longPreferencesKey("logged_in_user_id")

    val loggedInUserId: Flow<Long?> = context.dataStore.data.map { prefs ->
        val id = prefs[keyUserId] ?: -1L
        if (id > 0L) id else null
    }

    suspend fun saveUserId(userId: Long) {
        context.dataStore.edit { prefs ->
            prefs[keyUserId] = userId
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(keyUserId)
        }
    }
}
