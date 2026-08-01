package com.tradepilot.desktop.updater

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.theme.AppColors

/**
 * Overlay non-intrusive di pojok kanan-atas Workspace (lihat pemasangannya
 * di layout/Workbench.kt) untuk auto-updater (Level 2).
 *
 * Sengaja HANYA tampil untuk state Downloading / ReadyToInstall / Failed --
 * state Idle & Checking TIDAK menampilkan apa pun sama sekali, karena cek +
 * download di background itu memang tidak perlu diketahui user selama
 * belum ada keputusan yang perlu diambil (sesuai keputusan produk: jangan
 * ganggu user, restart tetap wajib konfirmasi).
 */
@Composable
fun UpdateBanner(modifier: Modifier = Modifier) {
    var isDismissed by remember { mutableStateOf(false) }
    val state = UpdateManager.state

    // Reset dismiss setiap kali muncul update BARU (SHA beda dari terakhir
    // kali user klik "Nanti"/"Tutup") -- supaya dismiss satu update tidak
    // permanen membisukan banner untuk update berikutnya.
    var lastSeenSha by remember { mutableStateOf<String?>(null) }
    val currentSha = (state as? UpdateState.ReadyToInstall)?.manifest?.commitSha
    LaunchedEffect(currentSha) {
        if (currentSha != null && currentSha != lastSeenSha) {
            isDismissed = false
            lastSeenSha = currentSha
        }
    }

    val isVisible = !isDismissed && (state is UpdateState.Downloading || state is UpdateState.ReadyToInstall || state is UpdateState.Failed)

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp, end = 12.dp)
                .widthIn(min = 260.dp, max = 320.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppColors.SurfaceRaised)
                .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            when (val s = state) {
                is UpdateState.Downloading -> DownloadingContent(s)
                is UpdateState.ReadyToInstall -> ReadyToInstallContent(onDismiss = { isDismissed = true })
                is UpdateState.Failed -> FailedContent(s, onDismiss = { isDismissed = true })
                else -> Unit
            }
        }
    }
}

@Composable
private fun DownloadingContent(state: UpdateState.Downloading) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val progress = if (state.totalBytes > 0) {
            (state.downloadedBytes.toFloat() / state.totalBytes.toFloat()).coerceIn(0f, 1f)
        } else null

        if (progress != null) {
            CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AppColors.Accent)
        } else {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = AppColors.Accent)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("Mengunduh pembaruan…", style = MaterialTheme.typography.labelMedium, color = AppColors.TextPrimary)
            if (progress != null) {
                Text(
                    "${(progress * 100).toInt()}% (${formatBytes(state.downloadedBytes)} / ${formatBytes(state.totalBytes)})",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.TextSecondary
                )
            } else {
                Text(formatBytes(state.downloadedBytes), style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun ReadyToInstallContent(onDismiss: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppColors.Success, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pembaruan siap dipasang", style = MaterialTheme.typography.labelMedium, color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = AppColors.TextSecondary, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "App akan ditutup sebentar lalu terbuka lagi otomatis dengan versi baru.",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextSecondary
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { UpdateManager.skipThisVersion() }) {
                Text("Lewati versi ini", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.width(4.dp))
            TextButton(onClick = { UpdateManager.installAndRestart() }) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Restart Sekarang", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun FailedContent(state: UpdateState.Failed, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = AppColors.Danger, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Update gagal", style = MaterialTheme.typography.labelMedium, color = AppColors.TextPrimary)
            Text(state.reason, style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = AppColors.TextSecondary, modifier = Modifier.size(14.dp))
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "?"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1) "%.1f MB".format(mb) else "${bytes / 1024} KB"
}
