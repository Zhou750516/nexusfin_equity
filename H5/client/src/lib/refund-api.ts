import { apiRequest } from "@/lib/api";

export interface RefundInfoResult {
  benefitOrderNo: string;
  refundable: boolean;
  refundStatus: string;
  refundableAmount: number;
  message: string;
}

export function getRefundInfo(benefitOrderNo: string) {
  return apiRequest<RefundInfoResult>({
    method: "GET",
    url: `/refund/info/${benefitOrderNo}`,
  });
}
