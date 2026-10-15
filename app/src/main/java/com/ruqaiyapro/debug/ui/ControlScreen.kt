package com.ruqaiyapro.debug.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ruqaiyapro.debug.RuqaiyaApp
import com.ruqaiyapro.debug.activity.KhataEntity
import com.ruqaiyapro.debug.core.Core
import com.ruqaiyapro.debug.floating.FloatingChibiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = RC.Green, checkedThumbColor = Color.Black))
    }
}

@Composable
fun ControlScreen(core: Core) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val dev = core.device
    var volume by remember { mutableFloatStateOf(dev.getVolumePercent().toFloat()) }

    val storagePerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                ctx.contentResolver.openInputStream(uri)?.use { input ->
                    File(ctx.filesDir, "chibi_custom.png").outputStream().use { input.copyTo(it) }
                }
                toast(ctx, "Chibi chobi save holo")
                core.logHud("Custom chibi image set")
                if (FloatingChibiService.running) {
                    ctx.stopService(Intent(ctx, FloatingChibiService::class.java))
                    core.scope.launch {
                        delay(300)
                        ctx.startService(Intent(ctx, FloatingChibiService::class.java))
                    }
                }
            } catch (e: Exception) {
                toast(ctx, "Chobi nite parlam na: ${e.message}")
            }
        }
    }

    fun startChibi() {
        if (!Settings.canDrawOverlays(ctx)) {
            toast(ctx, "Overlay permission dao, tarpor abar Start chapo")
            ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
            return
        }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            storagePerm.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        try {
            ctx.startService(Intent(ctx, FloatingChibiService::class.java))
            core.chibiRunning = true
            core.logHud("Chibi started")
        } catch (e: Exception) {
            toast(ctx, "Chibi start hoy ni: ${e.message}")
        }
    }

    fun stopChibi() {
        ctx.stopService(Intent(ctx, FloatingChibiService::class.java))
        core.chibiRunning = false
        core.logHud("Chibi stopped")
    }

    val termuxCmd = "cp /sdcard/Download/tomar_pic.png /sdcard/Android/data/${ctx.packageName}/files/chibi_custom.png"

    // ---- Khata
    val khataFlow = remember { RuqaiyaApp.instance.database.khataDao().all() }
    val khata by khataFlow.collectAsState(initial = emptyList())
    var kName by remember { mutableStateOf("") }
    var kAmount by remember { mutableStateOf("") }
    var kNote by remember { mutableStateOf("") }
    var kGave by remember { mutableStateOf(true) }
    val owedToMe = khata.filter { it.iGave }.sumOf { it.amount }
    val iOwe = khata.filter { !it.iGave }.sumOf { it.amount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RCard("Device Control") {
                Text("Master Volume: ${volume.toInt()}%", color = Color.White, fontSize = 13.sp)
                Slider(value = volume, onValueChange = { volume = it; dev.setVolumePercent(it.toInt()) }, valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = RC.Green, activeTrackColor = RC.Green))
                Text("Screen Brightness: ${(core.brightness * 100).toInt()}%", color = Color.White, fontSize = 13.sp)
                Slider(value = core.brightness, onValueChange = { core.brightness = it.coerceIn(0.05f, 1f) }, valueRange = 0.05f..1f,
                    colors = SliderDefaults.colors(thumbColor = RC.Green, activeTrackColor = RC.Green))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RButton("Vol 65%", onClick = { volume = 65f; dev.setVolumePercent(65) }, outlined = true, modifier = Modifier.weight(1f))
                    RButton("Bright 70%", onClick = { core.brightness = 0.7f }, outlined = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                SwitchRow("Flashlight", dev.torchOn) { if (!dev.setTorch(it)) toast(ctx, "Torch kaj korlo na") }
                SwitchRow("Flash Strobe (GXC)", dev.strobeOn) { dev.setStrobe(it) }
                SwitchRow("Battery 1% Ultra Saver (screen 1%)", core.ultraSaver) { core.setUltraSaver(it) }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RButton("Wi-Fi panel", onClick = { try { ctx.startActivity(dev.wifiIntent()) } catch (e: Exception) { toast(ctx, "Khola gelo na") } }, outlined = true, modifier = Modifier.weight(1f))
                    RButton("Bluetooth", onClick = { try { ctx.startActivity(dev.bluetoothIntent()) } catch (e: Exception) { toast(ctx, "Khola gelo na") } }, outlined = true, modifier = Modifier.weight(1f))
                }
                Text("Android 10+ e app Wi-Fi/Bluetooth nije ON/OFF korte pare na, tai panel khole.", color = RC.TextSec, fontSize = 10.sp)
            }
        }

        item {
            RCard("Floating Chibi") {
                Text(if (core.chibiRunning || FloatingChibiService.running) "Status: RUNNING" else "Status: STOPPED",
                    color = if (core.chibiRunning || FloatingChibiService.running) RC.Green else RC.TextSec, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RButton("Start", onClick = { startChibi() }, modifier = Modifier.weight(1f))
                    RButton("Stop", onClick = { stopChibi() }, outlined = true, modifier = Modifier.weight(1f))
                    RButton("Reload", onClick = {
                        stopChibi()
                        core.scope.launch { delay(300); startChibi() }
                    }, outlined = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RButton("Chibi Pic Change", onClick = { picker.launch("image/*") }, modifier = Modifier.weight(1f))
                    RButton("Reset Default", onClick = {
                        listOf(File(ctx.filesDir, "chibi_custom.png"), File(ctx.filesDir, "chibi.png"),
                            File(ctx.getExternalFilesDir(null), "chibi_custom.png"), File(ctx.getExternalFilesDir(null), "chibi.png"))
                            .forEach { try { it.delete() } catch (e: Exception) { } }
                        toast(ctx, "Default chibi e fire gelo")
                    }, outlined = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text("Gesture: 1 angul drag = move, 2 angul pinch = size (40-300dp), tap = bounce + vibrate, 500ms press = menu toast.",
                    color = RC.TextSec, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Text("Termux command (Android 11-e Termux Android/data-te likhte nao pare - tokhon 'Chibi Pic Change' button use koro):",
                    color = RC.TextSec, fontSize = 11.sp)
                Text(termuxCmd, color = RC.Cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                RButton("Copy command", onClick = { clipboard.setText(AnnotatedString(termuxCmd)); toast(ctx, "Copied") }, outlined = true)
            }
        }

        item {
            RCard("Khata (Hisab & Dhar)") {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("Pabo", color = RC.TextSec, fontSize = 11.sp)
                        Text("৳ ${"%.0f".format(owedToMe)}", color = RC.Green, fontSize = 18.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Dibo", color = RC.TextSec, fontSize = 11.sp)
                        Text("৳ ${"%.0f".format(iOwe)}", color = RC.Red, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                RField(kName, { kName = it }, "Naam")
                Spacer(Modifier.height(6.dp))
                RField(kAmount, { kAmount = it.filter { c -> c.isDigit() || c == '.' } }, "Taka")
                Spacer(Modifier.height(6.dp))
                RField(kNote, { kNote = it }, "Note (optional)")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RChip("Ami dilam (pabo)", kGave) { kGave = true }
                    RChip("Ami nilam (dibo)", !kGave) { kGave = false }
                }
                Spacer(Modifier.height(6.dp))
                RButton("Add", onClick = {
                    val amt = kAmount.toDoubleOrNull()
                    if (kName.isBlank() || amt == null || amt <= 0.0) { toast(ctx, "Naam ar taka din"); return@RButton }
                    core.scope.launch(Dispatchers.IO) {
                        RuqaiyaApp.instance.database.khataDao().insert(KhataEntity(person = kName.trim(), amount = amt, iGave = kGave, note = kNote.trim()))
                    }
                    kName = ""; kAmount = ""; kNote = ""
                }, modifier = Modifier.fillMaxWidth())
            }
        }

        items(khata, key = { it.id }) { k ->
            RCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(k.person, color = Color.White, fontSize = 14.sp)
                        if (k.note.isNotBlank()) Text(k.note, color = RC.TextSec, fontSize = 11.sp)
                    }
                    Text((if (k.iGave) "+" else "-") + "৳ ${"%.0f".format(k.amount)}",
                        color = if (k.iGave) RC.Green else RC.Red, fontSize = 15.sp)
                    IconButton(onClick = {
                        core.scope.launch(Dispatchers.IO) { RuqaiyaApp.instance.database.khataDao().delete(k.id) }
                    }) { Icon(Icons.Filled.Delete, "Delete", tint = RC.TextSec) }
                }
            }
        }
    }
}
