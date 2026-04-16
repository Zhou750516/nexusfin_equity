import axios, { AxiosError, AxiosHeaders, type AxiosRequestConfig } from "axios";
import { toast } from "sonner";
import { DEFAULT_LOCALE, normalizeLocale, type Locale } from "@/i18n/locale";
import type { ApiResponse } from "@/types/loan.types";

const AUTH_TOKEN_STORAGE_KEY = "nexusfin.h5.auth-token";
let activeLocale: Locale = DEFAULT_LOCALE;

export function setApiLocale(locale: Locale) {
  activeLocale = normalizeLocale(locale);
}

const rawApiClient = axios.create({
  baseURL: "/api",
  timeout: 15000,
  withCredentials: true,
});

rawApiClient.interceptors.request.use((config) => {
  const nextConfig = { ...config };
  const headers = AxiosHeaders.from(config.headers);

  if (typeof window !== "undefined") {
    headers.set("Accept-Language", activeLocale);

    const token = window.localStorage.getItem(AUTH_TOKEN_STORAGE_KEY);
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
  }

  nextConfig.headers = headers;
  return nextConfig;
});

function extractMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  if (axios.isAxiosError(error)) {
    const responseMessage = readResponseMessage(error);
    if (responseMessage) {
      return responseMessage;
    }
    if (error.message) {
      return error.message;
    }
  }

  return "Request failed";
}

function readResponseMessage(error: AxiosError<unknown>): string | null {
  const payload = error.response?.data;
  if (typeof payload === "object" && payload !== null && "message" in payload) {
    const message = payload.message;
    return typeof message === "string" && message ? message : null;
  }
  return null;
}

export async function apiRequest<T>(config: AxiosRequestConfig): Promise<T> {
  try {
    const response = await rawApiClient.request<ApiResponse<T>>(config);
    const payload = response.data;

    if (payload.code !== 0) {
      throw new Error(payload.message || "Request failed");
    }

    return payload.data;
  } catch (error) {
    const message = extractMessage(error);
    toast.error(message);
    throw new Error(message);
  }
}

export const apiClient = rawApiClient;
