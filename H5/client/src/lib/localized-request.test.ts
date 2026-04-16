import { describe, expect, it } from "vitest";
import { shouldRequestLocalizedData } from "./localized-request";

describe("shouldRequestLocalizedData", () => {
  it("requests localized data on first load when required key exists", () => {
    expect(
      shouldRequestLocalizedData({
        locale: "en-US",
        loadedLocale: null,
        requestKey: "APP-1001",
      }),
    ).toBe(true);
  });

  it("skips localized request when required key is missing", () => {
    expect(
      shouldRequestLocalizedData({
        locale: "en-US",
        loadedLocale: null,
        requestKey: null,
      }),
    ).toBe(false);
  });

  it("skips refetch when current locale is already loaded", () => {
    expect(
      shouldRequestLocalizedData({
        locale: "zh-CN",
        loadedLocale: "zh-CN",
        requestKey: "APP-1001",
        loadedRequestKey: "APP-1001",
      }),
    ).toBe(false);
  });

  it("refetches localized data when locale changes", () => {
    expect(
      shouldRequestLocalizedData({
        locale: "vi-VN",
        loadedLocale: "en-US",
        requestKey: "APP-1001",
      }),
    ).toBe(true);
  });

  it("refetches localized data when request key changes in the same locale", () => {
    expect(
      shouldRequestLocalizedData({
        locale: "en-US",
        loadedLocale: "en-US",
        requestKey: "APP-1002",
        loadedRequestKey: "APP-1001",
      }),
    ).toBe(true);
  });

  it("supports locale-based refetch for pages without required keys", () => {
    expect(
      shouldRequestLocalizedData({
        locale: "zh-TW",
        loadedLocale: "en-US",
      }),
    ).toBe(true);

    expect(
      shouldRequestLocalizedData({
        locale: "zh-TW",
        loadedLocale: "zh-TW",
      }),
    ).toBe(false);
  });
});
