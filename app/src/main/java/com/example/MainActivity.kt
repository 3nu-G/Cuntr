package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.example.data.CounterSession
import com.example.ui.theme.CountrTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.CLOSE_DASHBOARD") {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(closeReceiver, IntentFilter("com.example.CLOSE_DASHBOARD"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(closeReceiver, IntentFilter("com.example.CLOSE_DASHBOARD"))
        }

        setContent {
            CountrTheme {
                MainScreen(
                    viewModel = viewModel,
                    onStartSession = { size, template, timer, note ->
                        startBubbleSession(size, template, timer, note)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(closeReceiver)
    }

    private fun startBubbleSession(size: String, template: String, enableTimer: Boolean, note: String) {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return
        }

        val serviceIntent = Intent(this, CounterOverlayService::class.java).apply {
            putExtra("SIZE", size)
            putExtra("TEMPLATE", template)
            putExtra("ENABLE_TIMER", enableTimer)
            putExtra("NOTE", note)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        val prefs = getSharedPreferences("countr_settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("auto_close", false)) {
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onStartSession: (String, String, Boolean, String) -> Unit) {
    val sessions by viewModel.previousSessions.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { viewModel.exportData(context, it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.importData(context, it) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF6750A4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Countr", fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = Color(0xFF1C1B1F))
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color(0xFF6750A4))
                    }
                    IconButton(onClick = { importLauncher.launch("application/json") }) {
                        Icon(Icons.Filled.Download, contentDescription = "Import", tint = Color(0xFF6750A4))
                    }
                    IconButton(onClick = { exportLauncher.launch("countr_sessions.json") }) {
                        Icon(Icons.Filled.Upload, contentDescription = "Export", tint = Color(0xFF6750A4))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFFEADDFF),
                contentColor = Color(0xFF21005D),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Filled.Add, "New Session")
            }
        }
    ) { innerPadding ->
        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No sessions yet", color = Color.Gray, fontSize = 20.sp)
                    Text("Tap + to start a floating counter", color = Color.Gray)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SESSION HISTORY", color = Color(0xFF49454F), fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.5.sp)
                    Text("${sessions.size} Total", color = Color(0xFF6750A4), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        SessionCard(
                            session = session,
                            onUpdateNote = { newNote -> viewModel.updateSession(session.copy(note = newNote)) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }

        if (showSettingsDialog) {
            SettingsDialog(onDismiss = { showSettingsDialog = false })
        }

        if (showDialog) {
            NewSessionDialog(
                onDismiss = { showDialog = false },
                onStart = { size, template, timer, note ->
                    onStartSession(size, template, timer, note)
                    showDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionCard(session: CounterSession, onUpdateNote: (String) -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd • HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(session.timestamp)).uppercase()
    var showEditNoteDialog by remember { mutableStateOf(false) }
    
    fun formatDuration(ms: Long): String {
        if (ms == 0L) return "0s"
        val s = ms / 1000
        return if (s > 60) "${s/60}m ${s%60}s" else "${s}s"
    }

    if (showEditNoteDialog) {
        var editedNote by remember { mutableStateOf(session.note) }
        AlertDialog(
            onDismissRequest = { showEditNoteDialog = false },
            title = { Text("Edit Note", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedNote,
                    onValueChange = { editedNote = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateNote(editedNote)
                    showEditNoteDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNoteDialog = false }) { Text("Cancel") }
            }
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE7E0EC)),
        onClick = { showEditNoteDialog = true }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(dateString, fontSize = 12.sp, color = Color(0xFF6750A4), fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${session.checkCount}", color = Color(0xFF2D6A4F), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Filled.Check, contentDescription = "Check", tint = Color(0xFF2D6A4F), modifier = Modifier.size(16.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${session.crossCount}", color = Color(0xFFB91C1C), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Filled.Close, contentDescription = "Cross", tint = Color(0xFFB91C1C), modifier = Modifier.size(16.dp))
                        }
                    }
                    if (session.totalCheckTimeMs > 0 || session.totalCrossTimeMs > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF79747E))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Avg Tick: ${formatDuration(session.avgCheckTimeMs)}", fontSize = 10.sp, color = Color(0xFF79747E))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF79747E))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Avg Cross: ${formatDuration(session.avgCrossTimeMs)}", fontSize = 10.sp, color = Color(0xFF79747E))
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Top) {
                        Text("${session.accuracy.toInt()}", fontSize = 32.sp, fontWeight = FontWeight.Light, color = Color(0xFF1C1B1F))
                        Text("%", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1C1B1F).copy(alpha = 0.4f), modifier = Modifier.padding(top = 4.dp))
                    }
                    Text("ACCURACY", fontSize = 10.sp, color = Color(0xFF79747E), fontWeight = FontWeight.Medium)
                }
            }
            if (session.note.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF3EDF7)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(session.note, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = Color(0xFF49454F))
                }
            }
        }
    }
}

