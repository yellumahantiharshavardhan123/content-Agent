import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

const PUBLIC_PATHS = ["/login"];
const INTERNAL_API_BASE_URL = process.env.API_INTERNAL_BASE_URL ?? "http://localhost:8080/api";

/** Overlays fresh Set-Cookie values onto the request's Cookie header so that
 * Server Components rendered later in this same request see the refreshed
 * tokens - mutating only the outgoing response's Set-Cookie would otherwise
 * take effect one request too late (the browser gets it, this render doesn't). */
function withRefreshedCookies(headers: Headers, setCookies: string[]): Headers {
  const merged = new Map<string, string>();

  (headers.get("cookie") ?? "")
    .split(";")
    .map((pair) => pair.trim())
    .filter(Boolean)
    .forEach((pair) => {
      const idx = pair.indexOf("=");
      if (idx > -1) merged.set(pair.slice(0, idx), pair.slice(idx + 1));
    });

  setCookies.forEach((cookie) => {
    const nameValue = cookie.split(";")[0];
    const idx = nameValue.indexOf("=");
    if (idx > -1) merged.set(nameValue.slice(0, idx).trim(), nameValue.slice(idx + 1).trim());
  });

  const newHeaders = new Headers(headers);
  newHeaders.set("cookie", Array.from(merged.entries()).map(([k, v]) => `${k}=${v}`).join("; "));
  return newHeaders;
}

/**
 * Route guard + silent session refresh. Runs (Node.js runtime) before every
 * page request: bounces unauthenticated visitors to /login, and when only
 * the access token has expired, transparently rotates it via the refresh
 * cookie before letting the request through - the real security boundary
 * is still the signed JWT verified server-side by the Spring Boot API.
 */
export async function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const isPublicPath = PUBLIC_PATHS.some((path) => pathname === path || pathname.startsWith(`${path}/`));

  const accessToken = request.cookies.get("access_token")?.value;
  const refreshToken = request.cookies.get("refresh_token")?.value;

  if (isPublicPath) {
    if (accessToken) {
      return NextResponse.redirect(new URL("/dashboard", request.url));
    }
    return NextResponse.next();
  }

  if (accessToken) {
    return NextResponse.next();
  }

  if (refreshToken) {
    try {
      const refreshResponse = await fetch(`${INTERNAL_API_BASE_URL}/auth/refresh`, {
        method: "POST",
        headers: { Cookie: `refresh_token=${refreshToken}` },
      });

      if (refreshResponse.ok) {
        const setCookies = refreshResponse.headers.getSetCookie();
        const response = NextResponse.next({
          request: { headers: withRefreshedCookies(request.headers, setCookies) },
        });
        setCookies.forEach((cookie) => response.headers.append("Set-Cookie", cookie));
        return response;
      }
    } catch {
      // Backend unreachable - fall through to the login redirect below.
    }
  }

  const loginUrl = new URL("/login", request.url);
  loginUrl.searchParams.set("from", pathname);
  return NextResponse.redirect(loginUrl);
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|.*\\.(?:svg|png|ico)$).*)"],
};
