"use client";

import { useState } from "react";
import { Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Sheet, SheetContent, SheetFooter, SheetHeader, SheetTitle } from "@/components/ui/sheet";
import { updateDraft } from "@/lib/api/drafts";
import { ApiError } from "@/lib/api/client";
import type { ContentDraft } from "@/types/drafts";

interface DraftEditSheetProps {
  draft: ContentDraft;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSaved: (updated: ContentDraft) => void;
}

export function DraftEditSheet({ draft, open, onOpenChange, onSaved }: DraftEditSheetProps) {
  const [title, setTitle] = useState(draft.title);
  const [contentText, setContentText] = useState(draft.contentText);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Re-seed the editable fields from the latest draft each time the sheet transitions to open,
  // following React's documented render-phase pattern instead of an effect (the `open` prop is
  // flipped by the parent, so there is no user-driven event to hook a handler onto).
  const [wasOpen, setWasOpen] = useState(open);
  if (open !== wasOpen) {
    setWasOpen(open);
    if (open) {
      setTitle(draft.title);
      setContentText(draft.contentText);
      setError(null);
    }
  }

  async function handleSave() {
    setIsSaving(true);
    setError(null);
    try {
      const response = await updateDraft(draft.id, { title, contentText, status: draft.status });
      if (response.data) {
        onSaved(response.data);
        onOpenChange(false);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save changes. Please try again.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="sm:max-w-lg">
        <SheetHeader>
          <SheetTitle>Edit draft</SheetTitle>
        </SheetHeader>
        <div className="flex flex-1 flex-col gap-4 overflow-y-auto px-4">
          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}
          <div className="flex flex-col gap-2">
            <Label>Title</Label>
            <Input value={title} onChange={(e) => setTitle(e.target.value)} />
          </div>
          <div className="flex flex-1 flex-col gap-2">
            <Label>Content</Label>
            <Textarea rows={12} value={contentText} onChange={(e) => setContentText(e.target.value)} />
          </div>
        </div>
        <SheetFooter>
          <Button onClick={handleSave} disabled={isSaving}>
            {isSaving && <Loader2 className="size-4 animate-spin" />}
            Save changes
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
