import { buildPath } from "@/lib/route";
import type { JointLoginResult } from "@/types/loan.types";

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

  return "/404";
}
