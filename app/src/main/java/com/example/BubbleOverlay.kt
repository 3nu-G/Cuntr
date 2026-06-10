package com.example

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun BubbleOverlay(
    sizeStr: String,
    templateStr: String,
    enableTimer: Boolean,
    onClose: (Int, Int, Long, Long) -> Unit,
    onDrag: (Float, Float) -> Unit
) {
    var checkCount by remember { mutableIntStateOf(0) }
    var crossCount by remember { mutableIntStateOf(0) }
    
    var checkTimeTotal by remember { mutableLongStateOf(0L) }
    var crossTimeTotal by remember { mutableLongStateOf(0L) }
    var elapsedTime by remember { mutableLongStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(false) }
    
    val total = checkCount + crossCount
    val accuracy = if (total == 0) 0f else (checkCount.toFloat() / total.toFloat()) * 100f
    
    var dragYOffset by remember { mutableFloatStateOf(0f) }
    
    val isCompact = templateStr.lowercase() == "compact"
    
    val width = when (sizeStr.lowercase()) {
        "small" -> 60.dp
        "large" -> 100.dp
        else -> 80.dp
    }

    val iconSizeOuter = when (sizeStr.lowercase()) {
        "small" -> 16.dp
        "large" -> 32.dp
        else -> 24.dp
    }
    
    val accuracySize = when (sizeStr.lowercase()) {
        "small" -> 10.sp
        "large" -> 16.sp
        else -> 12.sp
    }

    val countSize = when (sizeStr.lowercase()) {
        "small" -> 8.sp
        "large" -> 14.sp
        else -> 10.sp
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            var lastTime = System.currentTimeMillis()
            while(isTimerRunning) {
                delay(50)
                val now = System.currentTimeMillis()
                elapsedTime += (now - lastTime)
                lastTime = now
            }
        }
    }

    // A visually distinct modern vertical bubble
    Box(
        modifier = Modifier
            .width(width)
            .shadow(16.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF6750A4))
            .border(2.dp, Color.White, RoundedCornerShape(50))
            .padding(8.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragYOffset > 300f) { // threshold to close
                            onClose(checkCount, crossCount, checkTimeTotal, crossTimeTotal)
                        } else {
                            dragYOffset = 0f
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    dragYOffset += dragAmount.y
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (enableTimer) {
                val seconds = (elapsedTime / 1000) % 60
                val minutes = (elapsedTime / 1000) / 60
                val tColor = if (isTimerRunning) Color(0xFF3DDC84) else Color.LightGray
                Text(
                    text = String.format("%02d:%02d", minutes, seconds),
                    color = tColor,
                    fontSize = if (isCompact && sizeStr.lowercase() == "small") 8.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { isTimerRunning = !isTimerRunning }
                )
            }

            Text(
                text = "${accuracy.toInt()}%",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = accuracySize,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { 
                            checkCount++ 
                            if (enableTimer) {
                                checkTimeTotal += elapsedTime
                                elapsedTime = 0L
                                isTimerRunning = true
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!isCompact) {
                            Text(text = "$checkCount", color = Color.White, fontSize = countSize, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Filled.Check, contentDescription = "Check", tint = Color.White, modifier = Modifier.size(iconSizeOuter))
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { 
                            crossCount++ 
                            if (enableTimer) {
                                crossTimeTotal += elapsedTime
                                elapsedTime = 0L
                                isTimerRunning = true
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!isCompact) {
                            Text(text = "$crossCount", color = Color.White, fontSize = countSize, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Filled.Close, contentDescription = "Cross", tint = Color.White, modifier = Modifier.size(iconSizeOuter))
                    }
                }
            }
        }
        
        // Show close indicator when dragging downwards significantly
        if (dragYOffset > 100f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}
