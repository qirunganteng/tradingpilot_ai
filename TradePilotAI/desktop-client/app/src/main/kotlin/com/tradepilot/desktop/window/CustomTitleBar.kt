package com.tradepilot.desktop.window

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.hoverable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.foundation.window.WindowDraggableArea
import com.tradepilot.desktop.theme.AppColors
import com.tradepilot.desktop.theme.Dimens
import java.awt.Frame

/**
 * Host: title bar custom di atas + konten aplikasi (Workbench) di bawahnya,
 * dibungkus dalam satu Column supaya title bar selalu nempel di paling atas
 * window persis seperti Chrome/VSCode.
 *
 * Dipanggil dari dalam `Window { ... }` (AppWindow.kt), jadi `this` di sini
 * adalah [FrameWindowScope] -- itu yang memberi akses ke `window`
 * (java.awt.Frame asli di baliknya) untuk minimize/maximize/close.
 */
@Composable
fun FrameWindowScope.CustomTitleBarHost(
    windowState: WindowState,
    onRequestExit: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(AppColors.Base)) {
        CustomTitleBar(
            title = "TradePilot AI",
            windowState = windowState,
            onMinimize = { window.extendedState = Frame.ICONIFIED },
            onToggleMaximize = {
                windowState.placement = if (windowState.placement == WindowPlacement.Maximized) {
                    WindowPlacement.Floating
                } else {
                    WindowPlacement.Maximized
                }
            },
            onClose = onRequestExit
        )
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun FrameWindowScope.CustomTitleBar(
    title: String,
    windowState: WindowState,
    onMinimize: () -> Unit,
    onToggleMaximize: () -> Unit,
    onClose: () -> Unit
) {
    val isMaximized = windowState.placement == WindowPlacement.Maximized

    // WindowDraggableArea adalah composable BAWAAN Compose Desktop (bukan
    // reimplementasi manual) -- ini yang menggantikan kemampuan "drag window"
    // yang otomatis hilang begitu title bar native dibuang (undecorated=true).
    WindowDraggableArea(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.TITLE_BAR_HEIGHT_DP.dp)
            .background(AppColors.Base)
            // Double-click title bar = toggle maximize, sama seperti
            // Chrome/Windows title bar asli.
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { onToggleMaximize() })
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                color = AppColors.TextSecondary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.weight(1f))
            TitleBarButton(
                icon = Icons.Default.Remove,
                contentDescription = "Minimize",
                onClick = onMinimize
            )
            TitleBarButton(
                icon = if (isMaximized) Icons.Default.FilterNone else Icons.Default.CropSquare,
                contentDescription = if (isMaximized) "Restore" else "Maximize",
                iconSizeDp = if (isMaximized) 12 else 13,
                onClick = onToggleMaximize
            )
            TitleBarButton(
                icon = Icons.Default.Close,
                contentDescription = "Close",
                hoverColor = AppColors.CloseHover,
                onClick = onClose
            )
        }
    }
}

/**
 * Tombol title bar (minimize/maximize/close) ala Windows: kotak 46x32,
 * berubah warna saat hover (close jadi merah, dua lainnya abu terang) --
 * TIDAK pakai Material `IconButton` bawaan karena itu berbentuk lingkaran
 * dengan ripple, bukan kotak penuh-tinggi seperti title bar OS asli.
 */
@Composable
private fun TitleBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    hoverColor: Color = Color(0xFF3A3A3A),
    iconSizeDp: Int = 11,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .width(46.dp)
            .fillMaxHeight()
            .hoverable(interactionSource)
            .background(if (isHovered) hoverColor else Color.Transparent)
            .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }) },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = if (isHovered && hoverColor == AppColors.CloseHover) Color.White else AppColors.TextSecondary,
            modifier = Modifier.size(iconSizeDp.dp)
        )
    }
}
