package com.ruqaiyapro.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruqaiyapro.debug.core.Core
import com.ruqaiyapro.debug.ui.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val core = (application as RuqaiyaApp).core
        setContent {
            RuqaiyaTheme { RuqaiyaRoot(core, this) }
        }
    }
}

private data class TabDef(val title: String, val icon: ImageVector)

@Composable
fun RuqaiyaRoot(core: Core, activity: ComponentActivity) {
    val tabs = remember {
        listOf(
            TabDef("Hub", Icons.Filled.Home),
            TabDef("Face", Icons.Filled.Face),
            TabDef("Control", Icons.Filled.Tune),
            TabDef("107", Icons.Filled.Apps),
            TabDef("Activity", Icons.Filled.History),
            TabDef("Update", Icons.Filled.SystemUpdate),
            TabDef("Settings", Icons.Filled.Settings)
        )
    }
    var selected by remember { mutableIntStateOf(0) }

    // Screen brightness (also drives the 1% Ultra Saver)
    LaunchedEffect(Unit) {
        snapshotFlow { if (core.ultraSaver) 0.01f else core.brightness }.collect { b ->
            try {
                val lp = activity.window.attributes
                lp.screenBrightness = b
                activity.window.attributes = lp
            } catch (e: Exception) { }
        }
    }

    // RAM check every 15 s (60 s in Ultra Saver)
    LaunchedEffect(Unit) {
        var warned = false
        while (true) {
            val (free, total) = core.device.ramInfo()
            core.freeRamMb = free
            core.totalRamMb = total
            if (free < 500 && !warned) {
                core.logHud("Low RAM: ${free}MB free")
                System.gc()
                warned = true
            } else if (free >= 500) warned = false
            delay(if (core.ultraSaver) 60_000L else 15_000L)
        }
    }

    Scaffold(
        containerColor = RC.Black,
        topBar = {
            Surface(color = Color(0xFF0A0A0A)) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ruqaiya", color = RC.Green, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Boss ${core.ownerName}'s companion", color = RC.TextSec, fontSize = 11.sp)
                    }
                    StatusPill(if (core.brain.isBrainActive()) "BRAIN ON" else "BRAIN OFF",
                        if (core.brain.isBrainActive()) RC.Green else RC.Amber)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0A0A0A), tonalElevation = 0.dp) {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = selected == i,
                        onClick = { selected = i },
                        icon = { Icon(t.icon, contentDescription = t.title, modifier = Modifier.size(20.dp)) },
                        label = { Text(t.title, fontSize = 9.sp, maxLines = 1) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = RC.Green,
                            indicatorColor = RC.Green,
                            unselectedIconColor = RC.TextSec,
                            unselectedTextColor = RC.TextSec
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(modifier = Modifier.padding(pad).fillMaxSize().background(RC.Black)) {
            when (selected) {
                0 -> HubScreen(core)
                1 -> FaceScreen(core, activity)
                2 -> ControlScreen(core)
                3 -> FeaturesScreen()
                4 -> ActivityScreen()
                5 -> SelfUpdateScreen(core)
                else -> SettingsScreen(core)
            }
        }
    }
}
