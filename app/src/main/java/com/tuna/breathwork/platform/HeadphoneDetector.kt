package com.tuna.breathwork.platform

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

/**
 * True stereo binaural beats only entrain when the two carriers reach separate ears.
 * Reports whether the user is (likely) wearing headphones: wired headset, USB audio,
 * or an active Bluetooth A2DP sink.
 */
class HeadphoneDetector(private val context: Context) {

    fun hasHeadphones(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Wired headset / USB audio device
        if (audioManager.isWiredHeadsetOn) return true
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL)
        if (devices.any { it.isSink && it.type in wiredSinkTypes }) return true

        // Bluetooth A2DP sink currently connected (requires BLUETOOTH_CONNECT; degrades
        // to wired-only detection when not granted)
        val btAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        if (btAdapter != null && btAdapter.isEnabled) {
            val connected = java.util.concurrent.atomic.AtomicBoolean(false)
            var proxyRef: BluetoothProfile? = null
            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    proxyRef = proxy
                    if (profile == BluetoothProfile.A2DP) {
                        val a2dp = proxy as BluetoothA2dp
                        connected.set(a2dp.connectedDevices.any {
                            a2dp.getConnectionState(it) == BluetoothProfile.STATE_CONNECTED
                        })
                    }
                }
                override fun onServiceDisconnected(profile: Int) {}
            }
            runCatching {
                if (btAdapter.getProfileProxy(context, listener, BluetoothProfile.A2DP)) {
                    repeat(20) {
                        if (connected.get()) {
                            proxyRef?.let { btAdapter.closeProfileProxy(BluetoothProfile.A2DP, it) }
                            return true
                        }
                        Thread.sleep(25)
                    }
                    proxyRef?.let { btAdapter.closeProfileProxy(BluetoothProfile.A2DP, it) }
                }
            }
        }
        return false
    }

    private val wiredSinkTypes = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    )
}
