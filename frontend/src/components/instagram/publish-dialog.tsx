"use client";

import { useCallback, useEffect, useState } from "react";
import { Loader2, Send, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { getDraft } from "@/lib/api/drafts";
import { listMedia } from "@/lib/api/media";
import { publishToInstagram, publishToInstagramMock } from "@/lib/api/instagram";
import { ApiError } from "@/lib/api/client";
import type { Approval } from "@/types/approval";
import type { MediaItem } from "@/types/media";
import type { InstagramPost } from "@/types/instagram";

interface PublishDialogProps {
  approval: Approval;
  onPublished: (post: InstagramPost) => void;
}

export function PublishDialog({ approval, onPublished }: PublishDialogProps) {
  const [open, setOpen] = useState(false);
  const [media, setMedia] = useState<MediaItem[]>([]);
  const [mediaId, setMediaId] = useState<string>("");
  const [caption, setCaption] = useState("");
  const [hashtags, setHashtags] = useState("");
  const [isLoadingDraft, setIsLoadingDraft] = useState(false);
  const [isPublishing, setIsPublishing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadMedia = useCallback(async () => {
    try {
      const response = await listMedia(0, 100);
      setMedia(response.data?.content ?? []);
    } catch {
      setMedia([]);
    }
  }, []);

  const loadDraftCaption = useCallback(async (contentId: string) => {
    setIsLoadingDraft(true);
    try {
      const response = await getDraft(contentId);
      if (response.data) setCaption(response.data.contentText);
    } finally {
      setIsLoadingDraft(false);
    }
  }, []);

  const loadDialogData = useCallback(async () => {
    await loadMedia();
    if (approval.contentId && !caption) {
      await loadDraftCaption(approval.contentId);
    }
    // caption is intentionally excluded - it becomes user-edited text after the initial
    // load, and re-running this on every keystroke would refetch and clobber it.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [approval.contentId, loadMedia, loadDraftCaption]);

  useEffect(() => {
    if (!open) return;
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadDialogData();
  }, [open, loadDialogData]);

  async function handlePublish(useMock: boolean) {
    if (!mediaId || !caption.trim()) {
      setError("Select a media item and make sure the caption is not empty.");
      return;
    }
    setIsPublishing(true);
    setError(null);
    try {
      const publishFn = useMock ? publishToInstagramMock : publishToInstagram;
      const response = await publishFn({
        approvalId: approval.id,
        mediaId,
        caption,
        hashtags: hashtags || undefined,
      });
      if (response.data) {
        onPublished(response.data);
        setOpen(false);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Publishing failed. Please try again.");
    } finally {
      setIsPublishing(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<Button size="sm" />}>
        <Send className="size-3.5" />
        Publish
      </DialogTrigger>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>Publish to Instagram</DialogTitle>
          <DialogDescription>{approval.contentTitle}</DialogDescription>
        </DialogHeader>

        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <div className="flex flex-col gap-4">
          <div className="flex flex-col gap-2">
            <Label>Media</Label>
            <Select value={mediaId} onValueChange={(value) => setMediaId(value ?? "")}>
              <SelectTrigger>
                <SelectValue placeholder="Select an image" />
              </SelectTrigger>
              <SelectContent>
                {media.map((item) => (
                  <SelectItem key={item.id} value={item.id}>
                    {item.fileName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex flex-col gap-2">
            <Label>Final Caption</Label>
            {isLoadingDraft ? (
              <div className="flex items-center gap-2 text-sm text-muted-foreground">
                <Loader2 className="size-3.5 animate-spin" />
                Loading content...
              </div>
            ) : (
              <Textarea rows={6} value={caption} onChange={(e) => setCaption(e.target.value)} />
            )}
          </div>

          <div className="flex flex-col gap-2">
            <Label>Hashtags (optional)</Label>
            <Input value={hashtags} onChange={(e) => setHashtags(e.target.value)} placeholder="#shooting #academy" />
          </div>

          {(caption || hashtags) && (
            <div className="rounded-lg border bg-muted/30 p-3">
              <p className="mb-1 text-xs font-medium text-muted-foreground">Preview</p>
              <p className="whitespace-pre-wrap text-sm">
                {caption}
                {hashtags ? `\n\n${hashtags}` : ""}
              </p>
            </div>
          )}
        </div>

        <DialogFooter className="flex-col gap-2 sm:flex-row">
          <Button variant="outline" onClick={() => handlePublish(true)} disabled={isPublishing}>
            {isPublishing ? <Loader2 className="size-4 animate-spin" /> : <Sparkles className="size-4" />}
            Mock Publish
          </Button>
          <Button onClick={() => handlePublish(false)} disabled={isPublishing}>
            {isPublishing ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
            Publish
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
