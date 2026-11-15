package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "admin_session_prefs")

data class LockoutState(
    val failedAttempts: Int = 0,
    val lockedUntilMillis: Long = 0L
)

data class AdminSessionState(
    val loggedInAdminId: Long? = null,
    val loggedInUsername: String? = null,
    val loggedInRole: String? = null,
    val sessionStartedAtMillis: Long = 0L,
    val keepSignedIn: Boolean = true
) {
    val isAuthenticated: Boolean get() = loggedInAdminId != null
}

class SessionPreferences(private val context: Context) {
    companion object {
        private val KEY_LOGGED_IN_ADMIN_ID = longPreferencesKey("logged_in_admin_id")
        private val KEY_LOGGED_IN_USERNAME = stringPreferencesKey("logged_in_username")
        private val KEY_LOGGED_IN_ROLE = stringPreferencesKey("logged_in_role")
        private val KEY_SESSION_STARTED_AT = longPreferencesKey("session_started_at_ms")
        private val KEY_KEEP_SIGNED_IN = booleanPreferencesKey("keep_signed_in")
        private val KEY_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        private val KEY_LOCKED_UNTIL = longPreferencesKey("pin_locked_until_ms")
    }

    val sessionState: Flow<AdminSessionState> = context.sessionDataStore.data.map { prefs ->
        val id = prefs[KEY_LOGGED_IN_ADMIN_ID] ?: -1L
        AdminSessionState(
            loggedInAdminId = if (id > 0L) id else null,
            loggedInUsername = prefs[KEY_LOGGED_IN_USERNAME],
            loggedInRole = prefs[KEY_LOGGED_IN_ROLE],
            sessionStartedAtMillis = prefs[KEY_SESSION_STARTED_AT] ?: 0L,
            keepSignedIn = prefs[KEY_KEEP_SIGNED_IN] ?: true
        )
    }

    val loggedInAdminId: Flow<Long?> = sessionState.map { it.loggedInAdminId }

    val lockoutState: Flow<LockoutState> = context.sessionDataStore.data.map { prefs ->
        LockoutState(
            failedAttempts = prefs[KEY_FAILED_ATTEMPTS] ?: 0,
            lockedUntilMillis = prefs[KEY_LOCKED_UNTIL] ?: 0L
        )
    }

    suspend fun getLoggedInAdminIdOnce(): Long? = loggedInAdminId.first()

    suspend fun getSessionSnapshot(): AdminSessionState = sessionState.first()

    suspend fun getLockoutSnapshot(): LockoutState = lockoutState.first()

    suspend fun setLoggedInAdminId(adminId: Long?) {
        context.sessionDataStore.edit { prefs ->
            if (adminId == null) {
                prefs.remove(KEY_LOGGED_IN_ADMIN_ID)
                prefs.remove(KEY_LOGGED_IN_USERNAME)
                prefs.remove(KEY_LOGGED_IN_ROLE)
                prefs.remove(KEY_SESSION_STARTED_AT)
                prefs.remove(KEY_KEEP_SIGNED_IN)
            } else {
                prefs[KEY_LOGGED_IN_ADMIN_ID] = adminId
            }
        }
    }

    suspend fun saveSession(
        adminId: Long,
        username: String,
        role: String,
        keepSignedIn: Boolean,
        startedAtMillis: Long = System.currentTimeMillis()
    ) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_LOGGED_IN_ADMIN_ID] = adminId
            prefs[KEY_LOGGED_IN_USERNAME] = username
            prefs[KEY_LOGGED_IN_ROLE] = role
            prefs[KEY_SESSION_STARTED_AT] = startedAtMillis
            prefs[KEY_KEEP_SIGNED_IN] = keepSignedIn
        }
    }

    suspend fun clearSession() {
        setLoggedInAdminId(null)
    }

    suspend fun recordFailedAttempt(attempts: Int, lockedUntilMillis: Long) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_FAILED_ATTEMPTS] = attempts
            prefs[KEY_LOCKED_UNTIL] = lockedUntilMillis
        }
    }

    suspend fun resetLockout() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(KEY_FAILED_ATTEMPTS)
            prefs.remove(KEY_LOCKED_UNTIL)
        }
    }
}
