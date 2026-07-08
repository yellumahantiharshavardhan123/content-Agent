"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { CopyCheck, Eye, Loader2, Pencil, RotateCcw, Trash2, CheckCircle2, Undo2, Send } from "lucide-react";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { CONTENT_TYPE_LABELS } from "@/lib/content-type-labels";
import { DRAFT_STATUS_BADGE_VARIANT, DRAFT_STATUS_LABELS } from "@/lib/draft-status-labels";
import { deleteDraft, duplicateDraft, finalizeDraft, restoreDraft, updateDraft } from "@/lib/api/drafts";
import { submitForApproval } from "@/lib/api/approval";
import { ApiError } from "@/lib/api/client";
import type { ContentDraft } from "@/types/drafts";
import { DraftPreviewSheet } from "@/components/drafts/draft-preview-sheet";
import { DraftEditSheet } from "@/components/drafts/draft-edit-sheet";

interface DraftCardProps {
  draft: ContentDraft;
  layout: "grid" | "list";
  onChanged: (updated: ContentDraft) => void;
  onDeleted: (id: string) => void;
  onDuplicated: (created: ContentDraft) => void;
}

export function DraftCard({ draft, layout, onChanged, onDeleted, onDuplicated }: DraftCardProps) {
  const [busy, setBusy] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);

  async function withBusy(key: string, fn: () => Promise<void>) {
    setBusy(key);
    setActionError(null);
    try {
      await fn();
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : "Something went wrong. Please try again.");
    } finally {
      setBusy(null);
    }
  }

  async function handleDuplicate() {
    await withBusy("duplicate", async () => {
      const response = await duplicateDraft(draft.id);
      if (response.data) onDuplicated(response.data);
    });
  }

  async function handleDelete() {
    await withBusy("delete", async () => {
      await deleteDraft(draft.id);
      onDeleted(draft.id);
    });
  }

  async function handleRestore() {
    await withBusy("restore", async () => {
      const response = await restoreDraft(draft.id);
      if (response.data) onChanged(response.data);
    });
  }

  async function handleFinalize() {
    await withBusy("finalize", async () => {
      const response = await finalizeDraft(draft.id);
      if (response.data) onChanged(response.data);
    });
  }

  async function handleMoveToDraft() {
    await withBusy("unfinalize", async () => {
      const response = await updateDraft(draft.id, { contentText: draft.contentText, title: draft.title, status: "DRAFT" });
      if (response.data) onChanged(response.data);
    });
  }

  async function handleSubmitForApproval() {
    await withBusy("submit", async () => {
      const response = await submitForApproval(draft.id);
      if (response.data) {
        onChanged({ ...draft, status: "READY_FOR_REVIEW" });
      }
    });
  }

  return (
    <Card className={cn(layout === "list" && "sm:flex-row sm:items-center")}>
      <CardHeader className={cn(layout === "list" && "sm:w-72 sm:shrink-0")}>
        <div className="flex flex-wrap items-center gap-2">
          <CardTitle className="text-sm">{draft.title}</CardTitle>
        </div>
        <div className="flex flex-wrap items-center gap-1.5 pt-1">
          <Badge variant={DRAFT_STATUS_BADGE_VARIANT[draft.status]}>{DRAFT_STATUS_LABELS[draft.status]}</Badge>
          <Badge variant="outline">{CONTENT_TYPE_LABELS[draft.contentType]}</Badge>
          {draft.deleted && <Badge variant="destructive">Deleted</Badge>}
        </div>
      </CardHeader>

      <CardContent className="flex-1 flex flex-col gap-2">
        {actionError && (
          <Alert variant="destructive">
            <AlertDescription>{actionError}</AlertDescription>
          </Alert>
        )}
        <p className={cn("whitespace-pre-wrap text-sm text-muted-foreground", layout === "grid" ? "line-clamp-4" : "line-clamp-2")}>
          {draft.contentText}
        </p>
      </CardContent>

      <CardFooter className="flex flex-wrap gap-1.5">
        <Button variant="ghost" size="sm" onClick={() => setPreviewOpen(true)}>
          <Eye className="size-3.5" />
          Preview
        </Button>

        {!draft.deleted && (
          <>
            <Button variant="ghost" size="sm" onClick={() => setEditOpen(true)}>
              <Pencil className="size-3.5" />
              Edit
            </Button>
            <Button variant="ghost" size="sm" onClick={handleDuplicate} disabled={busy === "duplicate"}>
              {busy === "duplicate" ? <Loader2 className="size-3.5 animate-spin" /> : <CopyCheck className="size-3.5" />}
              Duplicate
            </Button>
            {draft.status === "DRAFT" && (
              <Button variant="ghost" size="sm" onClick={handleSubmitForApproval} disabled={busy === "submit"}>
                {busy === "submit" ? <Loader2 className="size-3.5 animate-spin" /> : <Send className="size-3.5" />}
                Submit for Approval
              </Button>
            )}
            {draft.status === "APPROVED" ? (
              <Button variant="ghost" size="sm" onClick={handleMoveToDraft} disabled={busy === "unfinalize"}>
                {busy === "unfinalize" ? <Loader2 className="size-3.5 animate-spin" /> : <Undo2 className="size-3.5" />}
                Move to Draft
              </Button>
            ) : (
              <Button variant="ghost" size="sm" onClick={handleFinalize} disabled={busy === "finalize"}>
                {busy === "finalize" ? <Loader2 className="size-3.5 animate-spin" /> : <CheckCircle2 className="size-3.5" />}
                Finalize
              </Button>
            )}
            <AlertDialog>
              <AlertDialogTrigger render={<Button variant="ghost" size="sm" className="text-destructive" />}>
                <Trash2 className="size-3.5" />
                Delete
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Delete this draft?</AlertDialogTitle>
                  <AlertDialogDescription>
                    &quot;{draft.title}&quot; will be moved to trash. You can restore it later.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel disabled={busy === "delete"}>Cancel</AlertDialogCancel>
                  <AlertDialogAction onClick={handleDelete} disabled={busy === "delete"}>
                    {busy === "delete" && <Loader2 className="size-4 animate-spin" />}
                    Delete
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </>
        )}

        {draft.deleted && (
          <Button variant="ghost" size="sm" onClick={handleRestore} disabled={busy === "restore"}>
            {busy === "restore" ? <Loader2 className="size-3.5 animate-spin" /> : <RotateCcw className="size-3.5" />}
            Restore
          </Button>
        )}
      </CardFooter>

      <DraftPreviewSheet draft={draft} open={previewOpen} onOpenChange={setPreviewOpen} />
      <DraftEditSheet draft={draft} open={editOpen} onOpenChange={setEditOpen} onSaved={onChanged} />
    </Card>
  );
}
