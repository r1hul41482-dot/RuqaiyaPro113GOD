package com.ruqaiyapro.debug.ui

import android.os.Build
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruqaiyapro.debug.brain.AIProviderType
import com.ruqaiyapro.debug.core.Core
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(core: Core) {
    val ctx = LocalContext.current
    val brain = core.brain
    var provider by remember { mutableStateOf(brain.getSelectedProvider()) }
    var apiKey by remember(provider) { mutableStateOf(brain.getApiKey(provider)) }
    var model by remember(provider) { mutableStateOf(brain.getModel(provider)) }
    var active by remember { mutableStateOf(brain.isBrainActive()) }
    var dynamic by remember { mutableStateOf(brain.isDynamicFeaturesEnabled()) }

    var testPrompt by remember { mutableStateOf("Boss ${core.ownerName} er jonno ek shundor shokal e shayari bolo") }
    var testReply by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    var lastOk by remember { mutableStateOf(false) }

    var ghRepo by remember { mutableStateOf(brain.getString("gh_repo")) }
    var ghBranch by remember { mutableStateOf(brain.getString("gh_branch", "main")) }
    var ghToken by remember { mutableStateOf(brain.getString("gh_token")) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RCard("Universal Brain (API Key)") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AIProviderType.values().forEach { p ->
                        RChip(p.displayName, provider == p) { provider = p; brain.setSelectedProvider(p); active = brain.isBrainActive() }
                    }
                }
                Spacer(Modifier.height(8.dp))
                RField(apiKey, { apiKey = it }, "${provider.displayName} API key (${provider.hint})", password = true)
                Spacer(Modifier.height(6.dp))
                RField(model, { model = it }, "Model name (change hole ekhane likho)")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RButton("Save", onClick = {
                        brain.setApiKey(provider, apiKey)
                        brain.setModel(provider, model)
                        active = brain.isBrainActive()
                        toast(ctx, "Saved (encrypted)")
                    }, modifier = Modifier.weight(1f))
                    StatusPill(if (active) "SDK ACTIVE" else "NO KEY", if (active) RC.Green else RC.Amber)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enable Dynamic AI Features", color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Switch(checked = dynamic, onCheckedChange = { dynamic = it; brain.setDynamicFeaturesEnabled(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = RC.Green, checkedThumbColor = Color.Black))
                }
                Text("Key phone-er encrypted storage-e thake, sudhu tomar choose kora AI provider-er kache jay.",
                    color = RC.TextSec, fontSize = 10.sp)
            }
        }

        item {
            RCard("Live AI Playground") {
                RField(testPrompt, { testPrompt = it }, "Prompt", singleLine = false, minLines = 2)
                Spacer(Modifier.height(8.dp))
                RButton(if (testing) "Chinta korchi..." else "Run", onClick = {
                    brain.setApiKey(provider, apiKey); brain.setModel(provider, model); active = brain.isBrainActive()
                    testing = true
                    core.scope.launch {
                        val r = brain.generate(testPrompt, "You are Ruqaiya, a friendly assistant. Reply in Bangla/Banglish.")
                        testReply = r.text
                        lastOk = r.success
                        testing = false
                        core.logActivity("AI_GENERATION", "Playground", r.text.take(200), if (r.success) "SUCCESS" else "FAILED", r.provider.displayName)
                    }
                }, enabled = !testing, modifier = Modifier.fillMaxWidth())
                if (testReply.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(testReply, color = if (lastOk) RC.Green else RC.Red, fontSize = 13.sp)
                    if (lastOk) Row {
                        IconButton(onClick = { core.scope.launch { core.feedback.recordFeedback(testPrompt, testReply, provider.displayName, true, "PLAYGROUND"); toast(ctx, "Saved") } }) {
                            Icon(Icons.Filled.ThumbUp, "Like", tint = RC.Green)
                        }
                        IconButton(onClick = { core.scope.launch { core.feedback.recordFeedback(testPrompt, testReply, provider.displayName, false, "PLAYGROUND"); toast(ctx, "Saved") } }) {
                            Icon(Icons.Filled.ThumbDown, "Dislike", tint = RC.Red)
                        }
                    }
                }
            }
        }

        item {
            RCard("GitHub (Self-Update ar APK download)") {
                RField(ghRepo, { ghRepo = it }, "Repo (username/repo)")
                Spacer(Modifier.height(6.dp))
                RField(ghBranch, { ghBranch = it }, "Branch (main)")
                Spacer(Modifier.height(6.dp))
                RField(ghToken, { ghToken = it }, "Personal Access Token (repo + workflow)", password = true)
                Spacer(Modifier.height(8.dp))
                RButton("Save GitHub", onClick = {
                    brain.putString("gh_repo", ghRepo); brain.putString("gh_branch", ghBranch); brain.putString("gh_token", ghToken)
                    toast(ctx, "Saved (encrypted)")
                }, modifier = Modifier.fillMaxWidth())
            }
        }

        item {
            RCard("Voice & Owner") {
                Text("Voice language", color = RC.TextSec, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RChip("Bangla", core.voiceLang == "bn-BD") { core.setVoiceLang("bn-BD") }
                    RChip("English", core.voiceLang == "en-US") { core.setVoiceLang("en-US") }
                    RChip("Hindi", core.voiceLang == "hi-IN") { core.setVoiceLang("hi-IN") }
                }
                Spacer(Modifier.height(8.dp))
                var owner by remember { mutableStateOf(core.ownerName) }
                RField(owner, { owner = it; core.setOwnerName(it.ifBlank { "Rubel" }) }, "Owner name")
            }
        }

        item {
            RCard("About") {
                Text("RuqaiyaPro 1.13 for Boss ${core.ownerName}", color = Color.White, fontSize = 13.sp)
                Text("${Build.MANUFACTURER} ${Build.MODEL} | Android ${Build.VERSION.RELEASE} | API ${Build.VERSION.SDK_INT}", color = RC.TextSec, fontSize = 11.sp)
                Text("RAM ${core.totalRamMb} MB total, ${core.freeRamMb} MB free", color = RC.TextSec, fontSize = 11.sp)
            }
        }
    }
}
