"use client";

import { CheckSquare } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { ApprovalQueueCard } from "@/components/approvals/approval-queue-card";
import type { Approval } from "@/types/approval";

interface ApprovalQueueProps {
  items: Approval[];
  isLoading: boolean;
  onChanged: (updated: Approval) => void;
}

export function ApprovalQueue({ items, isLoading, onChanged }: ApprovalQueueProps) {
  if (isLoading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-44 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-2 py-16 text-center text-muted-foreground">
        <CheckSquare className="size-8" />
        <p className="text-sm">Nothing here.</p>
      </div>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {items.map((item) => (
        <ApprovalQueueCard key={item.id} approval={item} onChanged={onChanged} />
      ))}
    </div>
  );
}
