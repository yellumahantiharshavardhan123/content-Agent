"use client";

import { Send } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { CONTENT_TYPE_LABELS } from "@/lib/content-type-labels";
import { PublishDialog } from "@/components/instagram/publish-dialog";
import type { Approval } from "@/types/approval";
import type { InstagramPost } from "@/types/instagram";

interface PublishingQueueProps {
  items: Approval[];
  isLoading: boolean;
  connected: boolean;
  onPublished: (post: InstagramPost) => void;
}

export function PublishingQueue({ items, isLoading, connected, onPublished }: PublishingQueueProps) {
  if (isLoading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-36 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center gap-2 py-12 text-center text-muted-foreground">
        <Send className="size-8" />
        <p className="text-sm">Nothing approved and ready to publish yet.</p>
      </div>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {items.map((approval) => (
        <Card key={approval.id}>
          <CardHeader>
            <CardTitle className="text-sm">{approval.contentTitle}</CardTitle>
            {approval.contentType && <Badge variant="outline">{CONTENT_TYPE_LABELS[approval.contentType]}</Badge>}
          </CardHeader>
          <CardContent>
            <p className="text-sm text-muted-foreground">
              Approved {new Date(approval.approvedAt ?? approval.updatedAt).toLocaleString()}
            </p>
          </CardContent>
          <CardFooter>
            {connected ? (
              <PublishDialog approval={approval} onPublished={onPublished} />
            ) : (
              <p className="text-xs text-muted-foreground">Connect an Instagram account to publish.</p>
            )}
          </CardFooter>
        </Card>
      ))}
    </div>
  );
}
