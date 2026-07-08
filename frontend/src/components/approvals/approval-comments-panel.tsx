"use client";

import { useState } from "react";
import { Loader2, Send } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { addApprovalComment } from "@/lib/api/approval";
import { ApiError } from "@/lib/api/client";
import type { Approval, ApprovalComment } from "@/types/approval";

interface ApprovalCommentsPanelProps {
  approval: Approval;
  onCommentAdded: (comment: ApprovalComment) => void;
}

export function ApprovalCommentsPanel({ approval, onCommentAdded }: ApprovalCommentsPanelProps) {
  const [text, setText] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit() {
    if (!text.trim()) return;
    setIsSaving(true);
    setError(null);
    try {
      const response = await addApprovalComment(approval.id, text);
      if (response.data) {
        onCommentAdded(response.data);
        setText("");
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not post comment. Please try again.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <div className="flex flex-col gap-4">
      {approval.comments.length === 0 ? (
        <p className="text-sm text-muted-foreground">No comments yet.</p>
      ) : (
        <ul className="flex flex-col gap-3">
          {approval.comments.map((comment) => (
            <li key={comment.id} className="rounded-lg border p-3">
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm font-medium">{comment.authorEmail ?? "Unknown"}</span>
                <span className="text-xs text-muted-foreground">{new Date(comment.createdAt).toLocaleString()}</span>
              </div>
              <p className="mt-1 text-sm text-muted-foreground">{comment.comment}</p>
            </li>
          ))}
        </ul>
      )}

      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <div className="flex flex-col gap-2">
        <Textarea
          rows={3}
          placeholder="Leave a comment for the submitter or reviewer..."
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <Button size="sm" className="self-start" onClick={handleSubmit} disabled={isSaving || !text.trim()}>
          {isSaving ? <Loader2 className="size-3.5 animate-spin" /> : <Send className="size-3.5" />}
          Post Comment
        </Button>
      </div>
    </div>
  );
}
