package com.example.adunlock

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ad_unlock_prefs")

object RewardStore {
    private val UNLOCK_UNTIL_KEY = longPreferencesKey("unlock_until_ms")

    suspend fun setUnlockUntil(context: Context, unlockUntilMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[UNLOCK_UNTIL_KEY] = unlockUntilMs
        }
    }

    suspend fun getUnlockUntil(context: Context): Long {
        val prefs = context.dataStore.data.first()
        return prefs[UNLOCK_UNTIL_KEY] ?: 0L
    }

    suspend fun isUnlocked(context: Context): Boolean {
        val until = getUnlockUntil(context)
        return until > System.currentTimeMillis()
    }

    suspend fun remainingMs(context: Context): Long {
        val until = getUnlockUntil(context)
        val now = System.currentTimeMillis()
        return if (until > now) until - now else 0L
    }
}
