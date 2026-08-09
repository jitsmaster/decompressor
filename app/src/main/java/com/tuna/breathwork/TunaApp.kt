package com.tuna.breathwork

import android.app.Application
import android.content.Context
import com.tuna.breathwork.data.SessionLogStore
import com.tuna.breathwork.data.Settings
import com.tuna.breathwork.data.SettingsStore

/**
 * Tiny dependency container. Settings are read fresh on demand (DataStore prefs read —
 * a few ms) so the Calm Now technique switch and voice params never go stale.
 */
class AppContainer(context: Context) {
    val settingsStore = SettingsStore(context)
    val logStore = SessionLogStore(context)

    suspend fun currentSettings(): Settings = settingsStore.current()
}

class TunaApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

val Application.container: AppContainer
    get() = (this as TunaApp).container
