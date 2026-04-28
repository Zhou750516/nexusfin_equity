import { buildPath } from "@/lib/route";
import type { JointLoginParams, JointLoginResult, JointLoginScene } from "@/types/loan.types";

const SUPPORTED_SCENES: JointLoginScene[] = ["push", "exercise", "refund"];

function normalizeOptionalParam(value: string | null) {
  if (value == null) {
    return undefined;
  }
  const normalized = value.trim();
  return normalized === "" ? undefined : normalized;
}

function normalizeScene(value: string | null): JointLoginScene | null {
  const normalized = value?.trim().toLowerCase();
  if (!normalized) {
    return null;
  }
  return SUPPORTED_SCENES.find((scene) => scene === normalized) ?? null;
}

export function parseJointLoginParams(search: string): JointLoginParams | null {
  const searchParams = new URLSearchParams(search);
  const token = searchParams.get("token")?.trim();
  const scene = normalizeScene(searchParams.get("scene"));
  if (!token || !scene) {
    return null;
  }
  return {
    token,
    scene,
    orderNo: normalizeOptionalParam(searchParams.get("orderNo")),
    benefitOrderNo: normalizeOptionalParam(searchParams.get("benefitOrderNo")),
    productCode: normalizeOptionalParam(searchParams.get("productCode")),
  };
}

export function resolveJointEntryTarget(result: JointLoginResult) {
  const params = {
    scene: result.scene,
    benefitOrderNo: result.benefitOrderNo,
  };

  if (result.targetPage === "joint-refund-entry") {
    return buildPath("/joint-refund-entry", params);
  }

  if (result.targetPage === "joint-dispatch") {
    return buildPath("/joint-dispatch", params);
  }

  return "/joint-unsupported";
}

export function resolveJointEntryErrorKey(message: string): "jointEntry.sessionExpired" | "jointEntry.systemBusy" {
  if (message.includes("JOINT_LOGIN_TOKEN_INVALID")) {
    return "jointEntry.sessionExpired";
  }

  return "jointEntry.systemBusy";
}
