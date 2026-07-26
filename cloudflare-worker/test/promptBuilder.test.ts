import { describe, it, expect } from "vitest";
import { buildChartAnalysisPrompt, DEFAULT_METHODS } from "../src/promptBuilder";

describe("promptBuilder", () => {
  it("should use default methods if empty array provided", () => {
    const prompt = buildChartAnalysisPrompt([]);
    expect(prompt).toContain("ICT");
    expect(prompt).toContain("SMC");
    expect(prompt).toContain("JSON");
  });

  it("should include custom methods if provided", () => {
    const prompt = buildChartAnalysisPrompt(["Fibonacci", "EMA200"]);
    expect(prompt).toContain("Fibonacci, EMA200");
  });
});
