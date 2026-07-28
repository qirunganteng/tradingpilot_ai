package com.tradepilot.app.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun ActivityBar(
    currentRoute: String,
    onNavigate: (TradePilotDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight().width(56.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            TradePilotDestination.activityBarItems.forEach { dest ->
                val icon = iconFor(dest)
                val tint = if (currentRoute == dest.route) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                IconButton(onClick = { onNavigate(dest) }) {
                    Icon(imageVector = icon, contentDescription = dest.label, tint = tint)
                }
            }
        }
    }
}

private fun iconFor(dest: TradePilotDestination): ImageVector = when (dest) {
    TradePilotDestination.Browser -> Icons.Default.Language
    TradePilotDestination.Analysis -> Icons.Default.Analytics
    TradePilotDestination.MoneyManagement -> Icons.Default.Shield
    TradePilotDestination.Journal -> Icons.Default.MenuBook
    TradePilotDestination.Statistic -> Icons.Default.BarChart
    TradePilotDestination.Notification -> Icons.Default.Notifications
    TradePilotDestination.Settings -> Icons.Default.Settings
    TradePilotDestination.AddTrade -> Icons.Default.Add
    else -> Icons.Default.Settings
}
