package com.devstyle.premiumdialer

import android.Manifest
import android.app.role.RoleManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val Lime = Color(0xFF7BCB35)
private val DeepGreen = Color(0xFF438A16)
private val Ink = Color(0xFF172018)
private val Muted = Color(0xFF7A8578)
private val Canvas = Color(0xFFF7FAF4)
private val Glass = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {
    private fun has(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = DeepGreen, onPrimary = Color.White,
                    secondary = Lime, background = Canvas, surface = Glass,
                    onBackground = Ink, onSurface = Ink
                )
            ) {
                DialerApp(
                    hasContacts = { has(Manifest.permission.READ_CONTACTS) },
                    hasCallLog = { has(Manifest.permission.READ_CALL_LOG) },
                    hasCall = { has(Manifest.permission.CALL_PHONE) },
                    requestPermissions = { perms, callback ->
                        permissionLauncher.launch(perms)
                        pendingPermissionCallback = callback
                    },
                    call = { number -> placeCall(number) },
                    openContactEditor = { number -> openContactEditor(number) },
                    loadContacts = { readContacts() },
                    loadCalls = { readCalls() },
                    requestDefault = { requestDefaultDialer() },
                    openSettings = { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
                )
            }
        }
    }

    private var pendingPermissionCallback: (() -> Unit)? = null
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { pendingPermissionCallback?.invoke(); pendingPermissionCallback = null }

    private fun placeCall(number: String) {
        if (number.isBlank()) {
            Toast.makeText(this, "Enter a phone number first", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse("tel:${Uri.encode(number)}")
        if (has(Manifest.permission.CALL_PHONE)) {
            try {
                startActivity(Intent(Intent.ACTION_CALL, uri))
            } catch (_: Exception) {
                startActivity(Intent(Intent.ACTION_DIAL, uri))
            }
        } else {
            startActivity(Intent(Intent.ACTION_DIAL, uri))
        }
    }

    private fun openContactEditor(number: String) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.PHONE, number)
        }
        startActivity(intent)
    }

    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
                startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            } else {
                Toast.makeText(this, "Already default, or role is unavailable on this device", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Choose this app in Settings > Default apps > Phone app", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        }
    }

    private fun readContacts(): List<Person> {
        if (!has(Manifest.permission.READ_CONTACTS)) return emptyList()
        val result = mutableListOf<Person>()
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            ), null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
        )
        cursor?.use {
            val ni = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val pi = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val ci = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            while (it.moveToNext()) {
                result.add(Person(it.getString(ni) ?: "Unknown", it.getString(pi) ?: "", it.getLong(ci)))
            }
        }
        return result.distinctBy { it.id to it.number }
    }

    private fun readCalls(): List<RecentCall> {
        if (!has(Manifest.permission.READ_CALL_LOG)) return emptyList()
        val result = mutableListOf<RecentCall>()
        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE,
                CallLog.Calls.DATE, CallLog.Calls.DURATION),
            null, null, "${CallLog.Calls.DATE} DESC"
        )
        cursor?.use {
            val name = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val number = it.getColumnIndex(CallLog.Calls.NUMBER)
            val type = it.getColumnIndex(CallLog.Calls.TYPE)
            val date = it.getColumnIndex(CallLog.Calls.DATE)
            val duration = it.getColumnIndex(CallLog.Calls.DURATION)
            while (it.moveToNext() && result.size < 150) {
                result.add(RecentCall(
                    it.getString(name).orEmpty().ifBlank { it.getString(number).orEmpty() },
                    it.getString(number).orEmpty(),
                    it.getInt(type), it.getLong(date), it.getLong(duration)
                ))
            }
        }
        return result
    }
}

data class Person(val name: String, val number: String, val id: Long)
data class RecentCall(val name: String, val number: String, val type: Int, val date: Long, val duration: Long)

