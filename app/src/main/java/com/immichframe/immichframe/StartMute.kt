package com.immichframe.immichframe

import android.content.Context
import android.media.AudioManager
import androidx.preference.PreferenceManager

/**
 * Implements the "Mute TV on Start" setting: mutes the device's media stream while the frame
 * (activity or screensaver) is showing, and unmutes it again when the frame goes away so other
 * apps are not left silent.
 *
 * The user gets sound back by pressing a volume button, which Android itself treats as an
 * unmute; nothing here needs to observe that.
 *
 * The activity and the screensaver can overlap briefly (one starting while the other stops), so
 * holders are counted: the first [acquire] mutes and only the last [release] restores. Call
 * both from the main thread.
 */
object StartMute {
    private var holders = 0
    private var mutedByUs = false

    fun acquire(context: Context) {
        holders++
        if (holders > 1 || !isEnabled(context)) return
        val audioManager = audioManager(context)
        // Leave a stream the user muted themselves alone, so release() does not unmute it.
        if (audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) return
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
        mutedByUs = true
    }

    fun release(context: Context) {
        if (holders == 0) return
        holders--
        if (holders > 0 || !mutedByUs) return
        mutedByUs = false
        audioManager(context)
            .adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
    }

    // Audio only ever plays in WebView mode with audio enabled, so muting is pointless otherwise.
    private fun isEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        return prefs.getBoolean("useWebView", true) &&
            prefs.getBoolean("playAudio", false) &&
            prefs.getBoolean("muteOnStart", false)
    }

    private fun audioManager(context: Context): AudioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
}
