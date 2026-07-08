import type { ContentType } from "@/types/ai";

export type DraftStatus = "DRAFT" | "READY_FOR_REVIEW" | "APPROVED" | "REJECTED" | "PUBLISHED" | "ARCHIVED";

export interface ContentDraft {
  id: string;
  generatedContentId: string | null;
  mediaId: string | null;
  contentType: ContentType;
  title: string;
  contentText: string;
  status: DraftStatus;
  deleted: boolean;
  deletedAt: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  updatedBy: string | null;
}

export interface CreateDraftInput {
  generatedContentId: string;
  title?: string;
}

export interface UpdateDraftInput {
  title?: string;
  contentText: string;
  status?: DraftStatus;
}

export interface ListDraftsParams {
  page?: number;
  size?: number;
  search?: string;
  status?: DraftStatus;
  contentType?: ContentType;
  mediaId?: string;
  includeDeleted?: boolean;
}