@Composable
private fun DialerApp(
    hasContacts: () -> Boolean,
    hasCallLog: () -> Boolean,
    hasCall: () -> Boolean,
    requestPermissions: (Array<String>, () -> Unit) -> Unit,
    call: (String) -> Unit,
    openContactEditor: (String) -> Unit,
    loadContacts: () -> List<Person>,
    loadCalls: () -> List<RecentCall>,
    requestDefault: () -> Unit,
    openSettings: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var number by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf(emptyList<Person>()) }
    var calls by remember { mutableStateOf(emptyList<RecentCall>()) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(selectedTab, refresh) {
        if (selectedTab == 1) {
            if (!hasContacts()) requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)) {}
            contacts = loadContacts()
        }
        if (selectedTab == 2) {
            if (!hasCallLog()) requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG)) {}
            calls = loadCalls()
        }
    }

    Scaffold(
        containerColor = Canvas,
        bottomBar = {
            NavigationBar(containerColor = Color.White.copy(alpha = .94f), tonalElevation = 0.dp) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.Dialpad, null) }, label = { Text("Keypad") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Contacts, null) }, label = { Text("Contacts") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Default.History, null) }, label = { Text("Recents") })
                NavigationBarItem(selected = selectedTab == 3, onClick = { selectedTab = 3 }, icon = { Icon(Icons.Default.Settings, null) }, label = { Text("Settings") })
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(Color(0xFFFDFEFC), Canvas, Color(0xFFF0F8E9)))
                ).padding(horizontal = 18.dp)
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(Lime.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PhoneInTalk, null, tint = DeepGreen, modifier = Modifier.size(25.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("PREMIUM DIALER", fontSize = 11.sp, color = DeepGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                    Text(listOf("Dial pad", "Your contacts", "Recent calls", "Preferences")[selectedTab], fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
                Box(Modifier.size(42.dp).clip(CircleShape).background(Color.White).border(1.dp, Lime.copy(alpha = .25f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MoreHoriz, null, tint = DeepGreen)
                }
            }
            when (selectedTab) {
                0 -> Keypad(number, { number = it }, { call(number) }, { openContactEditor(number) }, { if (!hasCall()) requestPermissions(arrayOf(Manifest.permission.CALL_PHONE)) { call(number) } else call(number) })
                1 -> {
                    SearchField(search, { search = it }, "Search contacts")
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${contacts.size} phone entries", color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        TextButton(onClick = { requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)) { refresh++ } }) { Text("Refresh") }
                    }
                    if (contacts.isEmpty()) EmptyState("No contacts shown", "Grant Contacts permission to load your phonebook.")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(contacts.filter { it.name.contains(search, true) || it.number.contains(search) }) { person ->
                            PersonRow(person) { number = person.number; selectedTab = 0 }
                        }
                    }
                    Button(onClick = { openContactEditor("") }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)) {
                        Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("Create contact")
                    }
                }
                2 -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Latest calls", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG)) { refresh++ } }) { Text("Refresh") }
                    }
                    if (calls.isEmpty()) EmptyState("No call history shown", "Grant Call Log permission to read recent calls.")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(calls) { item ->
                            RecentRow(item) { number = item.number; selectedTab = 0 }
                        }
                    }
                }
                else -> {
                    GlassCard {
                        Icon(Icons.Default.PhoneAndroid, null, tint = DeepGreen, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Default phone app", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Request Android's default dialer role. Some devices require extra in-call and notification handling before replacing the system phone app.", color = Muted, fontSize = 13.sp)
                        Button(onClick = requestDefault, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)) { Text("Set as default dialer") }
                    }
                    Spacer(Modifier.height(12.dp))
                    GlassCard {
                        Text("Permissions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Manage permissions if contacts, call history or calling aren't available.", color = Muted, fontSize = 13.sp)
                        OutlinedButton(onClick = openSettings, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Security, null); Spacer(Modifier.width(8.dp)); Text("Open app settings") }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Premium Glass Dialer · 1.0.0", color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun Keypad(number: String, onNumber: (String) -> Unit, onCall: () -> Unit, onSave: () -> Unit, onCallPermission: () -> Unit) {
    GlassCard {
        Text("PHONE NUMBER", color = Muted, fontSize = 10.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = number, onValueChange = onNumber,
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 28.sp, color = Ink, fontWeight = FontWeight.Medium),
                decorationBox = { inner -> if (number.isEmpty()) Text("Enter number", fontSize = 25.sp, color = Color(0xFFB7C0B3)); inner() }
            )
            if (number.isNotEmpty()) IconButton(onClick = { onNumber(number.dropLast(1)) }) { Icon(Icons.Default.Backspace, null, tint = Muted) }
        }
        HorizontalDivider(color = Lime.copy(alpha = .3f))
        Spacer(Modifier.height(12.dp))
        val keys = listOf(
            listOf("1" to "", "2" to "ABC", "3" to "DEF"),
            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
            listOf("*" to "", "0" to "+", "#" to "")
        )
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (digit, letters) ->
                    Box(
                        Modifier.weight(1f).padding(vertical = 4.dp).height(62.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White.copy(alpha = .82f))
                            .border(1.dp, Lime.copy(alpha = .18f), RoundedCornerShape(18.dp))
                            .clickable { onNumber(number + digit) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(digit, fontSize = 25.sp, fontWeight = FontWeight.Medium, color = Ink)
                            if (letters.isNotEmpty()) Text(letters, fontSize = 8.sp, color = Muted, letterSpacing = 1.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(17.dp)) {
                Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(5.dp)); Text("Save")
            }
            Button(
                onClick = onCallPermission,
                modifier = Modifier.weight(1.4f).height(58.dp),
                shape = RoundedCornerShape(19.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepGreen)
            ) {
                Icon(Icons.Default.Call, null); Spacer(Modifier.width(8.dp)); Text("Call", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GlassCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Color.White.copy(alpha = .82f))
            .border(1.dp, Color.White, RoundedCornerShape(26.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun SearchField(value: String, onValue: (String) -> Unit, hint: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Color.White)
            .border(1.dp, Lime.copy(alpha = .22f), RoundedCornerShape(17.dp)).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, null, tint = Muted)
        Spacer(Modifier.width(9.dp))
        BasicTextField(value, onValue, modifier = Modifier.weight(1f), singleLine = true,
            decorationBox = { inner -> if (value.isEmpty()) Text(hint, color = Muted); inner() })
    }
}

@Composable
private fun PersonRow(person: Person, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .9f))
            .clickable(onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(43.dp).clip(CircleShape).background(Lime.copy(alpha = .2f)), contentAlignment = Alignment.Center) {
            Text(person.name.take(1).uppercase(), color = DeepGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(person.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(person.number, color = Muted, fontSize = 13.sp)
        }
        Icon(Icons.Default.Call, null, tint = DeepGreen)
    }
}

