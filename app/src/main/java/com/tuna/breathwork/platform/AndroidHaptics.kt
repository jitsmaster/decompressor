package com.tuna.breathwork.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.tuna.breathwork.domain.HapticKind
import com.tuna.breathwork.session.HapticDriver

/**
 * Haptic language for eyes-closed practice (SPEC extension):
 *  - PULSE   = two quick taps → "breathe in"
 *  - SOFT    = gentle continuous buzz for the phase duration → "breathe out"
 *  - PRETICK = one soft tick ~1 s before a phase ends → "get ready to switch"
 *  - NONE    = silence (holds)
 * Calm Now passes hapticsEnabled=false to suppress entirely (toggle in Settings).
 */
class AndroidHaptics(context: Context, private val enabled: Boolean) : HapticDriver {

    private val vibrator: Vibrator? = if (enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                ?.let { manager -> (manager as VibratorManager).defaultVibrator }
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        }
    } else null

    override fun start(kind: HapticKind, durationMs: Long) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        when (kind) {
            HapticKind.NONE -> v.cancel()
            HapticKind.PULSE -> v.vibrate(doubleTap())
            HapticKind.SOFT -> v.vibrate(continuousSoft(durationMs))
            HapticKind.PRETICK -> v.vibrate(softTick())
        }
    }

    override fun stop() {
        vibrator?.cancel()
    }

    private fun doubleTap(): VibrationEffect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(longArrayOf(0, 90, 140, 90), -1)
        } else {
            @Suppress("DEPRECATION")
            VibrationEffect.createOneShot(90, VibrationEffect.DEFAULT_AMPLITUDE)
        }

    /** Continuous but gentle (~45% amplitude) buzz for the exhale length. */
    private fun continuousSoft(durationMs: Long): VibrationEffect {
        val dur = durationMs.coerceIn(500, 10_000)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(dur, 110)
        } else {
            @Suppress("DEPRECATION")
            VibrationEffect.createOneShot(dur, 110)
        }
    }

    private fun softTick(): VibrationEffect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(60, 90)
        } else {
            @Suppress("DEPRECATION")
            VibrationEffect.createOneShot(60, 90)
        }
}
