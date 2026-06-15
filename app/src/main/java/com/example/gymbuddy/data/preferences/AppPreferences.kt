package com.example.gymbuddy.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// ── DataStore singleton ──────────────────────────────────────────────
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gymbuddy_prefs")

// ── Preference keys ──────────────────────────────────────────────────
object PrefKeys {
    val THEME_MODE        = stringPreferencesKey("theme_mode")   // "dark" | "light" | "system"
    val WEIGHT_UNIT       = stringPreferencesKey("weight_unit")  // "kg" | "lbs"
    val NOTIF_WORKOUT     = booleanPreferencesKey("notif_workout")
    val NOTIF_HOUR        = intPreferencesKey("notif_hour")
    val NOTIF_MIN         = intPreferencesKey("notif_min")
    val NOTIF_STREAK      = booleanPreferencesKey("notif_streak")
    val NOTIF_FRIENDS     = booleanPreferencesKey("notif_friends")
}

// ── Preference wrapper ───────────────────────────────────────────────
class AppPreferences(private val dataStore: DataStore<Preferences>) {

    // BroadcastReceiver'lar için kısa yol
    constructor(context: Context) : this(context.dataStore)

    val themeMode: Flow<String>   = dataStore.data.map { it[PrefKeys.THEME_MODE]    ?: "dark" }
    val weightUnit: Flow<String>  = dataStore.data.map { it[PrefKeys.WEIGHT_UNIT]   ?: "kg" }
    val notifWorkout: Flow<Boolean> = dataStore.data.map { it[PrefKeys.NOTIF_WORKOUT] ?: false }
    val notifHour: Flow<Int>      = dataStore.data.map { it[PrefKeys.NOTIF_HOUR]    ?: 8 }
    val notifMin: Flow<Int>       = dataStore.data.map { it[PrefKeys.NOTIF_MIN]     ?: 0 }
    val notifStreak: Flow<Boolean>  = dataStore.data.map { it[PrefKeys.NOTIF_STREAK]  ?: true }
    val notifFriends: Flow<Boolean> = dataStore.data.map { it[PrefKeys.NOTIF_FRIENDS] ?: true }

    suspend fun setThemeMode(mode: String)       { dataStore.edit { it[PrefKeys.THEME_MODE]    = mode } }
    suspend fun setWeightUnit(unit: String)      { dataStore.edit { it[PrefKeys.WEIGHT_UNIT]   = unit } }
    suspend fun setNotifWorkout(on: Boolean)     { dataStore.edit { it[PrefKeys.NOTIF_WORKOUT] = on  } }
    suspend fun setNotifTime(hour: Int, min: Int){ dataStore.edit { it[PrefKeys.NOTIF_HOUR] = hour; it[PrefKeys.NOTIF_MIN] = min } }
    suspend fun setNotifStreak(on: Boolean)      { dataStore.edit { it[PrefKeys.NOTIF_STREAK]  = on  } }
    suspend fun setNotifFriends(on: Boolean)     { dataStore.edit { it[PrefKeys.NOTIF_FRIENDS] = on  } }
}
