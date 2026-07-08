import { apiFetch, type PageResponse } from "@/lib/api/client";
import type { ConnectAccountInput, InstagramAccount, InstagramHistoryEntry, InstagramPost, PublishInput } from "@/types/instagram";

export function connectInstagramAccount(input: ConnectAccountInput) {
  return apiFetch<InstagramAccount>("/instagram/connect", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function getInstagramStatus() {
  return apiFetch<InstagramAccount>("/instagram/status");
}

export function publishToInstagram(input: PublishInput) {
  return apiFetch<InstagramPost>("/instagram/publish", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function publishToInstagramMock(input: PublishInput) {
  return apiFetch<InstagramPost>("/instagram/publish/mock", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function getInstagramHistory(page = 0, size = 20) {
  return apiFetch<PageResponse<InstagramHistoryEntry>>(
    `/instagram/history?page=${page}&size=${size}&sort=createdAt,desc`
  );
}

export function disconnectInstagramAccount() {
  return apiFetch<void>("/instagram/disconnect", { method: "DELETE" });
}
