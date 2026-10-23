package com.devstyle.premiumdialer

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

/** Receives calls from Telecom when the user explicitly makes this the default dialer. */
class PremiumInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallSession.attach(call)
        try {
            startActivity(Intent(this, InCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
        } catch (_: Exception) {
            // Telecom still owns the call; the user can return to the app to access controls.
        }
    }

    override fun onCallRemoved(call: Call) {
        CallSession.detach(call)
        super.onCallRemoved(call)
    }
}

/** Shared observable state between the Telecom service and the call UI. */
object CallSession {
    var call: Call? = null
        private set
    val state = mutableIntStateOf(Call.STATE_DISCONNECTED)
    val displayNumber = mutableStateOf("")

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            this@CallSession.state.intValue = state
            displayNumber.value = call.details.handle?.schemeSpecificPart.orEmpty()
        }
    }

    fun attach(newCall: Call) {
        call?.unregisterCallback(callback)
        call = newCall
        state.intValue = newCall.state
        displayNumber.value = newCall.details.handle?.schemeSpecificPart.orEmpty()
        newCall.registerCallback(callback)
    }

    fun detach(removedCall: Call) {
        removedCall.unregisterCallback(callback)
        if (call == removedCall) {
            call = null
            state.intValue = Call.STATE_DISCONNECTED
            displayNumber.value = ""
        }
    }

    fun answer() { call?.answer(VideoProfile.STATE_AUDIO_ONLY) }
    fun hangUp() { call?.disconnect() }
    fun toggleHold() {
        val active = call ?: return
        if (active.state == Call.STATE_HOLDING) active.unhold() else active.hold()
    }
}
