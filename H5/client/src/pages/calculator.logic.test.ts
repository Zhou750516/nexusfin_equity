import { describe, expect, it } from "vitest";
import { buildApplyLoanPayload } from "./calculator.logic";

describe("calculator apply payload", () => {
  it("maps selected purpose key into apply payload purpose", () => {
    expect(
      buildApplyLoanPayload({
        amount: 3000,
        term: 3,
        receivingAccountId: "acc_001",
        agreedProtocols: ["user_agreement", "loan_agreement"],
        purposeKey: "calculator.loanPurpose.rent",
      }),
    ).toEqual({
      amount: 3000,
      term: 3,
      receivingAccountId: "acc_001",
      agreedProtocols: ["user_agreement", "loan_agreement"],
      purpose: "rent",
    });
  });
});
