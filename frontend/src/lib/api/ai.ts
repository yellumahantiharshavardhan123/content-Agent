import { apiFetch, type PageResponse } from "@/lib/api/client";
import type {
  ContentType,
  GenerateContentInput,
  GeneratedContent,
  GenerationHistoryEntry,
} from "@/types/ai";

export function generateContent(input: GenerateContentInput) {
  return apiFetch<GeneratedContent>("/ai/generate", {
    method: "POST",
    body: JSON.stringify(input),
  });
}

export function regenerateContent(contentId: string) {
  return apiFetch<GeneratedContent>("/ai/regenerate", {
    method: "POST",
    body: JSON.stringify({ contentId }),
  });
}

export function getHistory(page = 0, size = 20) {
  return apiFetch<PageResponse<GenerationHistoryEntry>>(`/ai/history?page=${page}&size=${size}&sort=createdAt,desc`);
}

export function listContent(page = 0, size = 20, mediaId?: string, contentType?: ContentType) {
  const params = new URLSearchParams({ page: String(page), size: String(size), sort: "createdAt,desc" });
  if (mediaId) params.set("mediaId", mediaId);
  if (contentType) params.set("contentType", contentType);
  return apiFetch<PageResponse<GeneratedContent>>(`/ai/content?${params.toString()}`);
}

export function getContent(id: string) {
  return apiFetch<GeneratedContent>(`/ai/content/${id}`);
}

export function updateContent(id: string, generatedText: string, draft: boolean) {
  return apiFetch<GeneratedContent>(`/ai/content/${id}`, {
    method: "PUT",
    body: JSON.stringify({ generatedText, draft }),
  });
}

export function deleteContent(id: string) {
  return apiFetch<void>(`/ai/content/${id}`, { method: "DELETE" });
}
