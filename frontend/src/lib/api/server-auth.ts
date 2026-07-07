import { cookies } from "next/headers";
import type { ApiResponse } from "@/lib/api/client";
import type { User } from "@/types/user";

const INTERNAL_API_BASE_URL = process.env.API_INTERNAL_BASE_URL ?? "http://localhost:8080/api";

/** Server Component-side current-user fetch: forwards the incoming request's cookies to the backend. */
export async function getCurrentUserServerSide(): Promise<User | null> {
  const cookieStore = await cookies();
  const cookieHeader = cookieStore.toString();

  const response = await fetch(`${INTERNAL_API_BASE_URL}/auth/me`, {
    headers: { Cookie: cookieHeader },
    cache: "no-store",
  });

  if (!response.ok) {
    return null;
  }

  const body = (await response.json()) as ApiResponse<User>;
  return body.data ?? null;
}
