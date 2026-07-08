export type MediaType = "IMAGE" | "VIDEO" | "PDF" | "TEXT_NOTE";

export interface MediaItem {
  id: string;
  fileName: string;
  mediaType: MediaType;
  contentType: string;
  fileSizeBytes: number;
  description: string | null;
  url: string;
  createdAt: string;
}
