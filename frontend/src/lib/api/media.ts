import { apiFetch, apiFetchFormData, type PageResponse } from "@/lib/api/client";
import type { MediaItem, MediaType } from "@/types/media";

export function uploadMedia(file: File, description?: string) {
  const formData = new FormData();
  formData.append("file", file);
  if (description) {
    formData.append("description", description);
  }
  return apiFetchFormData<MediaItem>("/media/upload", formData);
}

export function listMedia(page = 0, size = 24, type?: MediaType) {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (type) {
    params.set("type", type);
  }
  return apiFetch<PageResponse<MediaItem>>(`/media?${params.toString()}`);
}

export function getMedia(id: string) {
  return apiFetch<MediaItem>(`/media/${id}`);
}

export function deleteMedia(id: string) {
  return apiFetch<void>(`/media/${id}`, { method: "DELETE" });
}
