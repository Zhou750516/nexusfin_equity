import { describe, expect, it } from "vitest";
import {
  buildConfirmRepaymentCleanPath,
  buildRepaymentSuccessPath,
  canProceedRepaymentAction,
  parseConfirmRepaymentEntry,
  resolveDefaultRepaymentSubmitType,
  resolveRepaymentInfoUrl,
  resolveRepaymentActionStage,
  resolveRepaymentUnavailableFeedback,
  resolveSelectedRepaymentCardId,
  shouldRunRepaymentLogin,
  shouldNavigateAfterRepaymentSubmit,
  shouldShowRepaymentSmsSection,
} from "./confirm-repayment.logic";

describe("confirm repayment page entry", () => {
  it("does not build a backend request url when loanId is missing", () => {
    expect(resolveRepaymentInfoUrl(null)).toBeNull();
    expect(resolveRepaymentInfoUrl(undefined)).toBeNull();
  });

  it("builds the repayment info backend url when loanId is valid", () => {
    expect(resolveRepaymentInfoUrl(20260501)).toBe("/repayment/info/20260501");
  });

  it("parses repayment deep link token and loanId", () => {
    expect(parseConfirmRepaymentEntry("?token=joint-token-001&loanId=1781594032")).toEqual({
      token: "joint-token-001",
      loanId: 1781594032,
    });
  });

  it("requires repayment login only when token and loanId are both present", () => {
    expect(shouldRunRepaymentLogin({ token: "joint-token-001", loanId: 1781594032 })).toBe(true);
    expect(shouldRunRepaymentLogin({ token: "joint-token-001", loanId: null })).toBe(false);
    expect(shouldRunRepaymentLogin({ token: null, loanId: 1781594032 })).toBe(false);
  });

  it("builds a clean confirm repayment path without token after repayment login", () => {
    expect(buildConfirmRepaymentCleanPath(1781594032)).toBe("/confirm-repayment?loanId=1781594032");
  });

  it("builds repayment success path with repaymentId and submitted amount", () => {
    expect(buildRepaymentSuccessPath("xhqbapi20260618181657154625", 1040.26))
      .toBe("/repayment-success?repaymentId=xhqbapi20260618181657154625&amount=1040.26");
  });

  it("defaults repayment submit type to scheduled for current due repayment", () => {
    expect(resolveDefaultRepaymentSubmitType()).toBe("scheduled");
  });

  it("uses a friendly unavailable copy for repayment info failures", () => {
    expect(resolveRepaymentUnavailableFeedback()).toEqual({
      titleKey: "repaymentConfirm.unavailableTitle",
      messageKey: "repaymentConfirm.unavailable",
      retryKey: "common.retry",
    });
  });
});

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

  it("does not show the sms section when backend marks sms as optional", () => {
    expect(shouldShowRepaymentSmsSection({ smsRequired: false })).toBe(false);
  });

  it("keeps the sms section available when backend requires sms", () => {
    expect(shouldShowRepaymentSmsSection({ smsRequired: true })).toBe(true);
  });

  it("does not navigate to success page when repayment submit returns failed", () => {
    expect(shouldNavigateAfterRepaymentSubmit({ status: "failed" })).toBe(false);
    expect(shouldNavigateAfterRepaymentSubmit({ status: "processing" })).toBe(true);
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
