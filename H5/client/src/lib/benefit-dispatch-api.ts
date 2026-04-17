import { apiRequest } from "@/lib/api";

export interface BenefitDispatchContextResult {
  benefitOrderNo: string;
  scene: string;
  orderStatus: string;
  allowRedirect: boolean;
  redirectMode: string;
  message: string;
}

export interface BenefitDispatchResolveResult {
  benefitOrderNo: string;
  allowRedirect: boolean;
  redirectMode: "DIRECT" | "INTERMEDIATE";
  supplierUrl: string | null;
  message: string;
}

export function getBenefitDispatchContext(benefitOrderNo: string) {
  return apiRequest<BenefitDispatchContextResult>({
    method: "GET",
    url: `/benefit-dispatch/context/${benefitOrderNo}`,
  });
}

export function resolveBenefitDispatch(benefitOrderNo: string) {
  return apiRequest<BenefitDispatchResolveResult>({
    method: "POST",
    url: "/benefit-dispatch/resolve",
    data: { benefitOrderNo },
  });
}
