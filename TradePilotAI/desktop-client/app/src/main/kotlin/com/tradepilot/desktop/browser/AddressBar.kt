package com.tradepilot.desktop.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.theme.AppColors
import com.tradepilot.desktop.theme.Dimens

/**
 * Address field kompak (Prioritas 4) + tombol bookmark bintang (Prioritas
 * 3 & 10 -- Ctrl+D memicu callback yang sama lewat Workbench).
 *
 * Sengaja pakai [BasicTextField] (bukan OutlinedTextField M3) supaya tinggi
 * benar-benar bisa dipaksa 32dp (Dimens.ADDRESS_FIELD_HEIGHT_DP) -- versi
 * lama pakai OutlinedTextField TANPA height override karena kalau dipaksa
 * .height(40.dp) itu penyebab bug "teks ketutup" yang sudah pernah
 * dilaporkan (OutlinedTextField M3 butuh ~56dp natural). BasicTextField
 * tidak punya masalah itu karena tidak ada label/padding internal M3 yang
 * mengasumsikan tinggi minimum tertentu.
 */
@Composable
fun AddressBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    isBookmarked: Boolean,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null
) {
    Box(
        modifier = modifier
            .height(Dimens.ADDRESS_FIELD_HEIGHT_DP.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(AppColors.SurfaceSunken)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        var fieldModifier = Modifier
            .fillMaxHeight()
            .padding(end = 28.dp) // ruang untuk tombol bookmark di kanan
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    (event.key == Key.Enter || event.key == Key.NumPadEnter)
                ) {
                    onSubmit(value)
                    true
                } else {
                    false
                }
            }
        if (focusRequester != null) {
            fieldModifier = fieldModifier.then(Modifier.focusRequester(focusRequester))
        }

        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = AppColors.TextPrimary, fontSize = MaterialTheme.typography.bodySmall.fontSize),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Accent),
                modifier = fieldModifier,
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            "Ketik URL — mis. youtube.com, github.com...",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.TextDisabled
                        )
                    }
                    inner()
                }
            )
        }

        IconButton(
            onClick = onToggleBookmark,
            modifier = Modifier.align(Alignment.CenterEnd).height(Dimens.ADDRESS_FIELD_HEIGHT_DP.dp)
        ) {
            Icon(
                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = if (isBookmarked) "Hapus bookmark (Ctrl+D)" else "Tambah bookmark (Ctrl+D)",
                tint = if (isBookmarked) AppColors.Accent else AppColors.TextSecondary,
                modifier = Modifier.height(16.dp)
            )
        }
    }
}