@Composable
private fun RecentRow(item: RecentCall, onClick: () -> Unit) {
    val icon = when (item.type) {
        CallLog.Calls.INCOMING_TYPE -> Icons.Default.CallReceived
        CallLog.Calls.MISSED_TYPE -> Icons.Default.CallMissed
        CallLog.Calls.OUTGOING_TYPE -> Icons.Default.CallMade
        else -> Icons.Default.Phone
    }
    val kind = when (item.type) {
        CallLog.Calls.INCOMING_TYPE -> "Incoming"
        CallLog.Calls.MISSED_TYPE -> "Missed"
        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
        CallLog.Calls.REJECTED_TYPE -> "Rejected"
        else -> "Call"
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = .9f))
            .clickable(onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(if (item.type == CallLog.Calls.MISSED_TYPE) Color(0xFFFFE8E5) else Lime.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (item.type == CallLog.Calls.MISSED_TYPE) Color(0xFFC33D30) else DeepGreen)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name.ifBlank { item.number }, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$kind · ${android.text.format.DateFormat.format("MMM d, h:mm a", item.date)} · ${item.duration}s", color = Muted, fontSize = 11.sp)
        }
        Icon(Icons.Default.Call, null, tint = DeepGreen)
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Info, null, tint = Lime, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(6.dp))
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(message, color = Muted, fontSize = 12.sp)
    }
}
