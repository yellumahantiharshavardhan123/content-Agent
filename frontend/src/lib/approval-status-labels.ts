import type { ApprovalStatus } from "@/types/approval";

export const APPROVAL_STATUS_LABELS: Record<ApprovalStatus, string> = {
  DRAFT: "Draft",
  PENDING_APPROVAL: "Pending Approval",
  APPROVED: "Approved",
  REJECTED: "Rejected",
  READY_FOR_PUBLISH: "Ready for Publish",
};

export const APPROVAL_STATUS_BADGE_VARIANT: Record<ApprovalStatus, "default" | "secondary" | "destructive" | "outline"> = {
  DRAFT: "secondary",
  PENDING_APPROVAL: "outline",
  APPROVED: "default",
  REJECTED: "destructive",
  READY_FOR_PUBLISH: "default",
};

export const APPROVAL_FILTER_TABS: { value: ApprovalStatus; label: string }[] = [
  { value: "PENDING_APPROVAL", label: "Pending" },
  { value: "READY_FOR_PUBLISH", label: "Approved" },
  { value: "REJECTED", label: "Rejected" },
];
