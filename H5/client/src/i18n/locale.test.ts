import { describe, expect, it } from "vitest";
import { messages } from "./messages/index";
import { SUPPORTED_LOCALES, detectBrowserLocale, getInitialLocale, normalizeLocale } from "./locale";

describe("i18n locale utilities", () => {
  it("normalizes supported browser language variants", () => {
    expect(normalizeLocale("zh-Hans")).toBe("zh-CN");
    expect(normalizeLocale("zh-HK")).toBe("zh-TW");
    expect(normalizeLocale("en-GB")).toBe("en-US");
    expect(normalizeLocale("vi")).toBe("vi-VN");
  });

  it("falls back to zh-CN for unsupported languages", () => {
    expect(normalizeLocale("fr-FR")).toBe("zh-CN");
    expect(detectBrowserLocale(["ja-JP"])).toBe("zh-CN");
  });

  it("prefers manually persisted locale over browser locale", () => {
    const storage = { getItem: () => "vi-VN" };
    expect(getInitialLocale(storage, ["en-US"])).toBe("vi-VN");
  });

  it("keeps shared message keys available across all locales", () => {
    for (const locale of SUPPORTED_LOCALES) {
      expect(messages[locale]["calculator.submit"]).toBeTruthy();
      expect(messages[locale]["approvalPending.title"]).toBeTruthy();
      expect(messages[locale]["benefits.openNow"]).toBeTruthy();
      expect(messages[locale]["repaymentSuccess.title"]).toBeTruthy();
      expect(messages[locale]["jointEntry.title"]).toBeTruthy();
    }
  });
});