@Composable
fun NewSessionDialog(onDismiss: () -> Unit, onStart: (String, String, Boolean, String) -> Unit) {
    var selectedSize by remember { mutableStateOf("Medium") }
    var selectedTemplate by remember { mutableStateOf("Standard") }
    var enableTimer by remember { mutableStateOf(false) }
    var sessionNote by remember { mutableStateOf("") }

    val sizes = listOf("Small", "Medium", "Large")
    val templates = listOf("Standard", "Compact")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Counter", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Bubble Size", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    sizes.forEach { size ->
                        FilterChip(selected = selectedSize == size, onClick = { selectedSize = size }, label = { Text(size) })
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Template", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    templates.forEach { template ->
                        FilterChip(selected = selectedTemplate == template, onClick = { selectedTemplate = template }, label = { Text(template) })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Per-Question Timer", fontWeight = FontWeight.SemiBold)
                        Text("Records time spent on each question", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(checked = enableTimer, onCheckedChange = { enableTimer = it })
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = sessionNote,
                    onValueChange = { sessionNote = it },
                    label = { Text("Optional Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onStart(selectedSize, selectedTemplate, enableTimer, sessionNote) }) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("countr_settings", Context.MODE_PRIVATE)
    
    var autoClose by remember { mutableStateOf(prefs.getBoolean("auto_close", false)) }
    var alpha by remember { mutableFloatStateOf(prefs.getFloat("bubble_alpha", 0.95f)) }
    
    var tileSize by remember { mutableStateOf(prefs.getString("tile_size", "Medium") ?: "Medium") }
    var tileTemplate by remember { mutableStateOf(prefs.getString("tile_template", "Standard") ?: "Standard") }
    var tileTimer by remember { mutableStateOf(prefs.getBoolean("tile_timer", false)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-close Dashboard", fontWeight = FontWeight.SemiBold)
                        Text("Close app when starting bubble", fontSize = 11.sp, color = Color.Gray, lineHeight = 14.sp)
                    }
                    Switch(checked = autoClose, onCheckedChange = { 
                        autoClose = it
                        prefs.edit().putBoolean("auto_close", it).apply()
                    })
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Bubble Transparency", fontWeight = FontWeight.SemiBold)
                Slider(
                    value = alpha,
                    onValueChange = { 
                        alpha = it 
                        prefs.edit().putFloat("bubble_alpha", it).apply()
                    },
                    valueRange = 0.1f..1.0f
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Quick Settings Tile Defaults", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Bubble Size", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Small", "Medium", "Large").forEach { size ->
                        FilterChip(
                            selected = tileSize == size,
                            onClick = { 
                                tileSize = size
                                prefs.edit().putString("tile_size", size).apply()
                            },
                            label = { Text(size) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Template", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Standard", "Compact").forEach { t ->
                        FilterChip(
                            selected = tileTemplate == t,
                            onClick = { 
                                tileTemplate = t
                                prefs.edit().putString("tile_template", t).apply()
                            },
                            label = { Text(t) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Enable Timer", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Switch(checked = tileTimer, onCheckedChange = { 
                        tileTimer = it
                        prefs.edit().putBoolean("tile_timer", it).apply()
                    })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
