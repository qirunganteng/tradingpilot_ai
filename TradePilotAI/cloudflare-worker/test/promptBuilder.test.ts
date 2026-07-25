import { describe, it, expect } from "vitest";
import { buildChartAnalysisPrompt, DEFAULT_METHODS } from "../src/promptBuilder";

describe("buildChartAnalysisPrompt", () => {
  it("memakai default methods ketika tidak ada methods diberikan", () => {
    const prompt = buildChartAnalysisPrompt([]);
    for (const method of DEFAULT_METHODS) {
      expect(prompt).toContain(method);
    }
  });

  it("memakai methods custom ketika diberikan", () => {
    const prompt = buildChartAnalysisPrompt(["ICT", "Liquidity"]);
    expect(prompt).toContain("ICT, Liquidity");
    expect(prompt).not.toContain("Momentum");
  });

  it("selalu meminta output JSON dengan field wajib", () => {
    const prompt = buildChartAnalysisPrompt();
    for (const field of ["pair", "trend", "signal", "confidence", "entry", "stop_loss", "take_profit", "risk_reward", "reasoning"]) {
      expect(prompt).toContain(`"${field}"`);
    }
  });

  it("menegaskan AI tidak boleh transaksi", () => {
    const prompt = buildChartAnalysisPrompt();
    expect(prompt.toLowerCase()).toContain("tidak melakukan transaksi");
  });
});
