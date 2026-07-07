import { apiFetch } from "@/lib/api/client";
import type { User } from "@/types/user";

export function login(email: string, password: string) {
  return apiFetch<User>("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
}

export function logout() {
  return apiFetch<void>("/auth/logout", { method: "POST" });
}

export function getCurrentUser() {
  return apiFetch<User>("/auth/me");
}

export function changePassword(currentPassword: string, newPassword: string) {
  return apiFetch<User>("/auth/change-password", {
    method: "POST",
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

export function forgotPassword(email: string) {
  return apiFetch<void>("/auth/forgot-password", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export function resetPassword(token: string, newPassword: string) {
  return apiFetch<void>("/auth/reset-password", {
    method: "POST",
    body: JSON.stringify({ token, newPassword }),
  });
}
