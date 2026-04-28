import type { Locale } from "../locale";
import approvalMessages from "./approval";
import benefitsMessages from "./benefits";
import calculatorMessages from "./calculator";
import commonMessages from "./common";
import jointMessages from "./joint";
import repaymentMessages from "./repayment";

export type MessageDictionary = Record<string, string>;

export const LOCALE_LABELS: Record<Locale, string> = {
  "zh-CN": "简中",
  "zh-TW": "繁中",
  "en-US": "EN",
  "vi-VN": "VI"
};

export const messages: Record<Locale, MessageDictionary> = {
  "zh-CN": {
    ...commonMessages["zh-CN"],
    ...calculatorMessages["zh-CN"],
    ...approvalMessages["zh-CN"],
    ...benefitsMessages["zh-CN"],
    ...repaymentMessages["zh-CN"],
    ...jointMessages["zh-CN"],
  },
  "zh-TW": {
    ...commonMessages["zh-TW"],
    ...calculatorMessages["zh-TW"],
    ...approvalMessages["zh-TW"],
    ...benefitsMessages["zh-TW"],
    ...repaymentMessages["zh-TW"],
    ...jointMessages["zh-TW"],
  },
  "en-US": {
    ...commonMessages["en-US"],
    ...calculatorMessages["en-US"],
    ...approvalMessages["en-US"],
    ...benefitsMessages["en-US"],
    ...repaymentMessages["en-US"],
    ...jointMessages["en-US"],
  },
  "vi-VN": {
    ...commonMessages["vi-VN"],
    ...calculatorMessages["vi-VN"],
    ...approvalMessages["vi-VN"],
    ...benefitsMessages["vi-VN"],
    ...repaymentMessages["vi-VN"],
    ...jointMessages["vi-VN"],
  },
};
