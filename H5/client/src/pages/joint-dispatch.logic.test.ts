import { describe, expect, it } from "vitest";
import { resolveDispatchPageState } from "@/pages/joint-dispatch.logic";

describe("resolveDispatchPageState", () => {
  it("routes to redirect mode when backend allows direct supplier jump", () => {
    const result = resolveDispatchPageState({
      allowRedirect: true,
      redirectMode: "DIRECT",
      supplierUrl: "https://supplier.example/benefit",
    });

    expect(result.type).toBe("redirect");
    expect(result.url).toBe("https://supplier.example/benefit");
  });
});
