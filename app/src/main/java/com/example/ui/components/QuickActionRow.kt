package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EzePrimary
import com.example.ui.theme.EzeSurfaceVariant

data class QuickActionItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val command: String
)

val defaultQuickActions = listOf(
    QuickActionItem("alarm", "Alarm 7 AM", Icons.Default.Alarm, "Set an alarm for 7 AM"),
    QuickActionItem("timer", "Timer 5m", Icons.Default.HourglassTop, "Set a timer for 5 minutes"),
    QuickActionItem("flashlight", "Flashlight", Icons.Default.FlashlightOn, "Turn on the flashlight"),
    QuickActionItem("calc", "Calculator", Icons.Default.Calculate, "What is 25 times 8?"),
    QuickActionItem("phone", "Dialer", Icons.Default.Phone, "Open dialer"),
    QuickActionItem("notifs", "Notifications", Icons.Default.Notifications, "Read my notifications"),
    QuickActionItem("search", "Web Search", Icons.Default.Search, "Search for latest tech news")
)

@Composable
fun QuickActionRow(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("quick_actions_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        defaultQuickActions.forEach { item ->
            AssistChip(
                onClick = { onActionClick(item.command) },
                label = { Text(item.title, style = MaterialTheme.typography.labelMedium) },
                leadingIcon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = EzePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = EzeSurfaceVariant.copy(alpha = 0.85f),
                    labelColor = MaterialTheme.colorScheme.onSurface
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = EzePrimary.copy(alpha = 0.25f)
                ),
                modifier = Modifier.testTag("quick_action_${item.id}")
            )
        }
    }
}
