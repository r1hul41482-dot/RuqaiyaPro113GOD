package com.ruqaiyapro.debug.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruqaiyapro.debug.RuqaiyaApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityScreen() {
    val dao = RuqaiyaApp.instance.database.activityDao()
    val logsFlow = remember { dao.getAllLogs() }
    val logs by logsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Real-time Activity Feed (${logs.size})", color = RC.Green, fontSize = 14.sp, modifier = Modifier.weight(1f))
                RButton("Clear", onClick = { scope.launch(Dispatchers.IO) { dao.clearAll() } }, outlined = true)
            }
        }
        if (logs.isEmpty()) {
            item { Text("Ekhono kono activity nei. Hub-e command dao ba Update tab use koro.", color = RC.TextSec, fontSize = 12.sp) }
        }
        items(logs, key = { it.id }) { l ->
            RCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(l.eventType, RC.Cyan)
                    Spacer(Modifier.weight(1f))
                    Text(fmt.format(Date(l.timestamp)), color = RC.TextSec, fontSize = 10.sp)
                    Spacer(Modifier.width(8.dp))
                    StatusPill(l.status, when (l.status) {
                        "SUCCESS" -> RC.Green
                        "FAILED" -> RC.Red
                        "WARNING" -> RC.Amber
                        else -> RC.Cyan
                    })
                }
                Spacer(Modifier.height(6.dp))
                Text(l.title, color = Color.White, fontSize = 13.sp)
                if (l.description.isNotBlank()) Text(l.description.take(300), color = RC.TextSec, fontSize = 11.sp)
            }
        }
    }
}
