package com.tuna.breathwork.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.tuna.breathwork.domain.HapticKind
import com.tuna.breathwork.session.HapticDriver

/**
 * Vibration output. PULSE = short tap at inhale start; SOFT = longer gentle pulse at
 * exhale start. Calm Now passes hapticsEnabled=false to suppress (SPEC D11).
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

    override fun fire(kind: HapticKind) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        when (kind) {
            HapticKind.PULSE -> v.vibrate(shortEffect())
            HapticKind.SOFT -> v.vibrate(longEffect())
            HapticKind.NONE -> {}
        }
    }

    private fun shortEffect(): VibrationEffect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        } else {
            VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
        }

    private fun longEffect(): VibrationEffect =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE)
        } else {
            VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE)
        }
}
