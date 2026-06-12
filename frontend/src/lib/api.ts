import { tokenStore } from "./tokenStore";
import type { ApiEnvelope, Page, TokenResponse } from "./types";

const BASE = "/api/v1";

export class ApiError extends Error {
  code: string | null;
  status: number;
  constructor(message: string, code: string | null, status: number) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  // some endpoints are public; for those we skip auth-refresh retry
  auth?: boolean;
  query?: Record<string, string | number | boolean | undefined | null>;
}

function buildUrl(path: string, query?: RequestOptions["query"]): string {
  const url = BASE + path;
  if (!query) return url;
  const params = new URLSearchParams();
  for (const [k, v] of Object.entries(query)) {
    if (v !== undefined && v !== null && v !== "") params.append(k, String(v));
  }
  const qs = params.toString();
  return qs ? `${url}?${qs}` : url;
}

// Single in-flight refresh shared across concurrent 401s.
let refreshing: Promise<boolean> | null = null;

async function doRefresh(): Promise<boolean> {
  const refreshToken = tokenStore.refresh;
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });
    if (!res.ok) return false;
    const env = (await res.json()) as ApiEnvelope<TokenResponse>;
    if (!env.success || !env.data) return false;
    tokenStore.set(env.data);
    return true;
  } catch {
    return false;
  }
}

async function rawRequest<T>(
  path: string,
  opts: RequestOptions,
  retry: boolean,
): Promise<T> {
  const isForm = opts.body instanceof FormData;
  const headers: Record<string, string> = {};
  // For multipart, let the browser set Content-Type (with the boundary).
  if (opts.body !== undefined && !isForm) headers["Content-Type"] = "application/json";
  const access = tokenStore.access;
  if (access) headers["Authorization"] = `Bearer ${access}`;

  const res = await fetch(buildUrl(path, opts.query), {
    method: opts.method ?? "GET",
    headers,
    body:
      opts.body === undefined
        ? undefined
        : isForm
          ? (opts.body as FormData)
          : JSON.stringify(opts.body),
  });

  // Try a transparent token refresh on 401 once.
  if (res.status === 401 && retry && tokenStore.refresh) {
    if (!refreshing) refreshing = doRefresh().finally(() => (refreshing = null));
    const ok = await refreshing;
    if (ok) return rawRequest<T>(path, opts, false);
    tokenStore.clear();
    window.dispatchEvent(new Event("ca:logout"));
    throw new ApiError("Session expired. Please log in again.", "UNAUTHORIZED", 401);
  }

  let env: ApiEnvelope<T> | null = null;
  const text = await res.text();
  if (text) {
    try {
      env = JSON.parse(text) as ApiEnvelope<T>;
    } catch {
      // non-JSON error body
    }
  }

  if (!res.ok || (env && env.success === false)) {
    const message = env?.error ?? `Request failed (${res.status})`;
    throw new ApiError(message, env?.errorCode ?? null, res.status);
  }

  return (env ? (env.data as T) : (undefined as T));
}

function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  return rawRequest<T>(path, opts, opts.auth !== false);
}

export const api = {
  get: <T>(path: string, query?: RequestOptions["query"]) =>
    request<T>(path, { method: "GET", query }),
  post: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "POST", body }),
  put: <T>(path: string, body?: unknown) =>
    request<T>(path, { method: "PUT", body }),
  // multipart upload (FormData body, browser sets the boundary)
  upload: <T>(path: string, form: FormData) =>
    request<T>(path, { method: "POST", body: form }),
  // raw access for auth flows that need the envelope/no-retry semantics
  request,
};

// Convenience type re-export for pages
export type { Page };
