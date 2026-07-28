package com.tradepilot.data.trading

import com.tradepilot.core.database.entity.TradeHistoryEntity
import com.tradepilot.domain.model.TradeDirection
import com.tradepilot.domain.model.TradeEntry

fun TradeEntry.toEntity(): TradeHistoryEntity = TradeHistoryEntity(
    id = id,
    pair = pair,
    direction = direction.name,
    entry = entry,
    exit = exit,
    sl = stopLoss,
    tp = takeProfit,
    lot = lot,
    profitLoss = profitLoss,
    rr = riskRewardRatio,
    balanceAfter = balanceAfter,
    timestamp = timestampMillis,
    notes = notes
)

fun TradeHistoryEntity.toDomain(): TradeEntry = TradeEntry(
    id = id,
    pair = pair,
    direction = runCatching { TradeDirection.valueOf(direction) }.getOrDefault(TradeDirection.NONE),
    entry = entry,
    exit = exit,
    stopLoss = sl,
    takeProfit = tp,
    lot = lot,
    profitLoss = profitLoss,
    riskRewardRatio = rr,
    balanceAfter = balanceAfter,
    timestampMillis = timestamp,
    notes = notes
)
