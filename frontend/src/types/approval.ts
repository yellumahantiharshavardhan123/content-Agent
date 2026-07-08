import type { ContentType } from "@/types/ai";

export type ApprovalStatus = "DRAFT" | "PENDING_APPROVAL" | "APPROVED" | "REJECTED" | "READY_FOR_PUBLISH";
export type ApprovalAction = "SUBMIT" | "APPROVE" | "REJECT" | "COMMENT";

export interface ApprovalComment {
  id: string;
  approvalId: string;
  authorId: string | null;
  authorEmail: string | null;
  comment: string;
  createdAt: string;
}

export interface Approval {
  id: string;
  contentId: string | null;
  contentTitle: string | null;
  contentType: ContentType | null;
  reviewerId: string | null;
  status: ApprovalStatus;
  remarks: string | null;
  approvedAt: string | null;
  rejectedAt: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
  comments: ApprovalComment[];
}

export interface ApprovalHistoryEntry {
  id: string;
  approvalId: string | null;
  contentId: string | null;
  action: ApprovalAction;
  previousStatus: ApprovalStatus | null;
  newStatus: ApprovalStatus;
  actorId: string | null;
  actorEmail: string | null;
  remarks: string | null;
  createdAt: string;
}

export interface ListApprovalsParams {
  page?: number;
  size?: number;
  status?: ApprovalStatus;
  search?: string;
  dateFrom?: string;
  dateTo?: string;
}
