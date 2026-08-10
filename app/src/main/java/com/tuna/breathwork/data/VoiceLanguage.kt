package com.tuna.breathwork.data

/** Session guidance language. Clips are bundled for both; the active one is chosen in Settings. */
enum class VoiceLanguage(val key: String, val label: String) {
    EN("en", "English"),
    ZH("zh", "中文");

    companion object {
        fun fromKey(key: String?): VoiceLanguage = entries.firstOrNull { it.key == key } ?: EN
    }
}
