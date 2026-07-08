import { apiFetch, type PageResponse } from "@/lib/api/client";
import type { ContentDraft, CreateDraftInput, ListDraftsParams, UpdateDraftInput } from "@/types/drafts";

export function listDrafts(params: ListDraftsParams = {}) {
  const query = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 20),
    sort: "createdAt,desc",
  });
  if (params.search) query.set("search", params.search);
  if (params.status) query.set("status", params.status);
  if (params.contentType) query.set("contentType", params.contentType);
  if (params.mediaId) query.set("mediaId", params.mediaId);
  if (params.includeDeleted) query.set("includeDeleted", "true");

  return apiFetch<PageResponse<ContentDraft>>(`/drafts?${query.toString()}`);
}

export function getDraft(id: string) {
  return apiFetch<ContentDraft>(`/drafts/${id}`);
}

export function createDraft(input: CreateDraftInput) {
  return apiFetch<ContentDraft>("/drafts", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function updateDraft(id: string, input: UpdateDraftInput) {
  return apiFetch<ContentDraft>(`/drafts/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  });
}

export function deleteDraft(id: string) {
  return apiFetch<void>(`/drafts/${id}`, { method: "DELETE" });
}

export function duplicateDraft(id: string) {
  return apiFetch<ContentDraft>(`/drafts/${id}/duplicate`, { method: "POST" });
}

export function restoreDraft(id: string) {
  return apiFetch<ContentDraft>(`/drafts/${id}/restore`, { method: "POST" });
}

export function finalizeDraft(id: string) {
  return apiFetch<ContentDraft>(`/drafts/${id}/finalize`, { method: "POST" });
}
