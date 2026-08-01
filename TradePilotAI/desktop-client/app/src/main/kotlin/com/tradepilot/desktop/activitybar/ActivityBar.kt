package com.tradepilot.desktop.activitybar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.theme.AppColors
import com.tradepilot.desktop.theme.Dimens

/**
 * Panel yang bisa ditampilkan di SideBar kiri (Prioritas 2 & 3). AI dan
 * Settings SENGAJA tidak masuk enum ini -- AI toggle CopilotPanel di kanan
 * (perilaku lama, tetap dipertahankan), Settings buka dialog modal (bukan
 * panel di SideBar).
 */
enum class SidePanel(val label: String) {
    EXPLORER("Explorer"),
    HISTORY("History"),
    BOOKMARKS("Bookmarks"),
    DOWNLOADS("Downloads"),
    WORKSPACE("Workspace")
}

/**
 * Prioritas 2: SEBELUMNYA hanya AI & Settings yang benar-benar bisa diklik --
 * Chart/Analytics sengaja disabled (placeholder belum ada fiturnya), tapi
 * Explorer/History/Bookmark/Downloads/Workspace juga TIDAK ADA sama sekali
 * di ActivityBar lama (cuma disebut sebagai teks statis di dalam SideBar).
 * Sekarang semuanya jadi icon activity-bar sungguhan yang switch konten
 * SideBar (lihat Explorer/ExplorerPanel.kt).
 */
@Composable
fun ActivityBar(
    activePanel: SidePanel,
    onSelectPanel: (SidePanel) -> Unit,
    isCopilotVisible: Boolean,
    onToggleCopilot: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(Dimens.ACTIVITY_BAR_WIDTH_DP.dp)
            .fillMaxHeight()
            .background(AppColors.Base),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(8.dp))
            ActivityBarIcon(
                icon = Icons.Default.Folder,
                tooltip = SidePanel.EXPLORER.label,
                isActive = activePanel == SidePanel.EXPLORER,
                onClick = { onSelectPanel(SidePanel.EXPLORER) }
            )
            ActivityBarIcon(
                icon = Icons.Default.History,
                tooltip = SidePanel.HISTORY.label,
                isActive = activePanel == SidePanel.HISTORY,
                onClick = { onSelectPanel(SidePanel.HISTORY) }
            )
            ActivityBarIcon(
                icon = Icons.Default.Bookmark,
                tooltip = SidePanel.BOOKMARKS.label,
                isActive = activePanel == SidePanel.BOOKMARKS,
                onClick = { onSelectPanel(SidePanel.BOOKMARKS) }
            )
            ActivityBarIcon(
                icon = Icons.Default.Download,
                tooltip = SidePanel.DOWNLOADS.label,
                isActive = activePanel == SidePanel.DOWNLOADS,
                onClick = { onSelectPanel(SidePanel.DOWNLOADS) }
            )
            ActivityBarIcon(
                icon = Icons.Default.Workspaces,
                tooltip = SidePanel.WORKSPACE.label,
                isActive = activePanel == SidePanel.WORKSPACE,
                onClick = { onSelectPanel(SidePanel.WORKSPACE) }
            )

            Spacer(Modifier.height(8.dp))

            ActivityBarIcon(
                icon = Icons.Default.SmartToy,
                tooltip = "AI Copilot",
                isActive = isCopilotVisible,
                onClick = onToggleCopilot
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ActivityBarIcon(
                icon = Icons.Default.Settings,
                tooltip = "Settings",
                isActive = false,
                onClick = onOpenSettings
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

/**
 * Satu icon ActivityBar dengan hover, active state (garis biru di kiri +
 * tint accent, ala VSCode), tooltip, dan click animation (scale-down
 * singkat saat ditekan).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ActivityBarIcon(
    icon: ImageVector,
    tooltip: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "activityBarIconScale"
    )
    val tint by animateColorAsState(
        targetValue = when {
            isActive -> AppColors.Accent
            isHovered -> AppColors.TextPrimary
            else -> AppColors.TextSecondary
        },
        label = "activityBarIconTint"
    )

    Box(
        modifier = Modifier
            .width(Dimens.ACTIVITY_BAR_WIDTH_DP.dp)
            .height(40.dp)
            .hoverable(interactionSource)
            .background(if (isHovered && !isActive) Color.White.copy(alpha = 0.06f) else Color.Transparent)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        // Garis indikator active state di sisi kiri, ala VSCode.
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(24.dp)
                    .background(AppColors.Accent)
            )
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = tooltip,
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}
