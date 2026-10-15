package com.ruqaiyapro.debug.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ruqaiyapro.debug.core.Core
import kotlinx.coroutines.launch

@Composable
fun HubScreen(core: Core) {
    val ctx = LocalContext.current
    val a = core.assistant
    var typed by remember { mutableStateOf("") }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) a.startListening() else toast(ctx, "Mic permission lagbe")
    }

    fun onMic() {
        if (a.listening) { a.stopListening(); return }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) a.startListening()
        else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(1f, 1.15f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Device status
        item {
            RCard("Device") {
                Text("${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                val low = core.freeRamMb in 1..499
                Text("RAM free: ${core.freeRamMb} MB / ${core.totalRamMb} MB",
                    color = if (low) RC.Amber else RC.TextSec, fontSize = 12.sp)
                if (core.ultraSaver) Text("Battery 1% Ultra Saver ON", color = RC.Amber, fontSize = 12.sp)
            }
        }

        // Mic
        item {
            RCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(if (a.listening) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(if (a.listening) RC.Red else RC.Green)
                            .clickable { onMic() },
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Mic, contentDescription = "Mic", tint = Color.Black, modifier = Modifier.size(40.dp)) }
                    Spacer(Modifier.height(10.dp))
                    Text(if (a.listening) "Shunchi... bolo" else "Mic chap diye bolo (\"What time\", \"Flash jalo\", \"Shayari bolo\")",
                        color = RC.TextSec, fontSize = 12.sp)
                }
                if (a.transcript.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(a.transcript, color = Color.White, fontSize = 13.sp)
                }
                if (a.reply.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Ruqaiya: ${a.reply}", color = RC.Green, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (a.lastWasAi) {
                        Row {
                            IconButton(onClick = {
                                core.scope.launch {
                                    core.feedback.recordFeedback(a.lastPrompt, a.reply, core.brain.getSelectedProvider().displayName, true, "CHAT")
                                    toast(ctx, "Thumbs up saved")
                                }
                            }) { Icon(Icons.Filled.ThumbUp, "Like", tint = RC.Green) }
                            IconButton(onClick = {
                                core.scope.launch {
                                    core.feedback.recordFeedback(a.lastPrompt, a.reply, core.brain.getSelectedProvider().displayName, false, "CHAT")
                                    toast(ctx, "Thumbs down saved")
                                }
                            }) { Icon(Icons.Filled.ThumbDown, "Dislike", tint = RC.Red) }
                        }
                    }
                }
            }
        }

        // Quick chips
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Open Camera", "What Time", "What Date", "Torch On", "Torch Off", "Shayari bolo", "Koutuk bolo", "Roast koro", "Amar naam ki?").forEach { c ->
                    AssistChip(
                        onClick = { a.handle(c) },
                        label = { Text(c, fontSize = 12.sp) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = RC.Green)
                    )
                }
            }
        }

        // Typed command
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RField(typed, { typed = it }, "Command likho...", modifier = Modifier.weight(1f))
                IconButton(onClick = { a.handle(typed); typed = "" }) {
                    Icon(Icons.Filled.Send, "Send", tint = RC.Green)
                }
            }
        }

        // HUD
        item {
            RCard("Live HUD Logs") {
                if (core.hud.isEmpty()) Text("Kono log nei", color = RC.TextSec, fontSize = 12.sp)
                core.hud.take(10).forEach {
                    Text(it, color = RC.TextSec, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
