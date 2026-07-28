package com.tradepilot.domain.usecase

import com.tradepilot.domain.model.MentorFeedback
import com.tradepilot.domain.model.TradeEntry
import kotlin.math.abs
import javax.inject.Inject

/**
 * AI Mentor (versi 7): evaluasi rule-based sederhana berdasar data trade
 * yang sudah tersimpan. Tidak memanggil Gemini lagi di sini (biar hemat
 * kuota) — murni heuristik dari angka RR/SL/TP yang sudah ada.
 *
 * Bisa di-upgrade nanti untuk memperkaya reasoning lewat AIRepository
 * tanpa mengubah signature UseCase ini.
 */
class GenerateMentorFeedbackUseCase @Inject constructor() {

    operator fun invoke(trade: TradeEntry): MentorFeedback {
        val slDistance = abs(trade.entry - trade.stopLoss)
        val tpDistance = abs(trade.takeProfit - trade.entry)
        val rr = trade.riskRewardRatio

        val whyGood = if (trade.profitLoss > 0 && rr >= 1.5) {
            "Entry menghasilkan profit dengan RR ${rr} — rasio risk/reward sehat."
        } else null

        val whyBad = if (trade.profitLoss < 0 && rr < 1.0) {
            "Trade rugi dengan RR di bawah 1 — potensi reward tidak sebanding risiko yang diambil."
        } else null

        val slTooTight = if (slDistance > 0 && slDistance < tpDistance * 0.2) {
            "Stop Loss tampak sangat dekat dibanding target TP — rawan tersapu noise/spread sebelum harga bergerak sesuai arah."
        } else null

        val tpTooFar = if (rr > 5.0) {
            "Take Profit sangat jauh (RR > 5) — peluang tersentuh lebih kecil, pertimbangkan TP bertahap (partial close)."
        } else null

        val betterEntry = if (trade.profitLoss < 0) {
            "Evaluasi ulang konfirmasi entry (BOS/CHOCH/Order Block) sebelum masuk agar tidak entry di tengah pergerakan yang belum terkonfirmasi."
        } else null

        return MentorFeedback(
            whyGood = whyGood,
            whyBad = whyBad,
            slTooTight = slTooTight,
            tpTooFar = tpTooFar,
            betterEntrySuggestion = betterEntry
        )
    }
}
