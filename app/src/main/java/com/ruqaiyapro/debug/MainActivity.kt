package com.ruqaiyapro.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.ruqaiyapro.debug.brain.BrainManager
import com.ruqaiyapro.debug.feedback.FeedbackManager
import com.ruqaiyapro.debug.features.Features113Manager
import com.ruqaiyapro.debug.selfupdate.SelfUpdateEngine

class MainActivity : ComponentActivity() {

    private lateinit var brainManager: BrainManager
    private lateinit var feedbackManager: FeedbackManager
    private lateinit var featuresManager: Features113Manager
    private lateinit var selfUpdateEngine: SelfUpdateEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        brainManager = BrainManager(this)
        feedbackManager = FeedbackManager(this)
        featuresManager = Features113Manager(this)
        selfUpdateEngine = SelfUpdateEngine(this, brainManager, feedbackManager)

        setContent {
            RuqaiyaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    RuqaiyaMainScreen(
                        brainManager = brainManager,
                        feedbackManager = feedbackManager,
                        featuresManager = featuresManager,
                        selfUpdateEngine = selfUpdateEngine
                    )
                }
            }
        }
    }
}

@Composable
fun RuqaiyaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00E676),
            background = Color.Black,
            surface = Color(0xFF141414)
        ),
        content = content
    )
}

@Composable
fun RuqaiyaMainScreen(
    brainManager: BrainManager,
    feedbackManager: FeedbackManager,
    featuresManager: Features113Manager,
    selfUpdateEngine: SelfUpdateEngine
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Hub", "Face", "Control", "107", "Activity", "Self-Update", "Settings")

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Top Bar
        Surface(
            color = Color(0xFF0A0A0A),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "RuqaiyaPro FOR BOSS RUBEL",
                    color = Color(0xFF00E676),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Infinix HOT 12 Play X6816C | API 30 | 4GB+3GB MemFusion",
                    color = Color(0xFF8E8E93),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> HubTabContent()
                1 -> FaceTabContent()
                2 -> ControlTabContent()
                3 -> FeaturesTabContent(featuresManager)
                4 -> ActivityTabContent()
                5 -> SelfUpdateTabContent(selfUpdateEngine)
                6 -> SettingsTabContent(brainManager, feedbackManager)
            }
        }

        // Bottom Navigation Bar
        NavigationBar(containerColor = Color(0xFF0A0A0A)) {
            tabs.forEachIndexed { index, title ->
                NavigationBarItem(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    label = { Text(title, color = if (selectedTab == index) Color(0xFF00E676) else Color.Gray) },
                    icon = { }
                )
            }
        }
    }
}

@Composable fun HubTabContent() { Text("Hub Tab Content", color = Color.White) }
@Composable fun FaceTabContent() { Text("Face Tab Content", color = Color.White) }
@Composable fun ControlTabContent() { Text("Control Tab Content", color = Color.White) }
@Composable fun FeaturesTabContent(manager: Features113Manager) { Text("107 Features Tab", color = Color.White) }
@Composable fun ActivityTabContent() { Text("Activity Feed (RecyclerView)", color = Color.White) }
@Composable fun SelfUpdateTabContent(engine: SelfUpdateEngine) { Text("Self-Update Engine Tab", color = Color.White) }
@Composable fun SettingsTabContent(brain: BrainManager, fb: FeedbackManager) { Text("Universal Brain Settings", color = Color.White) }