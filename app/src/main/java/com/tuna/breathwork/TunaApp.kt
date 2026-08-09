package com.tuna.breathwork

import android.app.Application
import android.content.Context
import com.tuna.breathwork.data.SessionLogStore
import com.tuna.breathwork.data.Settings
import com.tuna.breathwork.data.SettingsStore
import kotlinx.coroutines.runBlocking

/**
 * Tiny dependency container. Settings are cached at first use (DataStore prefs read —
 * a few ms); everything else is created on demand.
 */
class AppContainer(context: Context) {
    val settingsStore = SettingsStore(context)
    val logStore = SessionLogStore(context)
    val settings: Settings by lazy { runBlocking { settingsStore.current() } }
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
