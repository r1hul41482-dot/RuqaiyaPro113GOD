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

@Composable
fun FeaturesScreen() {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    val all = FeatureCatalog.all
    val list = all.filter {
        (filter == "ALL" || it.status == filter) &&
            (query.isBlank() || it.name.contains(query, true) || it.note.contains(query, true) || it.id == query.trim())
    }
    val real = all.count { it.status == "R" }
    val partial = all.count { it.status == "P" }
    val none = all.count { it.status == "N" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            RCard("113 Features - sotti status") {
                Text("REAL $real  |  PARTIAL $partial  |  NOT YET $none", color = Color.White, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                RField(query, { query = it }, "Search (nam, note ba number)")
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RChip("All", filter == "ALL") { filter = "ALL" }
                    RChip("Real", filter == "R") { filter = "R" }
                    RChip("Partial", filter == "P") { filter = "P" }
                    RChip("Not yet", filter == "N") { filter = "N" }
                }
            }
        }
        items(list, key = { it.id }) { f ->
            RCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${f.id}", color = RC.TextSec, fontSize = 11.sp, modifier = Modifier.width(36.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(f.name, color = Color.White, fontSize = 13.sp)
                        if (f.note.isNotBlank()) Text(f.note, color = RC.TextSec, fontSize = 11.sp)
                    }
                    when (f.status) {
                        "R" -> StatusPill("REAL", RC.Green)
                        "P" -> StatusPill("PARTIAL", RC.Amber)
                        else -> StatusPill("NOT YET", RC.TextSec)
                    }
                }
            }
        }
    }
}
