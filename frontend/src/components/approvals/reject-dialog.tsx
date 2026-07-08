"use client";

import { useState } from "react";
import { Loader2, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { rejectApproval } from "@/lib/api/approval";
import { ApiError } from "@/lib/api/client";
import type { Approval } from "@/types/approval";

interface RejectDialogProps {
  approval: Approval;
  onRejected: (updated: Approval) => void;
  size?: "sm" | "default";
}

export function RejectDialog({ approval, onRejected, size = "sm" }: RejectDialogProps) {
  const [open, setOpen] = useState(false);
  const [remarks, setRemarks] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleReject() {
    if (!remarks.trim()) {
      setError("Please explain why this content is being rejected.");
      return;
    }
    setIsSaving(true);
    setError(null);
    try {
      const response = await rejectApproval(approval.id, remarks);
      if (response.data) {
        onRejected(response.data);
        setOpen(false);
        setRemarks("");
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not reject. Please try again.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<Button size={size} variant="outline" className="text-destructive" />}>
        <XCircle className="size-3.5" />
        Reject
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Reject this content?</DialogTitle>
          <DialogDescription>
            &quot;{approval.contentTitle}&quot; will be sent back to Draft for revision.
          </DialogDescription>
        </DialogHeader>
        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        <div className="flex flex-col gap-2">
          <Label>Reason for rejection (required)</Label>
          <Textarea rows={3} value={remarks} onChange={(e) => setRemarks(e.target.value)} placeholder="Needs a stronger call to action..." />
        </div>
        <DialogFooter>
          <Button variant="destructive" onClick={handleReject} disabled={isSaving}>
            {isSaving && <Loader2 className="size-4 animate-spin" />}
            Confirm Rejection
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
