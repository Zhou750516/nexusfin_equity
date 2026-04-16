import { apiRequest } from "@/lib/api";
import type { JointLoginParams, JointLoginResult } from "@/types/loan.types";

export function jointLogin(payload: JointLoginParams) {
  return apiRequest<JointLoginResult>({
    method: "POST",
    url: "/auth/joint-login",
    data: payload,
  });
}
