package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Timer
import com.example.data.CounterSession
import com.example.ui.theme.CountrTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CountrTheme {
                MainScreen(
                    viewModel = viewModel,
                    onStartSession = { size, template, timer ->
                        startBubbleSession(size, template, timer)
                    }
                )
            }
        }
    }

    private fun startBubbleSession(size: String, template: String, enableTimer: Boolean) {
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
        }
        startService(serviceIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onStartSession: (String, String, Boolean) -> Unit) {
    val sessions by viewModel.previousSessions.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
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
                    IconButton(onClick = { importLauncher.launch("application/json") }) {
                        Icon(Icons.Filled.Download, contentDescription = "Import", tint = Color(0xFF6750A4))
                    }
                    IconButton(onClick = { exportLauncher.launch("countr_sessions.json") }) {
                        Icon(Icons.Filled.Upload, contentDescription = "Export", tint = Color(0xFF6750A4))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SESSION HISTORY",
                        color = Color(0xFF49454F),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "${sessions.size} Total",
                        color = Color(0xFF6750A4),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        SessionCard(session)
                    }
                    item { Spacer(modifier = Modifier.height(88.dp)) }
                }
            }
        }

        if (showDialog) {
            NewSessionDialog(
                onDismiss = { showDialog = false },
                onStart = { size, template, timer ->
                    onStartSession(size, template, timer)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun SessionCard(session: CounterSession) {
    val dateFormat = SimpleDateFormat("MMM dd • HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(session.timestamp)).uppercase()
    
    fun formatDuration(ms: Long): String {
        if (ms == 0L) return "0s"
        val s = ms / 1000
        return if (s > 60) "${s/60}m ${s%60}s" else "${s}s"
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, Color(0xFFE7E0EC))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = dateString, 
                    fontSize = 12.sp, 
                    color = Color(0xFF6750A4),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "${session.checkCount}", color = Color(0xFF2D6A4F), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Filled.Check, contentDescription = "Check", tint = Color(0xFF2D6A4F), modifier = Modifier.size(16.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text = "${session.crossCount}", color = Color(0xFFB91C1C), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Filled.Close, contentDescription = "Cross", tint = Color(0xFFB91C1C), modifier = Modifier.size(16.dp))
                    }
                }
                if (session.totalCheckTimeMs > 0 || session.totalCrossTimeMs > 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
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
                    Text(
                        text = "${session.accuracy.toInt()}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        color = Color(0xFF1C1B1F)
                    )
                    Text(
                        text = "%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1C1B1F).copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Text(
                    text = "ACCURACY",
                    fontSize = 10.sp,
                    color = Color(0xFF79747E),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun NewSessionDialog(onDismiss: () -> Unit, onStart: (String, String, Boolean) -> Unit) {
    var selectedSize by remember { mutableStateOf("Medium") }
    var selectedTemplate by remember { mutableStateOf("Standard") }
    var enableTimer by remember { mutableStateOf(false) }

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
                        FilterChip(
                            selected = selectedSize == size,
                            onClick = { selectedSize = size },
                            label = { Text(size) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Template", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    templates.forEach { template ->
                        FilterChip(
                            selected = selectedTemplate == template,
                            onClick = { selectedTemplate = template },
                            label = { Text(template) }
                        )
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
            }
        },
        confirmButton = {
            Button(onClick = { onStart(selectedSize, selectedTemplate, enableTimer) }) {
                Text("Start")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
