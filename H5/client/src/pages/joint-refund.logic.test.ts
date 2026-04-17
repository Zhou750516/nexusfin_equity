import { describe, expect, it } from "vitest";
import { resolveRefundPageState } from "@/pages/joint-refund.logic";

describe("resolveRefundPageState", () => {
  it("returns apply state when refund is allowed and no processing result exists", () => {
    const result = resolveRefundPageState({
      refundable: true,
      refundStatus: "NONE",
    });

    expect(result.type).toBe("apply");
  });
});
