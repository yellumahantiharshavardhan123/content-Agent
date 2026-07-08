"use client";

import Link from "next/link";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { CONTENT_TYPE_LABELS } from "@/lib/content-type-labels";
import { APPROVAL_STATUS_BADGE_VARIANT, APPROVAL_STATUS_LABELS } from "@/lib/approval-status-labels";
import { ApproveDialog } from "@/components/approvals/approve-dialog";
import { RejectDialog } from "@/components/approvals/reject-dialog";
import { ViewDraftDialog } from "@/components/approvals/view-draft-dialog";
import type { Approval } from "@/types/approval";

interface ApprovalQueueCardProps {
  approval: Approval;
  onChanged: (updated: Approval) => void;
}

export function ApprovalQueueCard({ approval, onChanged }: ApprovalQueueCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-sm">
          <Link href={`/approvals/${approval.id}`} className="hover:underline">
            {approval.contentTitle ?? "Untitled content"}
          </Link>
        </CardTitle>
        <div className="flex flex-wrap items-center gap-1.5 pt-1">
          <Badge variant={APPROVAL_STATUS_BADGE_VARIANT[approval.status]}>
            {APPROVAL_STATUS_LABELS[approval.status]}
          </Badge>
          {approval.contentType && <Badge variant="outline">{CONTENT_TYPE_LABELS[approval.contentType]}</Badge>}
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground">
          Submitted {new Date(approval.createdAt).toLocaleString()}
        </p>
        {approval.remarks && <p className="mt-2 text-sm italic text-muted-foreground">&quot;{approval.remarks}&quot;</p>}
      </CardContent>
      <CardFooter className="flex flex-wrap gap-1.5">
        <ViewDraftDialog contentId={approval.contentId} />
        {approval.status === "PENDING_APPROVAL" && (
          <>
            <ApproveDialog approval={approval} onApproved={onChanged} />
            <RejectDialog approval={approval} onRejected={onChanged} />
          </>
        )}
        <Button variant="ghost" size="sm" render={<Link href={`/approvals/${approval.id}`} />}>
          Details
        </Button>
      </CardFooter>
    </Card>
  );
}
