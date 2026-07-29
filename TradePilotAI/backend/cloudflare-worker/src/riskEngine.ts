import type { CalculateRiskRequestBody, RiskRecommendation } from "./types";

/**
 * Port persis dari CalculateRiskUseCase.kt (dulu di :shared, Kotlin).
 * Rumus SAMA PERSIS -- jangan diubah tanpa mengubah versi Kotlin juga
 * kalau suatu saat keduanya perlu tetap ada berdampingan.
 *
 *  riskAmount   = balance * (riskPercent / 100)
 *  slDistance   = |entryPrice - stopLossPrice| dalam pip
 *  lot          = riskAmount / (slDistancePips * pipValuePerLotUsd)
 *  RR           = tpDistancePips / slDistancePips
 *  maxDailyLoss = riskAmount * 3 (default, bisa dikonfigurasi user nanti)
 *  maxTrade     = 3 (default)
 */
export function calculateRisk(body: CalculateRiskRequestBody): RiskRecommendation {
  const { balance, riskPercent, entryPrice, stopLossPrice, takeProfitPrice } = body;
  const pipValuePerLotUsd = body.pipValuePerLotUsd ?? 10.0;
  const pipSize = body.pipSize ?? 0.0001;

  if (!(balance > 0)) {
    throw new RiskValidationError("Balance harus lebih besar dari 0");
  }
  if (!(riskPercent > 0)) {
    throw new RiskValidationError("Risk percent harus lebih besar dari 0");
  }

  const riskAmount = balance * (riskPercent / 100.0);
  const slDistancePips = Math.abs(entryPrice - stopLossPrice) / pipSize;
  const tpDistancePips = Math.abs(takeProfitPrice - entryPrice) / pipSize;

  const lot = slDistancePips > 0 ? riskAmount / (slDistancePips * pipValuePerLotUsd) : 0.0;
  const rr = slDistancePips > 0 ? tpDistancePips / slDistancePips : 0.0;

  return {
    riskPercent,
    lot: roundTo(lot, 2),
    stopLoss: stopLossPrice,
    takeProfit: takeProfitPrice,
    riskRewardRatio: roundTo(rr, 2),
    maxDailyLoss: roundTo(riskAmount * 3, 2),
    maxTrade: 3
  };
}

export class RiskValidationError extends Error {}

function roundTo(value: number, decimals: number): number {
  const factor = Math.pow(10, decimals);
  return Math.round(value * factor) / factor;
}
