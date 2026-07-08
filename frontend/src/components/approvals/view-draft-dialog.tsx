"use client";

import { useState } from "react";
import { Eye, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import { CONTENT_TYPE_LABELS } from "@/lib/content-type-labels";
import { getDraft } from "@/lib/api/drafts";
import { ApiError } from "@/lib/api/client";
import type { ContentDraft } from "@/types/drafts";

interface ViewDraftDialogProps {
  contentId: string | null;
  size?: "sm" | "default";
}

export function ViewDraftDialog({ contentId, size = "sm" }: ViewDraftDialogProps) {
  const [draft, setDraft] = useState<ContentDraft | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleOpenChange(open: boolean) {
    if (!open || !contentId || draft) return;
    setIsLoading(true);
    setError(null);
    try {
      const response = await getDraft(contentId);
      setDraft(response.data ?? null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not load the draft.");
    } finally {
      setIsLoading(false);
    }
  }

  if (!contentId) {
    return null;
  }

  return (
    <Dialog onOpenChange={handleOpenChange}>
      <DialogTrigger render={<Button size={size} variant="ghost" />}>
        <Eye className="size-3.5" />
        View Draft
      </DialogTrigger>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{draft?.title ?? "Draft content"}</DialogTitle>
          <DialogDescription>{draft ? CONTENT_TYPE_LABELS[draft.contentType] : "Loading..."}</DialogDescription>
        </DialogHeader>
        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        {isLoading && (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="size-5 animate-spin text-muted-foreground" />
          </div>
        )}
        {draft && (
          <p className="max-h-96 overflow-y-auto whitespace-pre-wrap text-sm">{draft.contentText}</p>
        )}
      </DialogContent>
    </Dialog>
  );
}
