"use client";

import { Badge } from "@/components/ui/badge";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from "@/components/ui/sheet";
import { CONTENT_TYPE_LABELS } from "@/lib/content-type-labels";
import { DRAFT_STATUS_BADGE_VARIANT, DRAFT_STATUS_LABELS } from "@/lib/draft-status-labels";
import type { ContentDraft } from "@/types/drafts";

interface DraftPreviewSheetProps {
  draft: ContentDraft;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function DraftPreviewSheet({ draft, open, onOpenChange }: DraftPreviewSheetProps) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>{draft.title}</SheetTitle>
          <SheetDescription>
            {CONTENT_TYPE_LABELS[draft.contentType]} - last updated {new Date(draft.updatedAt).toLocaleString()}
          </SheetDescription>
        </SheetHeader>
        <div className="flex flex-wrap items-center gap-1.5 px-4">
          <Badge variant={DRAFT_STATUS_BADGE_VARIANT[draft.status]}>{DRAFT_STATUS_LABELS[draft.status]}</Badge>
          {draft.deleted && <Badge variant="destructive">Deleted</Badge>}
        </div>
        <div className="flex-1 overflow-y-auto px-4 pb-4">
          <p className="whitespace-pre-wrap text-sm">{draft.contentText}</p>
        </div>
      </SheetContent>
    </Sheet>
  );
}
