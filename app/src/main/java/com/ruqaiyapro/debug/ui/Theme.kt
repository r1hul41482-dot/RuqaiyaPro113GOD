package com.ruqaiyapro.debug.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object RC {
    val Black = Color(0xFF000000)
    val Card = Color(0xFF141414)
    val CardBorder = Color(0xFF1F2A24)
    val Green = Color(0xFF00E676)
    val GreenDim = Color(0xFF0B3D24)
    val Cyan = Color(0xFF00E5FF)
    val Red = Color(0xFFFF1744)
    val Amber = Color(0xFFFFD600)
    val TextSec = Color(0xFF9E9E9E)
}

fun toast(ctx: Context, msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

@Composable
fun RuqaiyaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = RC.Green, onPrimary = Color.Black, secondary = RC.Cyan,
            background = RC.Black, onBackground = Color.White,
            surface = RC.Card, onSurface = Color.White,
            surfaceVariant = RC.Card, outline = RC.CardBorder
        ),
        content = content
    )
}

@Composable
fun RCard(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = RC.Card,
        border = BorderStroke(1.dp, RC.CardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (title != null) {
                Text(title, color = RC.Green, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
            }
            content()
        }
    }
}

@Composable
fun RButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, outlined: Boolean = false, enabled: Boolean = true) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick, modifier = modifier, enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (enabled) RC.Green else RC.CardBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RC.Green)
        ) { Text(text, fontSize = 13.sp, maxLines = 1) }
    } else {
        Button(
            onClick = onClick, modifier = modifier, enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RC.Green, contentColor = Color.Black)
        ) { Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
    }
}

@Composable
fun RChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick,
        label = { Text(text, fontSize = 12.sp) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = RC.GreenDim, selectedLabelColor = RC.Green
        )
    )
}

@Composable
fun RField(
    value: String, onChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, password: Boolean = false, singleLine: Boolean = true, minLines: Int = 1
) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = minLines,
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = RC.Green, unfocusedBorderColor = RC.CardBorder,
            cursorColor = RC.Green, focusedLabelColor = RC.Green
        )
    )
}

@Composable
fun StatusPill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f)) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
