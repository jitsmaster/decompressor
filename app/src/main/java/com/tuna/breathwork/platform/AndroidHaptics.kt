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
            HapticKind.PULSE -> v.vibrate(inBuzz())      // high-frequency: rapid pulsing
            HapticKind.SOFT -> v.vibrate(outRumble())    // low-frequency: slow, deep pulsing
            HapticKind.PRETICK -> v.vibrate(softTick())
        }
    }

    override fun stop() {
        vibrator?.cancel()
    }

    /** Rapid 40 ms on / 40 ms off (~12.5 Hz) — a high, light buzz for the inhale. */
    private fun inBuzz(): VibrationEffect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(longArrayOf(0, 40, 40), intArrayOf(0, 65, 0), 1)
        } else {
            @Suppress("DEPRECATION")
            VibrationEffect.createOneShot(40, 65)
        }

    /** Slow 180 ms on / 420 ms off (~1.7 Hz) — a low, gentle rumble for the exhale. */
    private fun outRumble(): VibrationEffect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createWaveform(longArrayOf(0, 180, 420), intArrayOf(0, 45, 0), 1)
        } else {
            @Suppress("DEPRECATION")
            VibrationEffect.createOneShot(180, 45)
        }
    }

    private fun softTick(): VibrationEffect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect.createOneShot(60, 50)
        } else {
            @Suppress("DEPRECATION")
            VibrationEffect.createOneShot(60, 50)
        }
}
