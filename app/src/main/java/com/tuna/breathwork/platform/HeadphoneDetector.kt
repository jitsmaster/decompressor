package com.tuna.breathwork.platform

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

/**
 * True stereo binaural beats only entrain when the two carriers reach separate ears.
 * Reports whether a headphone-class sink is present: wired headset/headphones, USB
 * audio, or a connected Bluetooth A2DP/SCO device. Purely synchronous, no permissions.
 */
class HeadphoneDetector(private val context: Context) {

    fun hasHeadphones(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.isWiredHeadsetOn) return true
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
        return devices.any { it.isSink && it.type in SINK_TYPES }
    }

    companion object {
        private val SINK_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        )
    }
}
