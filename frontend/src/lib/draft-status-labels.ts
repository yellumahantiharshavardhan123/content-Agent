import type { DraftStatus } from "@/types/drafts";

export const DRAFT_STATUS_LABELS: Record<DraftStatus, string> = {
  DRAFT: "Draft",
  READY_FOR_REVIEW: "Ready for Review",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  PUBLISHED: "Published",
  ARCHIVED: "Archived",
};

export const DRAFT_STATUSES = Object.keys(DRAFT_STATUS_LABELS) as DraftStatus[];

export const DRAFT_STATUS_BADGE_VARIANT: Record<DraftStatus, "default" | "secondary" | "destructive" | "outline"> = {
  DRAFT: "secondary",
  READY_FOR_REVIEW: "outline",
  APPROVED: "default",
  REJECTED: "destructive",
  PUBLISHED: "default",
  ARCHIVED: "outline",
};
