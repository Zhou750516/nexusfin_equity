import { beforeEach, describe, expect, it, vi } from "vitest";

const requestMock = vi.fn();
const interceptorUseMock = vi.fn();
const headersSetMock = vi.fn();
const toastErrorMock = vi.fn();
const recoverJointLoginSessionMock = vi.fn();
const shouldRecoverJointSessionMock = vi.fn();

vi.mock("axios", () => {
  class MockAxiosHeaders {
    static from() {
      return {
        set: headersSetMock,
      };
    }
  }

  return {
    default: {
      create: vi.fn(() => ({
        request: requestMock,
        interceptors: {
          request: {
            use: interceptorUseMock,
          },
        },
      })),
      isAxiosError: (error: unknown) => Boolean((error as { isAxiosError?: boolean })?.isAxiosError),
    },
    AxiosHeaders: MockAxiosHeaders,
    AxiosError: class MockAxiosError extends Error {},
  };
});

vi.mock("sonner", () => ({
  toast: {
    error: toastErrorMock,
  },
}));

vi.mock("@/lib/joint-session", () => ({
  recoverJointLoginSession: recoverJointLoginSessionMock,
}));

vi.mock("@/lib/api.logic", () => ({
  shouldRecoverJointSession: shouldRecoverJointSessionMock,
}));

describe("apiRequest", () => {
  beforeEach(() => {
    requestMock.mockReset();
    interceptorUseMock.mockReset();
    headersSetMock.mockReset();
    toastErrorMock.mockReset();
    recoverJointLoginSessionMock.mockReset();
    shouldRecoverJointSessionMock.mockReset();
    vi.resetModules();
  });

  it("recovers joint session on unauthorized response with redirect hint", async () => {
    shouldRecoverJointSessionMock.mockReturnValue(true);
    recoverJointLoginSessionMock.mockReturnValue(true);
    requestMock.mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 401,
        data: {
          data: {
            redirectHint: "joint-entry",
          },
        },
      },
      message: "Missing auth cookie",
    });

    const { apiRequest } = await import("./api");
    const promise = apiRequest({ method: "GET", url: "/users/me" });

    await expect(Promise.race([
      promise.then(() => "resolved").catch(() => "rejected"),
      new Promise((resolve) => setTimeout(() => resolve("pending"), 0)),
    ])).resolves.toBe("pending");

    expect(shouldRecoverJointSessionMock).toHaveBeenCalledWith(401, {
      data: {
        redirectHint: "joint-entry",
      },
    });
    expect(recoverJointLoginSessionMock).toHaveBeenCalledTimes(1);
    expect(toastErrorMock).not.toHaveBeenCalled();
  });

  it("toasts and throws response message when recovery does not apply", async () => {
    shouldRecoverJointSessionMock.mockReturnValue(false);
    requestMock.mockRejectedValue({
      isAxiosError: true,
      response: {
        status: 500,
        data: {
          message: "System busy",
        },
      },
      message: "Request failed",
    });

    const { apiRequest } = await import("./api");

    await expect(apiRequest({ method: "GET", url: "/users/me" })).rejects.toThrow("System busy");
    expect(toastErrorMock).toHaveBeenCalledWith("System busy");
  });
});
