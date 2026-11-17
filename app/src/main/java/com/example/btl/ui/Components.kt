package com.example.btl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.btl.data.Student

@Composable
internal fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
internal fun ScreenIcon(screen: Screen) {
    val color = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val unit = size.width / 24
        fun line(x: Int, y: Int, a: Int, b: Int) =
            drawLine(color, Offset(x * unit, y * unit), Offset(a * unit, b * unit),
                1.8f * unit, StrokeCap.Round)
        fun circle(x: Int, y: Int, r: Int) =
            drawCircle(color, r * unit, Offset(x * unit, y * unit), style = Stroke(1.8f * unit))
        fun rect(x: Int, y: Int, w: Int, h: Int) =
            drawRect(color, Offset(x * unit, y * unit), Size(w * unit, h * unit), style = Stroke(1.8f * unit))
        when (screen) {
            Screen.STUDENTS -> {
                circle(12, 7, 3); line(5, 19, 5, 17); line(5, 17, 8, 14)
                line(8, 14, 16, 14); line(16, 14, 19, 17); line(19, 17, 19, 19)
            }
            Screen.COURSES -> {
                rect(4, 5, 16, 14); line(12, 5, 12, 19)
            }
            Screen.ENROLLMENTS -> {
                rect(5, 4, 14, 16); line(8, 12, 11, 15); line(11, 15, 16, 9)
            }
            Screen.REPORTS -> {
                line(4, 20, 20, 20); line(7, 16, 7, 11); line(12, 16, 12, 7); line(17, 16, 17, 4)
            }
        }
    }
}

@Composable
internal fun Badge(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer,
            RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
}

@Composable
internal fun EmptyState(title: String, description: String) {
    Panel {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun StudentCard(student: Student, detail: String? = null, actions: (@Composable RowScope.() -> Unit)? = null) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Text(student.name.trim().take(1).uppercase(), fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f)) {
                Text(student.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${student.code}  ·  ${student.age} tuổi", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Scratch: ${student.scratchLevel}", style = MaterialTheme.typography.bodyMedium)
        if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actions != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, content = actions)
    }
}

@Composable
internal fun ChoiceField(label: String, options: List<Pair<String, String>>, selected: String?,
                         enabled: Boolean = true, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 6.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, enabled = enabled && options.isNotEmpty(),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Text(options.firstOrNull { it.first == selected }?.second ?: "Chọn $label",
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp))
                Text("▾")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
                modifier = Modifier.heightIn(max = 300.dp)) {
                options.forEach { (key, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = { onSelect(key); expanded = false })
                }
            }
        }
    }
}

@Composable
internal fun ErrorText(error: String?) {
    if (error != null) Text(error, color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium)
}
