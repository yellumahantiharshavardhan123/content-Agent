"use client";

import { CheckCircle2, History, Link2, Plug, Unplug, XCircle } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import type { InstagramHistoryEntry } from "@/types/instagram";

interface InstagramHistoryListProps {
  entries: InstagramHistoryEntry[];
  isLoading: boolean;
}

const ACTION_ICON: Record<InstagramHistoryEntry["action"], React.ReactNode> = {
  CONNECT: <Plug className="size-4" />,
  DISCONNECT: <Unplug className="size-4" />,
  PUBLISH_ATTEMPT: <History className="size-4" />,
  PUBLISH_SUCCESS: <CheckCircle2 className="size-4 text-green-600 dark:text-green-500" />,
  PUBLISH_FAILURE: <XCircle className="size-4 text-destructive" />,
  RETRY: <Link2 className="size-4" />,
};

const ACTION_LABEL: Record<InstagramHistoryEntry["action"], string> = {
  CONNECT: "Account connected",
  DISCONNECT: "Account disconnected",
  PUBLISH_ATTEMPT: "Publish attempted",
  PUBLISH_SUCCESS: "Published successfully",
  PUBLISH_FAILURE: "Publish failed",
  RETRY: "Publish retried",
};

export function InstagramHistoryList({ entries, isLoading }: InstagramHistoryListProps) {
  if (isLoading) {
    return (
      <div className="flex flex-col gap-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-14 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (entries.length === 0) {
    return <p className="text-sm text-muted-foreground">No publishing activity yet.</p>;
  }

  return (
    <ol className="flex flex-col gap-4">
      {entries.map((entry) => (
        <li key={entry.id} className="flex gap-3">
          <div className="mt-0.5 flex size-7 shrink-0 items-center justify-center rounded-full border bg-muted">
            {ACTION_ICON[entry.action]}
          </div>
          <div className="flex flex-col gap-0.5">
            <p className="text-sm font-medium">
              {ACTION_LABEL[entry.action]}
              {entry.publisherName && <span className="text-muted-foreground"> - {entry.publisherName}</span>}
            </p>
            <p className="text-xs text-muted-foreground">
              {entry.actorEmail ?? "System"} - {new Date(entry.createdAt).toLocaleString()}
            </p>
            {entry.errorMessage && <p className="text-sm text-destructive">{entry.errorMessage}</p>}
          </div>
        </li>
      ))}
    </ol>
  );
}
