import type { Role } from "./types";

export interface JwtClaims {
  userId?: number;
  username?: string;
  role?: Role;
  sub?: string;
  exp?: number;
  [k: string]: unknown;
}

// Decode the payload of a JWT without verifying the signature.
// The backend signs tokens with userId/username/role claims; we read them to
// know the current user since there is no /auth/me endpoint.
export function decodeJwt(token: string): JwtClaims | null {
  try {
    const payload = token.split(".")[1];
    if (!payload) return null;
    const json = atob(payload.replace(/-/g, "+").replace(/_/g, "/"));
    const decoded = decodeURIComponent(
      Array.from(json)
        .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
        .join(""),
    );
    return JSON.parse(decoded) as JwtClaims;
  } catch {
    return null;
  }
}

export function isExpired(claims: JwtClaims | null): boolean {
  if (!claims?.exp) return false;
  return claims.exp * 1000 < Date.now();
}
