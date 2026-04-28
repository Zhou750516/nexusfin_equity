export function readRedirectHint(payload: unknown): string | null {
  if (typeof payload !== "object" || payload === null || !("data" in payload)) {
    return null;
  }

  const data = payload.data;
  if (typeof data !== "object" || data === null || !("redirectHint" in data)) {
    return null;
  }

  const redirectHint = data.redirectHint;
  return typeof redirectHint === "string" && redirectHint ? redirectHint : null;
}

export function shouldRecoverJointSession(status: number | undefined, payload: unknown): boolean {
  return status === 401 && readRedirectHint(payload) === "joint-entry";
}
