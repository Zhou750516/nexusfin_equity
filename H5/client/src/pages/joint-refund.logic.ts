export function resolveRefundPageState(input: { refundable: boolean; refundStatus?: string | null }) {
  if (!input.refundable) {
    return {
      type: "blocked" as const,
    };
  }

  if (input.refundStatus === "PROCESSING") {
    return {
      type: "processing" as const,
    };
  }

  return {
    type: "apply" as const,
  };
}
