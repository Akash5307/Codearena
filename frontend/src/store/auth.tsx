import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { authApi } from "../lib/services";
import { tokenStore } from "../lib/tokenStore";
import { decodeJwt } from "../lib/jwt";
import type { Role, TokenResponse } from "../lib/types";

export interface CurrentUser {
  userId: number | null;
  username: string;
  role: Role;
}

interface AuthContextValue {
  user: CurrentUser | null;
  loading: boolean;
  login: (usernameOrEmail: string, password: string) => Promise<void>;
  register: (username: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function userFromToken(accessToken: string | null): CurrentUser | null {
  if (!accessToken) return null;
  const claims = decodeJwt(accessToken);
  if (!claims) return null;
  const username = claims.username ?? claims.sub ?? "";
  if (!username) return null;
  return {
    userId: typeof claims.userId === "number" ? claims.userId : null,
    username,
    role: (claims.role as Role) ?? "USER",
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(() =>
    userFromToken(tokenStore.access),
  );
  const [loading] = useState(false);

  function applyTokens(tokens: TokenResponse) {
    tokenStore.set(tokens);
    setUser(userFromToken(tokens.accessToken));
  }

  // React to forced logout from the API client (refresh failure).
  useEffect(() => {
    const onLogout = () => setUser(null);
    window.addEventListener("ca:logout", onLogout);
    return () => window.removeEventListener("ca:logout", onLogout);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      async login(usernameOrEmail, password) {
        applyTokens(await authApi.login({ usernameOrEmail, password }));
      },
      async register(username, email, password) {
        applyTokens(await authApi.register({ username, email, password }));
      },
      async logout() {
        try {
          await authApi.logout(tokenStore.refresh);
        } catch {
          // ignore network/expiry errors on logout
        }
        tokenStore.clear();
        setUser(null);
      },
    }),
    [user, loading],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
