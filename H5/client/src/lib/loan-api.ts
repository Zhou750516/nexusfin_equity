import { apiRequest } from "@/lib/api";
import type {
  ActivateParams,
  ActivateResult,
  ApplyParams,
  ApplyResult,
  ApprovalResult,
  ApprovalStatus,
  BenefitsCardDetail,
  CalculateParams,
  CalculateResult,
  CalculatorConfig,
  RepaymentInfo,
  RepaymentSmsConfirmParams,
  RepaymentSmsConfirmResult,
  RepaymentSmsSendParams,
  RepaymentSmsSendResult,
  RepaymentResult,
  RepaymentSubmitResult,
  RepaymentParams,
} from "@/types/loan.types";

export function getCalculatorConfig() {
  return apiRequest<CalculatorConfig>({
    method: "GET",
    url: "/loan/calculator-config",
  });
}

export function calculateLoan(payload: CalculateParams) {
  return apiRequest<CalculateResult>({
    method: "POST",
    url: "/loan/calculate",
    data: payload,
  });
}

export function applyLoan(payload: ApplyParams) {
  return apiRequest<ApplyResult>({
    method: "POST",
    url: "/loan/apply",
    data: payload,
  });
}

export function getApprovalStatus(applicationId: string) {
  return apiRequest<ApprovalStatus>({
    method: "GET",
    url: `/loan/approval-status/${applicationId}`,
  });
}

export function getBenefitsCardDetail() {
  return apiRequest<BenefitsCardDetail>({
    method: "GET",
    url: "/benefits/card-detail",
  });
}

export function activateBenefitsCard(payload: ActivateParams) {
  return apiRequest<ActivateResult>({
    method: "POST",
    url: "/benefits/activate",
    data: payload,
  });
}

export function getApprovalResult(applicationId: string) {
  return apiRequest<ApprovalResult>({
    method: "GET",
    url: `/loan/approval-result/${applicationId}`,
  });
}

export function getRepaymentInfo(loanId: string) {
  return apiRequest<RepaymentInfo>({
    method: "GET",
    url: `/repayment/info/${loanId}`,
  });
}

export function sendRepaymentSms(payload: RepaymentSmsSendParams) {
  return apiRequest<RepaymentSmsSendResult>({
    method: "POST",
    url: "/repayment/sms-send",
    data: payload,
  });
}

export function confirmRepaymentSms(payload: RepaymentSmsConfirmParams) {
  return apiRequest<RepaymentSmsConfirmResult>({
    method: "POST",
    url: "/repayment/sms-confirm",
    data: payload,
  });
}

export function submitRepayment(payload: RepaymentParams) {
  return apiRequest<RepaymentSubmitResult>({
    method: "POST",
    url: "/repayment/submit",
    data: payload,
  });
}

export function getRepaymentResult(repaymentId: string) {
  return apiRequest<RepaymentResult>({
    method: "GET",
    url: `/repayment/result/${repaymentId}`,
  });
}
