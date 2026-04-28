import type { JointLoginParams } from "@/types/loan.types";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  JOINT_LOGIN_PARAMS_STORAGE_KEY,
  persistJointLoginParams,
  readJointLoginParams,
  recoverJointLoginSession,
  resolveJointLoginRecoveryPath,
} from "./joint-session";

function createStorage() {
  const store = new Map<string, string>();
  return {
    getItem(key: string) {
      return store.get(key) ?? null;
    },
    setItem(key: string, value: string) {
      store.set(key, value);
    },
    removeItem(key: string) {
      store.delete(key);
    },
    clear() {
      store.clear();
    },
  };
}

describe("joint-session", () => {
  const assign = vi.fn();
  const sessionStorage = createStorage();

  afterEach(() => {
    vi.unstubAllGlobals();
    assign.mockReset();
    sessionStorage.clear();
  });

  it("builds joint-entry recovery path with stored params", () => {
    const params: JointLoginParams = {
      token: "joint-token-001",
      scene: "push",
      benefitOrderNo: "BEN-001",
      orderNo: "ORD-001",
      productCode: "PROD-001",
    };

    expect(resolveJointLoginRecoveryPath(params)).toBe(
      "/joint-entry?token=joint-token-001&scene=push&orderNo=ORD-001&benefitOrderNo=BEN-001&productCode=PROD-001",
    );
  });

  it("persists and reads joint login params from session storage", () => {
    vi.stubGlobal("window", {
      sessionStorage,
      location: { assign },
    });

    const params: JointLoginParams = {
      token: "joint-token-002",
      scene: "refund",
      benefitOrderNo: "BEN-002",
    };

    persistJointLoginParams(params);

    expect(sessionStorage.getItem(JOINT_LOGIN_PARAMS_STORAGE_KEY)).not.toBeNull();
    expect(readJointLoginParams()).toEqual(params);
  });

  it("redirects to joint-entry when recoverable params exist", () => {
    vi.stubGlobal("window", {
      sessionStorage,
      location: { assign },
    });

    sessionStorage.setItem(JOINT_LOGIN_PARAMS_STORAGE_KEY, JSON.stringify({
      token: "joint-token-003",
      scene: "exercise",
      benefitOrderNo: "BEN-003",
    }));

    expect(recoverJointLoginSession()).toBe(true);
    expect(assign).toHaveBeenCalledWith(
      "/joint-entry?token=joint-token-003&scene=exercise&benefitOrderNo=BEN-003",
    );
  });

  it("returns false when no recoverable params exist", () => {
    vi.stubGlobal("window", {
      sessionStorage,
      location: { assign },
    });

    expect(recoverJointLoginSession()).toBe(false);
    expect(assign).not.toHaveBeenCalled();
  });
});
