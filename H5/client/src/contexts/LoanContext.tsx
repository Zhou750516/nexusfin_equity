import { isLoanPurpose } from "@/lib/loan-purpose";
import type { LoanPurpose } from "@/types/loan.types";
import { createContext, ReactNode, useContext, useEffect, useMemo, useState } from "react";

export type LoanFlowStatus = "idle" | "pending" | "reviewing" | "approved" | "rejected" | "loan_failed";

export interface LoanState {
  amount: number;
  term: number;
  purpose: LoanPurpose | null;
  applicationId: string | null;
  approvalStatus: LoanFlowStatus;
  approvalMessage: string | null;
  benefitsCardActivated: boolean;
  benefitOrderNo: string | null;
  loanId: string | null;
  repaymentId: string | null;
  receivingAccountId: string | null;
}

interface LoanContextValue extends LoanState {
  setAmount: (amount: number) => void;
  setTerm: (term: number) => void;
  setPurpose: (purpose: LoanPurpose | null) => void;
  setApplicationId: (applicationId: string | null) => void;
  setApprovalStatus: (status: LoanFlowStatus) => void;
  setApprovalMessage: (message: string | null) => void;
  setBenefitsCardActivated: (activated: boolean) => void;
  setBenefitOrderNo: (benefitOrderNo: string | null) => void;
  setLoanId: (loanId: string | null) => void;
  setRepaymentId: (repaymentId: string | null) => void;
  setReceivingAccountId: (receivingAccountId: string | null) => void;
  reset: () => void;
}

const STORAGE_KEY = "nexusfin.h5.loan-state";

const DEFAULT_STATE: LoanState = {
  amount: 3000,
  term: 3,
  purpose: null,
  applicationId: null,
  approvalStatus: "idle",
  approvalMessage: null,
  benefitsCardActivated: false,
  benefitOrderNo: null,
  loanId: null,
  repaymentId: null,
  receivingAccountId: null,
};

const LoanContext = createContext<LoanContextValue | null>(null);

function readStoredState(): LoanState {
  if (typeof window === "undefined") {
    return DEFAULT_STATE;
  }

  try {
    const rawValue = window.sessionStorage.getItem(STORAGE_KEY);
    if (!rawValue) {
      return DEFAULT_STATE;
    }

    const parsed = JSON.parse(rawValue) as Partial<LoanState>;
    return {
      amount: typeof parsed.amount === "number" ? parsed.amount : DEFAULT_STATE.amount,
      term: typeof parsed.term === "number" ? parsed.term : DEFAULT_STATE.term,
      purpose: isLoanPurpose(parsed.purpose) ? parsed.purpose : null,
      applicationId: typeof parsed.applicationId === "string" ? parsed.applicationId : null,
      approvalStatus: isLoanFlowStatus(parsed.approvalStatus) ? parsed.approvalStatus : DEFAULT_STATE.approvalStatus,
      approvalMessage: typeof parsed.approvalMessage === "string" ? parsed.approvalMessage : null,
      benefitsCardActivated: typeof parsed.benefitsCardActivated === "boolean" ? parsed.benefitsCardActivated : false,
      benefitOrderNo: typeof parsed.benefitOrderNo === "string" ? parsed.benefitOrderNo : null,
      loanId: typeof parsed.loanId === "string" ? parsed.loanId : null,
      repaymentId: typeof parsed.repaymentId === "string" ? parsed.repaymentId : null,
      receivingAccountId: typeof parsed.receivingAccountId === "string" ? parsed.receivingAccountId : null,
    };
  } catch {
    return DEFAULT_STATE;
  }
}

function isLoanFlowStatus(value: unknown): value is LoanFlowStatus {
  return value === "idle"
    || value === "pending"
    || value === "reviewing"
    || value === "approved"
    || value === "rejected"
    || value === "loan_failed";
}

interface LoanProviderProps {
  children: ReactNode;
}

export function LoanProvider({ children }: LoanProviderProps) {
  const [state, setState] = useState<LoanState>(() => readStoredState());

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  }, [state]);

  const value = useMemo<LoanContextValue>(() => ({
    ...state,
    setAmount: (amount) => setState((previous) => ({ ...previous, amount })),
    setTerm: (term) => setState((previous) => ({ ...previous, term })),
    setPurpose: (purpose) => setState((previous) => ({ ...previous, purpose })),
    setApplicationId: (applicationId) => setState((previous) => ({ ...previous, applicationId })),
    setApprovalStatus: (approvalStatus) => setState((previous) => ({ ...previous, approvalStatus })),
    setApprovalMessage: (approvalMessage) => setState((previous) => ({ ...previous, approvalMessage })),
    setBenefitsCardActivated: (benefitsCardActivated) => setState((previous) => ({ ...previous, benefitsCardActivated })),
    setBenefitOrderNo: (benefitOrderNo) => setState((previous) => ({ ...previous, benefitOrderNo })),
    setLoanId: (loanId) => setState((previous) => ({ ...previous, loanId })),
    setRepaymentId: (repaymentId) => setState((previous) => ({ ...previous, repaymentId })),
    setReceivingAccountId: (receivingAccountId) => setState((previous) => ({ ...previous, receivingAccountId })),
    reset: () => setState(DEFAULT_STATE),
  }), [state]);

  return <LoanContext.Provider value={value}>{children}</LoanContext.Provider>;
}

export function useLoan() {
  const context = useContext(LoanContext);
  if (!context) {
    throw new Error("useLoan must be used within LoanProvider");
  }
  return context;
}
