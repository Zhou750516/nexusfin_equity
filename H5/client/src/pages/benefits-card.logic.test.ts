import { describe, expect, it, vi } from "vitest";
import {
  applyBenefitsSign,
  canActivateBenefits,
  canStartBenefitsActivation,
  checkBenefitsActivationSignGate,
  confirmBenefitsSignAndActivate,
  resolveDefaultBenefitsUserCard,
} from "./benefits-card.logic";

describe("benefits card page logic", () => {
  it("prefers the backend default card for the deduction summary", () => {
    expect(resolveDefaultBenefitsUserCard([
      { cardId: "card-001", defaultCard: false },
      { cardId: "card-002", defaultCard: true },
    ])?.cardId).toBe("card-002");
  });

  it("falls back to the first card when no backend default is marked", () => {
    expect(resolveDefaultBenefitsUserCard([
      { cardId: "card-001", defaultCard: false },
      { cardId: "card-002", defaultCard: false },
    ])?.cardId).toBe("card-001");
  });

  it("blocks activation when the application id is missing", () => {
    expect(canActivateBenefits({
      applicationId: null,
      protocolReady: true,
      hasJointLoginToken: true,
    })).toBe(false);
  });

  it("blocks activation when protocol readiness is not satisfied", () => {
    expect(canActivateBenefits({
      applicationId: "APP-001",
      protocolReady: false,
      hasJointLoginToken: true,
    })).toBe(false);
  });

  it("blocks activation when the joint-login token is missing", () => {
    expect(canActivateBenefits({
      applicationId: "APP-001",
      protocolReady: true,
      hasJointLoginToken: false,
    })).toBe(false);
  });

  it("allows activation only when application id, protocol readiness, and joint-login token are all present", () => {
    expect(canActivateBenefits({
      applicationId: "APP-001",
      protocolReady: true,
      hasJointLoginToken: true,
    })).toBe(true);
  });

  it("allows starting activation sign gate without protocol readiness", () => {
    expect(canStartBenefitsActivation({
      applicationId: "APP-001",
      hasJointLoginToken: true,
    })).toBe(true);
  });

  it("does not start activation sign gate without application id", () => {
    expect(canStartBenefitsActivation({
      applicationId: null,
      hasJointLoginToken: true,
    })).toBe(false);
  });

  it("returns no-card when no default bank card is available", async () => {
    const getSignStatus = vi.fn();

    const result = await checkBenefitsActivationSignGate({
      userCard: null,
      getSignStatus,
    });

    expect(result.type).toBe("no-card");
    expect(getSignStatus).not.toHaveBeenCalled();
  });

  it("allows direct activation when the default card is already signed", async () => {
    const getSignStatus = vi.fn().mockResolvedValue({
      accountNo: "card-001",
      signed: true,
      status: "SIGNED",
    });

    const result = await checkBenefitsActivationSignGate({
      userCard: {
        cardId: "card-001",
        bankName: "招商银行",
        cardLastFour: "8119",
        defaultCard: true,
      },
      getSignStatus,
    });

    expect(result).toEqual({ type: "activate" });
    expect(getSignStatus).toHaveBeenCalledWith("card-001");
  });

  it("opens the sign dialog without activating when the default card is not signed", async () => {
    const getSignStatus = vi.fn().mockResolvedValue({
      accountNo: "card-001",
      signed: false,
      status: "UNSIGNED",
    });

    const result = await checkBenefitsActivationSignGate({
      userCard: {
        cardId: "card-001",
        bankName: "招商银行",
        cardLastFour: "8119",
        defaultCard: true,
      },
      getSignStatus,
    });

    expect(result).toEqual({
      type: "sign-required",
      accountNo: "card-001",
      bankName: "招商银行",
      maskedCardNo: "**** 8119",
    });
  });

  it("stores userSignId after sign apply succeeds", async () => {
    const applySign = vi.fn().mockResolvedValue({
      userSignId: 88001234,
      applyTime: "2026-05-19 18:00:00",
      status: "SMS_SENT",
    });

    const result = await applyBenefitsSign({
      accountNo: "card-001",
      applySign,
    });

    expect(result).toEqual({
      userSignId: 88001234,
      applyTime: "2026-05-19 18:00:00",
      status: "SMS_SENT",
    });
    expect(applySign).toHaveBeenCalledWith({ accountNo: "card-001" });
  });

  it("confirms sign and continues activation when sign confirm succeeds", async () => {
    const confirmSign = vi.fn().mockResolvedValue({
      userSignId: 88001234,
      agreementNo: "AGRM-001",
      signed: true,
      status: "SIGNED",
    });
    const activate = vi.fn().mockResolvedValue(undefined);

    const result = await confirmBenefitsSignAndActivate({
      userSignId: 88001234,
      verificationCode: "123456",
      confirmSign,
      activate,
    });

    expect(result.type).toBe("activated");
    expect(confirmSign).toHaveBeenCalledWith({
      userSignId: 88001234,
      verificationCode: "123456",
    });
    expect(activate).toHaveBeenCalledTimes(1);
  });

  it("does not activate when sign confirm fails", async () => {
    const confirmSign = vi.fn().mockRejectedValue(new Error("验证码错误"));
    const activate = vi.fn();

    await expect(confirmBenefitsSignAndActivate({
      userSignId: 88001234,
      verificationCode: "000000",
      confirmSign,
      activate,
    })).rejects.toThrow("验证码错误");

    expect(activate).not.toHaveBeenCalled();
  });
});
