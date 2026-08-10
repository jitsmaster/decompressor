package com.tuna.breathwork.session

import com.tuna.breathwork.domain.Phase
import com.tuna.breathwork.domain.SessionResult

enum class HeadphoneStatus { CHECKING, STEREO, MONO_FALLBACK }

data class SessionUiState(
    val phase: Phase? = null,
    val cycle: Int = 0,
    val totalCycles: Int = 0,
    val completed: SessionResult? = null,
    val aborted: Boolean = false,
    val headphoneStatus: HeadphoneStatus = HeadphoneStatus.CHECKING,
    val sessionStarted: Boolean = false,
    /** Wall-clock time the current phase started — drives the countdown ring/seconds. */
    val phaseStartedAtMs: Long = 0,
)
