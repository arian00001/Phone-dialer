package com.devstyle.premiumdialer

import android.os.Bundle
import android.telecom.Call
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CallGreen = Color(0xFF438A16)
private val CallLime = Color(0xFF7BCB35)

class InCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showCallUi()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showCallUi()
    }

    private fun showCallUi() {
        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme(primary = CallGreen, secondary = CallLime)) {
                val callState by CallSession.state
                val number by CallSession.displayNumber
                val ringing = callState == Call.STATE_RINGING
                val active = callState == Call.STATE_ACTIVE
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF6FAF2)) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.size(100.dp).background(CallLime.copy(alpha = .2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = CallGreen, modifier = Modifier.size(46.dp)) }
                        Spacer(Modifier.height(24.dp))
                        Text(if (ringing) "Incoming call" else if (active) "Call in progress" else "Phone call", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(number.ifBlank { "Unknown number" }, fontSize = 18.sp, color = Color(0xFF6D7969))
                        Spacer(Modifier.height(36.dp))
                        if (ringing) {
                            Button(onClick = { CallSession.answer() }, colors = ButtonDefaults.buttonColors(containerColor = CallGreen), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().height(58.dp)) {
                                Icon(Icons.Default.Call, contentDescription = null); Text("  Answer", fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                        } else if (active || callState == Call.STATE_HOLDING) {
                            OutlinedButton(onClick = { CallSession.toggleHold() }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(20.dp)) {
                                Icon(Icons.Default.Pause, contentDescription = null); Text(if (callState == Call.STATE_HOLDING) "  Resume call" else "  Hold call")
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        Button(onClick = { CallSession.hangUp(); finish() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC93636)), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().height(58.dp)) {
                            Icon(Icons.Default.CallEnd, contentDescription = null); Text(if (ringing) "  Decline" else "  End call", fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("Cellular call controls", fontSize = 12.sp, color = Color(0xFF879184))
                    }
                }
            }
        }
    }
}
