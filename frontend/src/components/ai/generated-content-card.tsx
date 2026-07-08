"use client";

import { useState } from "react";
import { Check, Copy, Eye, Loader2, Pencil, RefreshCw, Trash2 } from "lucide-react";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
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
import { CONTENT_TYPE_LABELS } from "@/lib/content-type-labels";
import { deleteContent, regenerateContent, updateContent } from "@/lib/api/ai";
import type { GeneratedContent } from "@/types/ai";

interface GeneratedContentCardProps {
  content: GeneratedContent;
  onChanged: (updated: GeneratedContent) => void;
  onDeleted: (id: string) => void;
}

export function GeneratedContentCard({ content, onChanged, onDeleted }: GeneratedContentCardProps) {
  const [isRegenerating, setIsRegenerating] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isSavingDraft, setIsSavingDraft] = useState(false);
  const [copied, setCopied] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [editText, setEditText] = useState(content.generatedText ?? "");
  const [isSavingEdit, setIsSavingEdit] = useState(false);

  async function handleRegenerate() {
    setIsRegenerating(true);
    try {
      const response = await regenerateContent(content.id);
      if (response.data) onChanged(response.data);
    } finally {
      setIsRegenerating(false);
    }
  }

  async function handleCopy() {
    await navigator.clipboard.writeText(content.generatedText ?? "");
    setCopied(true);
    setTimeout(() => setCopied(false), 1500);
  }

  async function handleToggleDraft() {
    setIsSavingDraft(true);
    try {
      const response = await updateContent(content.id, content.generatedText ?? "", !content.draft);
      if (response.data) onChanged(response.data);
    } finally {
      setIsSavingDraft(false);
    }
  }

  async function handleSaveEdit() {
    setIsSavingEdit(true);
    try {
      const response = await updateContent(content.id, editText, content.draft);
      if (response.data) onChanged(response.data);
      setEditOpen(false);
    } finally {
      setIsSavingEdit(false);
    }
  }

  async function handleDelete() {
    setIsDeleting(true);
    try {
      await deleteContent(content.id);
      onDeleted(content.id);
    } finally {
      setIsDeleting(false);
    }
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-wrap items-center gap-2">
          <CardTitle className="text-sm">{CONTENT_TYPE_LABELS[content.contentType]}</CardTitle>
          <Badge variant={content.status === "SUCCESS" ? "default" : "destructive"}>{content.status}</Badge>
          <Badge
            variant="secondary"
            className="cursor-pointer"
            onClick={handleToggleDraft}
            aria-disabled={isSavingDraft}
          >
            {isSavingDraft ? <Loader2 className="size-3 animate-spin" /> : null}
            {content.draft ? "Draft" : "Final"}
          </Badge>
          {content.edited && <Badge variant="outline">Edited</Badge>}
        </div>
      </CardHeader>
      <CardContent>
        {content.status === "SUCCESS" ? (
          <p className="line-clamp-4 whitespace-pre-wrap text-sm text-muted-foreground">{content.generatedText}</p>
        ) : (
          <p className="text-sm text-destructive">{content.errorMessage ?? "Generation failed"}</p>
        )}
      </CardContent>
      <CardFooter className="flex flex-wrap gap-1.5">
        <Button variant="ghost" size="sm" onClick={() => setPreviewOpen(true)}>
          <Eye className="size-3.5" />
          Preview
        </Button>
        <Button variant="ghost" size="sm" onClick={handleCopy} disabled={!content.generatedText}>
          {copied ? <Check className="size-3.5" /> : <Copy className="size-3.5" />}
          {copied ? "Copied" : "Copy"}
        </Button>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => {
            setEditText(content.generatedText ?? "");
            setEditOpen(true);
          }}
        >
          <Pencil className="size-3.5" />
          Edit
        </Button>
        <Button variant="ghost" size="sm" onClick={handleRegenerate} disabled={isRegenerating}>
          {isRegenerating ? <Loader2 className="size-3.5 animate-spin" /> : <RefreshCw className="size-3.5" />}
          Regenerate
        </Button>
        <AlertDialog>
          <AlertDialogTrigger render={<Button variant="ghost" size="sm" className="text-destructive" />}>
            <Trash2 className="size-3.5" />
            Delete
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Delete this content?</AlertDialogTitle>
              <AlertDialogDescription>
                This generated {CONTENT_TYPE_LABELS[content.contentType].toLowerCase()} will be permanently deleted.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={isDeleting}>Cancel</AlertDialogCancel>
              <AlertDialogAction onClick={handleDelete} disabled={isDeleting}>
                {isDeleting && <Loader2 className="size-4 animate-spin" />}
                Delete
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </CardFooter>

      <Dialog open={previewOpen} onOpenChange={setPreviewOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>{CONTENT_TYPE_LABELS[content.contentType]}</DialogTitle>
            <DialogDescription>
              {content.aiModel ? `Generated with ${content.aiModel}` : "Generation preview"}
            </DialogDescription>
          </DialogHeader>
          <p className="max-h-96 overflow-y-auto whitespace-pre-wrap text-sm">{content.generatedText}</p>
        </DialogContent>
      </Dialog>

      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>Edit content</DialogTitle>
          </DialogHeader>
          <Textarea rows={10} value={editText} onChange={(e) => setEditText(e.target.value)} />
          <DialogFooter>
            <Button onClick={handleSaveEdit} disabled={isSavingEdit}>
              {isSavingEdit && <Loader2 className="size-4 animate-spin" />}
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </Card>
  );
}
