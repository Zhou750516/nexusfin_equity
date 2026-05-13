import { describe, expect, it } from "vitest";
import { formatCurrency, formatDateYmd } from "./format";

describe("formatCurrency", () => {
  it("always keeps two decimal places for integer yuan amounts", () => {
    expect(formatCurrency(3000, "zh-CN")).toBe("¥3,000.00");
    expect(formatCurrency(3000, "zh-CN", { includeSymbol: false })).toBe("3,000.00");
  });

  it("keeps two decimal places for fractional yuan amounts", () => {
    expect(formatCurrency(1018.5, "zh-CN")).toBe("¥1,018.50");
    expect(formatCurrency(26.5, "en-US", { includeSymbol: false })).toBe("26.50");
  });
});

describe("formatDateYmd", () => {
  it("formats valid date values as YYYY-MM-DD and keeps invalid values unchanged", () => {
    expect(formatDateYmd("2026-06-13")).toBe("2026-06-13");
    expect(formatDateYmd("2026-06-13T08:30:00+08:00")).toBe("2026-06-13");
    expect(formatDateYmd("not-a-date")).toBe("not-a-date");
  });
});
