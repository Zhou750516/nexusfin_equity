import { buildPath } from "@/lib/route";
import type { JointLoginParams } from "@/types/loan.types";

export const JOINT_LOGIN_PARAMS_STORAGE_KEY = "nexusfin.h5.joint-login.params";

export function persistJointLoginParams(params: JointLoginParams) {
  if (typeof window === "undefined") {
    return;
  }
  window.sessionStorage.setItem(JOINT_LOGIN_PARAMS_STORAGE_KEY, JSON.stringify(params));
}

export function readJointLoginParams(): JointLoginParams | null {
  if (typeof window === "undefined") {
    return null;
  }
  const rawValue = window.sessionStorage.getItem(JOINT_LOGIN_PARAMS_STORAGE_KEY);
  if (!rawValue) {
    return null;
  }
  return JSON.parse(rawValue) as JointLoginParams;
}

export function resolveJointLoginRecoveryPath(params: JointLoginParams): string {
  return buildPath("/joint-entry", {
    token: params.token,
    scene: params.scene,
    orderNo: params.orderNo,
    benefitOrderNo: params.benefitOrderNo,
    productCode: params.productCode,
  });
}

export function recoverJointLoginSession(): boolean {
  if (typeof window === "undefined") {
    return false;
  }
  const params = readJointLoginParams();
  if (!params) {
    return false;
  }
  window.location.assign(resolveJointLoginRecoveryPath(params));
  return true;
}
