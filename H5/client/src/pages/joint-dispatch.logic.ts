export function resolveDispatchPageState(input: {
  allowRedirect: boolean;
  redirectMode: "DIRECT" | "INTERMEDIATE";
  supplierUrl?: string | null;
}) {
  if (input.allowRedirect && input.redirectMode === "DIRECT" && input.supplierUrl) {
    return {
      type: "redirect" as const,
      url: input.supplierUrl,
    };
  }

  return {
    type: "info" as const,
  };
}
