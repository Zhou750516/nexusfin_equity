import { toLoanPurpose } from "@/lib/loan-purpose";
import type { ApplyParams } from "@/types/loan.types";

export function buildApplyLoanPayload(input: {
  amount: number;
  term: number;
  receivingAccountId: string;
  agreedProtocols: string[];
  purposeKey: string;
}): ApplyParams {
  return {
    amount: input.amount,
    term: input.term,
    receivingAccountId: input.receivingAccountId,
    agreedProtocols: input.agreedProtocols,
    purpose: toLoanPurpose(input.purposeKey),
  };
}
