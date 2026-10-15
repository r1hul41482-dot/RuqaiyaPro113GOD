package com.ruqaiyapro.debug.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ruqaiyapro.debug.core.Core
import com.ruqaiyapro.debug.selfupdate.SelfUpdateEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun installApk(ctx: Context, apk: File) {
    if (Build.VERSION.SDK_INT >= 26 && !ctx.packageManager.canRequestPackageInstalls()) {
        toast(ctx, "'Install unknown apps' allow koro, tarpor abar chapo")
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return
    }
    try {
        val uri = FileProvider.getUriForFile(ctx, "com.ruqaiyapro.debug.fileprovider", apk)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(i)
    } catch (e: Exception) {
        toast(ctx, "Install intent fail: ${e.message}")
    }
}

private suspend fun readAppLogs(): String = withContext(Dispatchers.IO) {
    try {
        val p = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", "200", "*:E"))
        p.inputStream.bufferedReader().readText().takeLast(3500)
    } catch (e: Exception) {
        "Log pawa gelo na: ${e.message}"
    }
}

@Composable
fun SelfUpdateScreen(core: Core) {
    val ctx = LocalContext.current
    val engine = remember { SelfUpdateEngine(core.brain, core.feedback) }
    var prompt by remember { mutableStateOf("") }
    var errorLog by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SelfUpdateEngine.GenResult?>(null) }
    var showRaw by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<String>() }

    fun log(s: String) {
        core.logHud(s)
        logs.add(0, s)
        while (logs.size > 40) logs.removeAt(logs.size - 1)
    }

    fun ghReady(): Boolean {
        val ok = core.brain.getString("gh_repo").contains("/") && core.brain.getString("gh_token").isNotBlank()
        if (!ok) toast(ctx, "Settings-e GitHub repo ar token dao")
        return ok
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RCard("GOD #1 Self-Update") {
                Text("Prompt dao -> AI code likhbe -> GitHub-e commit -> GitHub APK build korbe -> ekhane download & install.",
                    color = RC.TextSec, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                RField(prompt, { prompt = it }, "Bach ki update korbo? (Bangla/English)", singleLine = false, minLines = 3)
                Spacer(Modifier.height(8.dp))
                RButton(if (busy) "Kaj cholche..." else "Generate code", onClick = {
                    if (prompt.isBlank()) { toast(ctx, "Prompt likho"); return@RButton }
                    if (!core.brain.isBrainActive()) { toast(ctx, "Age Settings-e API key dao"); return@RButton }
                    busy = true
                    core.scope.launch {
                        val r = engine.generate(prompt) { log(it) }
                        result = r
                        busy = false
                        core.logActivity("SELF_UPDATE", "Generate: ${prompt.take(60)}", "${r?.files?.size ?: 0} files",
                            if (r != null) "SUCCESS" else "FAILED", core.brain.getSelectedProvider().displayName)
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            RCard("Auto-Fix Engine") {
                Text("App-er error log AI-ke pathiye fix newa.", color = RC.TextSec, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                RField(errorLog, { errorLog = it }, "Error log (paste ba Load logs)", singleLine = false, minLines = 3)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RButton("Load logs", onClick = { core.scope.launch { errorLog = readAppLogs() } }, outlined = true, modifier = Modifier.weight(1f))
                    RButton("Fix Kor", onClick = {
                        if (errorLog.isBlank()) { toast(ctx, "Age log din"); return@RButton }
                        if (!core.brain.isBrainActive()) { toast(ctx, "Age Settings-e API key dao"); return@RButton }
                        busy = true
                        core.scope.launch {
                            val p = "Ei Android runtime/build error fix koro. Sudhu fix kora file dao.\n\n$errorLog"
                            result = engine.generate(p) { log(it) }
                            busy = false
                        }
                    }, enabled = !busy, modifier = Modifier.weight(1f))
                }
            }
        }

        result?.let { res ->
            item {
                RCard("AI Result (${res.provider})") {
                    if (res.files.isEmpty()) {
                        Text("Kono file block paoa jay ni. Raw uttor:", color = RC.Amber, fontSize = 12.sp)
                    } else {
                        res.files.forEach { Text("• ${it.path}  (${it.content.length} chars)", color = Color.White, fontSize = 12.sp) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RButton(if (showRaw) "Hide code" else "Show code", onClick = { showRaw = !showRaw }, outlined = true, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            core.scope.launch { core.feedback.recordFeedback(prompt, res.rawText, res.provider, true, "SELF_UPDATE"); toast(ctx, "Saved") }
                        }) { Icon(Icons.Filled.ThumbUp, "Like", tint = RC.Green) }
                        IconButton(onClick = {
                            core.scope.launch { core.feedback.recordFeedback(prompt, res.rawText, res.provider, false, "SELF_UPDATE"); toast(ctx, "Saved") }
                        }) { Icon(Icons.Filled.ThumbDown, "Dislike", tint = RC.Red) }
                    }
                    if (showRaw) {
                        Spacer(Modifier.height(6.dp))
                        Text(res.rawText.take(6000), color = RC.TextSec, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (res.files.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Sotorkota: AI-er code main branch-e commit hobe. Vul thakle build fail korbe (app bhange na, karon purano APK thakbe).",
                            color = RC.Amber, fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        RButton("Commit & Build on GitHub", onClick = {
                            if (!ghReady()) return@RButton
                            busy = true
                            core.scope.launch {
                                val run = engine.commitAndWatch(
                                    core.brain.getString("gh_repo"), core.brain.getString("gh_branch", "main").ifBlank { "main" },
                                    core.brain.getString("gh_token"), res.files, "Ruqaiya self-update: ${prompt.take(50)}"
                                ) { log(it) }
                                busy = false
                                core.logActivity("SELF_UPDATE", "Commit & build", "Run: ${run?.conclusion ?: "unknown"}",
                                    if (run?.conclusion == "success") "SUCCESS" else "WARNING")
                            }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        item {
            RCard("Latest APK") {
                Text("GitHub-er shesh successful build download kore install korbe.", color = RC.TextSec, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                RButton("Download & Install latest APK", onClick = {
                    if (!ghReady()) return@RButton
                    busy = true
                    core.scope.launch {
                        val dest = File(ctx.cacheDir, "update/ruqaiya-update.apk")
                        val ok = engine.downloadLatestApk(
                            core.brain.getString("gh_repo"), core.brain.getString("gh_branch", "main").ifBlank { "main" },
                            core.brain.getString("gh_token"), dest
                        ) { log(it) }
                        busy = false
                        if (ok) installApk(ctx, dest)
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                val repo = core.brain.getString("gh_repo")
                if (repo.contains("/")) {
                    RButton("Open Actions page", onClick = {
                        try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/$repo/actions")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { }
                    }, outlined = true, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item {
            RCard("Progress log") {
                if (logs.isEmpty()) Text("Kichu nei", color = RC.TextSec, fontSize = 12.sp)
            }
        }
        items(logs.take(15)) { l ->
            Text(l, color = RC.TextSec, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
