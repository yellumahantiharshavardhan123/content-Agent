"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowLeft } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { CONTENT_TYPE_LABELS } from "@/lib/content-type-labels";
import { APPROVAL_STATUS_BADGE_VARIANT, APPROVAL_STATUS_LABELS } from "@/lib/approval-status-labels";
import { ApproveDialog } from "@/components/approvals/approve-dialog";
import { RejectDialog } from "@/components/approvals/reject-dialog";
import { ViewDraftDialog } from "@/components/approvals/view-draft-dialog";
import { ApprovalHistoryTimeline } from "@/components/approvals/approval-history-timeline";
import { ApprovalCommentsPanel } from "@/components/approvals/approval-comments-panel";
import { getApproval, getApprovalHistory } from "@/lib/api/approval";
import { ApiError } from "@/lib/api/client";
import type { Approval, ApprovalComment, ApprovalHistoryEntry } from "@/types/approval";

export default function ApprovalDetailPage() {
  const params = useParams<{ id: string }>();
  const [approval, setApproval] = useState<Approval | null>(null);
  const [history, setHistory] = useState<ApprovalHistoryEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isHistoryLoading, setIsHistoryLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const response = await getApproval(params.id);
      setApproval(response.data ?? null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not load this approval.");
    } finally {
      setIsLoading(false);
    }
  }, [params.id]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    refresh();
  }, [refresh]);

  const refreshHistory = useCallback(async (contentId: string) => {
    setIsHistoryLoading(true);
    try {
      const response = await getApprovalHistory(contentId);
      setHistory(response.data?.content ?? []);
    } finally {
      setIsHistoryLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!approval?.contentId) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    refreshHistory(approval.contentId);
  }, [approval?.contentId, refreshHistory]);

  function handleChanged(updated: Approval) {
    setApproval(updated);
    if (updated.contentId) {
      refreshHistory(updated.contentId);
    }
  }

  function handleCommentAdded(comment: ApprovalComment) {
    setApproval((prev) => (prev ? { ...prev, comments: [...prev.comments, comment] } : prev));
    if (approval?.contentId) {
      refreshHistory(approval.contentId);
    }
  }

  return (
    <div className="flex flex-1 flex-col gap-6 p-6">
      <Button variant="ghost" size="sm" className="self-start" render={<Link href="/approvals" />}>
        <ArrowLeft className="size-3.5" />
        Back to Approvals
      </Button>

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {isLoading && (
        <div className="flex flex-col gap-4">
          <Skeleton className="h-24 w-full rounded-lg" />
          <Skeleton className="h-64 w-full rounded-lg" />
        </div>
      )}

      {!isLoading && approval && (
        <>
          <Card>
            <CardHeader>
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <CardTitle className="text-xl">{approval.contentTitle ?? "Untitled content"}</CardTitle>
                  <div className="mt-2 flex flex-wrap items-center gap-1.5">
                    <Badge variant={APPROVAL_STATUS_BADGE_VARIANT[approval.status]}>
                      {APPROVAL_STATUS_LABELS[approval.status]}
                    </Badge>
                    {approval.contentType && <Badge variant="outline">{CONTENT_TYPE_LABELS[approval.contentType]}</Badge>}
                  </div>
                </div>
                <div className="flex flex-wrap gap-1.5">
                  <ViewDraftDialog contentId={approval.contentId} size="default" />
                  {approval.status === "PENDING_APPROVAL" && (
                    <>
                      <ApproveDialog approval={approval} onApproved={handleChanged} size="default" />
                      <RejectDialog approval={approval} onRejected={handleChanged} size="default" />
                    </>
                  )}
                </div>
              </div>
            </CardHeader>
            <CardContent className="flex flex-col gap-1 text-sm text-muted-foreground">
              <p>Submitted {new Date(approval.createdAt).toLocaleString()}</p>
              {approval.approvedAt && <p>Approved {new Date(approval.approvedAt).toLocaleString()}</p>}
              {approval.rejectedAt && <p>Rejected {new Date(approval.rejectedAt).toLocaleString()}</p>}
              {approval.remarks && <p className="italic">&quot;{approval.remarks}&quot;</p>}
            </CardContent>
          </Card>

          <div className="grid gap-6 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="text-base">History</CardTitle>
              </CardHeader>
              <CardContent>
                <ApprovalHistoryTimeline entries={history} isLoading={isHistoryLoading} />
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-base">Comments</CardTitle>
              </CardHeader>
              <CardContent>
                <ApprovalCommentsPanel approval={approval} onCommentAdded={handleCommentAdded} />
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}
