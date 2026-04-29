import { describe, expect, it } from "vitest";
import { messages } from "./messages/index";
import { SUPPORTED_LOCALES, detectBrowserLocale, getInitialLocale, normalizeLocale } from "./locale";

const REQUIRED_RENDERED_KEYS = [
  "calculator.protocolDrawerTitle",
  "calculator.protocolImportant",
  "calculator.protocolImportantBody",
  "calculator.protocol.loanContract",
  "calculator.protocol.privacyAuth",
  "calculator.protocol.userService",
  "calculator.protocol.privacy",
  "calculator.protocol.payment",
  "calculator.protocolAgreeButton",
  "calculator.tipsTitle",
  "calculator.tip1Prefix",
  "calculator.tip1Highlight",
  "calculator.tip1Suffix",
  "calculator.tip2",
  "calculator.tip3",
  "calculator.partnersTitle",
  "calculator.partnersDescription",
  "calculator.partnersFootnote",
  "calculator.partnersAck",
  "calculator.loanPurposeTitle",
  "approvalPending.dismiss.title",
  "approvalPending.dismiss.bullet1Strong",
  "approvalPending.dismiss.bullet1Rest",
  "approvalPending.dismiss.bullet2Prefix",
  "approvalPending.dismiss.bullet2Strong",
  "approvalPending.dismiss.bullet3Prefix",
  "approvalPending.dismiss.bullet3Strong",
  "approvalPending.dismiss.note",
  "approvalPending.dismiss.continue",
  "approvalPending.dismiss.confirm",
  "approvalPending.confirm.title",
  "approvalPending.confirm.payCard",
  "approvalPending.confirm.payCardValue",
  "approvalPending.confirm.amount",
  "approvalPending.confirm.codeLabel",
  "approvalPending.confirm.codePlaceholder",
  "approvalPending.confirm.sendCode",
  "approvalPending.confirm.codeCountdown",
  "approvalPending.confirm.payButton",
  "approvalPending.matching.vipBadge",
  "approvalPending.matching.title",
  "approvalPending.matching.subtitle",
  "approvalPending.matching.bullet1",
  "approvalPending.matching.bullet2",
  "approvalPending.matching.bullet3",
  "approvalResult.amountUnit",
  "approvalResult.tip1",
  "approvalResult.tip2",
  "approvalResult.tip3",
  "approvalResult.tip4",
] as const;

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

  it("keeps rendered calculator and approval keys available across all locales", () => {
    for (const locale of SUPPORTED_LOCALES) {
      const missingKeys = REQUIRED_RENDERED_KEYS.filter((key) => !(key in messages[locale]));
      expect(missingKeys, `locale ${locale} is missing rendered keys`).toEqual([]);
    }
  });
});
