export function normalizeAppBase(baseUrl: string = import.meta.env.BASE_URL): string {
  const trimmed = baseUrl.trim();
  if (trimmed === "" || trimmed === "/") {
    return "";
  }
  const normalized = trimmed.startsWith("/") ? trimmed : `/${trimmed}`;
  return normalized.endsWith("/") ? normalized.slice(0, -1) : normalized;
}

export function resolveAppHref(pathname: string, baseUrl: string = import.meta.env.BASE_URL): string {
  const base = normalizeAppBase(baseUrl);
  if (!base) {
    return pathname;
  }
  if (pathname === "/") {
    return `${base}/`;
  }
  const normalizedPath = pathname.startsWith("/") ? pathname : `/${pathname}`;
  return `${base}${normalizedPath}`;
}

export function buildPath(pathname: string, params: Record<string, string | number | null | undefined>): string {
  const search = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined && value !== "") {
      search.set(key, String(value));
    }
  });

  const query = search.toString();
  return query ? `${pathname}?${query}` : pathname;
}

export function getQueryParam(name: string): string | null {
  if (typeof window === "undefined") {
    return null;
  }
  return new URLSearchParams(window.location.search).get(name);
}
