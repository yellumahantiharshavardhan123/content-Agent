import { apiFetch, type PageResponse } from "@/lib/api/client";
import type { Approval, ApprovalHistoryEntry, ApprovalComment, ListApprovalsParams } from "@/types/approval";

export function submitForApproval(contentId: string) {
  return apiFetch<Approval>("/approval/submit", {
    method: "POST",
    body: JSON.stringify({ contentId }),
  });
}

export function approveApproval(id: string, remarks?: string) {
  return apiFetch<Approval>(`/approval/approve/${id}`, {
    method: "POST",
    body: JSON.stringify({ remarks: remarks || null }),
  });
}

export function rejectApproval(id: string, remarks: string) {
  return apiFetch<Approval>(`/approval/reject/${id}`, {
    method: "POST",
    body: JSON.stringify({ remarks }),
  });
}

export function addApprovalComment(id: string, comment: string) {
  return apiFetch<ApprovalComment>(`/approval/comment/${id}`, {
    method: "POST",
    body: JSON.stringify({ comment }),
  });
}

export function listApprovals(params: ListApprovalsParams = {}) {
  const query = new URLSearchParams({
    page: String(params.page ?? 0),
    size: String(params.size ?? 12),
    sort: "createdAt,desc",
  });
  if (params.status) query.set("status", params.status);
  if (params.search) query.set("search", params.search);
  if (params.dateFrom) query.set("dateFrom", params.dateFrom);
  if (params.dateTo) query.set("dateTo", params.dateTo);

  return apiFetch<PageResponse<Approval>>(`/approval/pending?${query.toString()}`);
}

export function getApproval(id: string) {
  return apiFetch<Approval>(`/approval/${id}`);
}

export function getApprovalHistory(contentId: string, page = 0, size = 50) {
  return apiFetch<PageResponse<ApprovalHistoryEntry>>(
    `/approval/history/${contentId}?page=${page}&size=${size}&sort=createdAt,desc`
  );
}
