import { describe, it, expect } from "vitest";
import { calculateRisk, RiskValidationError } from "../src/riskEngine";
import type { CalculateRiskRequestBody } from "../src/types";

const baseBody: CalculateRiskRequestBody = {
  balance: 1000,
  riskPercent: 1,
  entryPrice: 1.1000,
  stopLossPrice: 1.0950,
  takeProfitPrice: 1.1100,
  deviceId: "test-device"
};

describe("riskEngine.calculateRisk", () => {
  it("menghitung lot dan RR sesuai rumus dasar (parity dengan CalculateRiskUseCase.kt)", () => {
    const result = calculateRisk(baseBody);

    // riskAmount = 1000 * 1% = 10
    // slDistancePips = |1.1000 - 1.0950| / 0.0001 = 50
    // lot = 10 / (50 * 10) = 0.02
    expect(result.lot).toBeCloseTo(0.02, 5);

    // tpDistancePips = |1.1100 - 1.1000| / 0.0001 = 100
    // RR = 100 / 50 = 2
    expect(result.riskRewardRatio).toBeCloseTo(2, 5);

    expect(result.maxDailyLoss).toBeCloseTo(30, 5); // riskAmount * 3
    expect(result.maxTrade).toBe(3);
    expect(result.stopLoss).toBe(baseBody.stopLossPrice);
    expect(result.takeProfit).toBe(baseBody.takeProfitPrice);
  });

  it("menghormati pipValuePerLotUsd dan pipSize custom (mis. XAUUSD)", () => {
    const result = calculateRisk({
      ...baseBody,
      entryPrice: 2000,
      stopLossPrice: 1990,
      takeProfitPrice: 2020,
      pipSize: 0.1,
      pipValuePerLotUsd: 1
    });

    // slDistancePips = 10 / 0.1 = 100 ; lot = 10 / (100 * 1) = 0.1
    expect(result.lot).toBeCloseTo(0.1, 5);
    // tpDistancePips = 20 / 0.1 = 200 ; RR = 200 / 100 = 2
    expect(result.riskRewardRatio).toBeCloseTo(2, 5);
  });

  it("lot dan RR = 0 kalau SL sama persis dengan entry (hindari div by zero)", () => {
    const result = calculateRisk({ ...baseBody, stopLossPrice: baseBody.entryPrice });
    expect(result.lot).toBe(0);
    expect(result.riskRewardRatio).toBe(0);
  });

  it("menolak balance <= 0", () => {
    expect(() => calculateRisk({ ...baseBody, balance: 0 })).toThrow(RiskValidationError);
    expect(() => calculateRisk({ ...baseBody, balance: -100 })).toThrow(RiskValidationError);
  });

  it("menolak riskPercent <= 0", () => {
    expect(() => calculateRisk({ ...baseBody, riskPercent: 0 })).toThrow(RiskValidationError);
  });

  it("membulatkan lot dan RR ke 2 desimal", () => {
    const result = calculateRisk({
      ...baseBody,
      entryPrice: 1.10001,
      stopLossPrice: 1.09973,
      takeProfitPrice: 1.10222
    });
    expect(Number.isFinite(result.lot)).toBe(true);
    expect(result.lot.toString().split(".")[1]?.length ?? 0).toBeLessThanOrEqual(2);
  });
});
