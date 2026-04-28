import { describe, expect, it } from "vitest";
import {
  canProceedRepaymentAction,
  resolveRepaymentActionStage,
  resolveSelectedRepaymentCardId,
} from "./confirm-repayment.logic";

describe("confirm repayment sms flow", () => {
  it("requires sending sms before repayment submit when sms verification is required", () => {
    expect(resolveRepaymentActionStage({
      hasBankCard: true,
      smsRequired: true,
      smsSent: false,
      smsVerified: false,
      captcha: "",
    })).toBe("send_sms");
  });

  it("requires captcha confirmation after sms has been sent", () => {
    expect(resolveRepaymentActionStage({
      hasBankCard: true,
      smsRequired: true,
      smsSent: true,
      smsVerified: false,
      captcha: "",
    })).toBe("confirm_sms");
  });

  it("allows direct submit after sms has been verified", () => {
    expect(resolveRepaymentActionStage({
      hasBankCard: true,
      smsRequired: true,
      smsSent: true,
      smsVerified: true,
      captcha: "123456",
    })).toBe("submit");
  });

  it("blocks captcha confirmation until the user enters a code", () => {
    expect(canProceedRepaymentAction({
      hasBankCard: true,
      smsRequired: true,
      smsSent: true,
      smsVerified: false,
      captcha: "",
    })).toBe(false);

    expect(canProceedRepaymentAction({
      hasBankCard: true,
      smsRequired: true,
      smsSent: true,
      smsVerified: false,
      captcha: "123456",
    })).toBe(true);
  });

  it("allows direct submit without sms verification when the backend marks it optional", () => {
    expect(resolveRepaymentActionStage({
      hasBankCard: true,
      smsRequired: false,
      smsSent: false,
      smsVerified: false,
      captcha: "",
    })).toBe("submit");
  });

  it("keeps the user's selected repayment card when it still exists in the latest card list", () => {
    expect(resolveSelectedRepaymentCardId(
      "card-002",
      "card-001",
      [{ accountId: "card-001" }, { accountId: "card-002" }],
    )).toBe("card-002");
  });

  it("falls back to the backend-selected card when the user's previous choice is unavailable", () => {
    expect(resolveSelectedRepaymentCardId(
      "card-999",
      "card-001",
      [{ accountId: "card-001" }, { accountId: "card-002" }],
    )).toBe("card-001");
  });

  it("falls back to the first available card when no prior selection exists", () => {
    expect(resolveSelectedRepaymentCardId(
      null,
      undefined,
      [{ accountId: "card-001" }, { accountId: "card-002" }],
    )).toBe("card-001");
  });
});
