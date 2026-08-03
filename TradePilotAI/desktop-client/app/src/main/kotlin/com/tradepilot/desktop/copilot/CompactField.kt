package com.tradepilot.desktop.copilot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.tradepilot.desktop.theme.AppColors
import com.tradepilot.desktop.theme.Dimens

/**
 * Field input compact bergaya sama seperti address bar (BasicTextField
 * custom, BUKAN OutlinedTextField M3 -- lihat catatan panjang soal alasannya
 * di AddressBar.kt & CopilotPanel.kt: OutlinedTextField M3 tidak bisa
 * benar-benar dipaksa kecil lewat heightIn(min=), cuma BasicTextField custom
 * yang bisa).
 *
 * Diekstrak dari CopilotPanel.kt (dulu private `RiskField`, cuma dipakai di
 * situ) supaya bisa dipakai ulang di TradingJournalPanel.kt tanpa duplikasi
 * kode (form entri jurnal butuh field yang sama persis: Pair, Entry, SL, TP,
 * Lot).
 */
/**
 * BUG DITEMUKAN & DIPERBAIKI (audit build FASE 1): dulu `Spacer` di-import
 * dari `androidx.compose.runtime` (unresolved -- Spacer itu Composable di
 * `androidx.compose.foundation.layout`, bukan bagian dari runtime), dan
 * `modifier` diletakkan SETELAH `onChange`. Kotlin cuma boleh pakai trailing
 * lambda syntax (`CompactField("Label", value) { value = it }`, dipakai di
 * SEMUA pemanggilnya di CopilotPanel.kt & TradingJournalPanel.kt) kalau
 * parameter function-type ada di posisi PALING TERAKHIR -- karena dulu
 * `modifier: Modifier` ada di belakang `onChange`, compiler malah mencoba
 * mencocokkan lambda itu ke `modifier` (makanya errornya "Argument type
 * mismatch: ... Modifier was expected" + "No value passed for parameter
 * 'onChange'" + "Unresolved reference 'it'" di SETIAP pemanggilan). Fix:
 * `modifier` dipindah ke sebelum `onChange` (pola standar Compose -- lihat
 * juga alasan sama di AddressBar.kt).
 */
@Composable
fun CompactField(label: String, value: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.AI_PANEL_FIELD_HEIGHT_DP.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.SurfaceSunken)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = AppColors.TextPrimary, fontSize = MaterialTheme.typography.bodySmall.fontSize),
                cursorBrush = SolidColor(AppColors.Accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
