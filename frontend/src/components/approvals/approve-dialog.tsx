"use client";

import { useState } from "react";
import { CheckCircle2, Loader2 } from "lucide-react";
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
import { approveApproval } from "@/lib/api/approval";
import { ApiError } from "@/lib/api/client";
import type { Approval } from "@/types/approval";

interface ApproveDialogProps {
  approval: Approval;
  onApproved: (updated: Approval) => void;
  size?: "sm" | "default";
}

export function ApproveDialog({ approval, onApproved, size = "sm" }: ApproveDialogProps) {
  const [open, setOpen] = useState(false);
  const [remarks, setRemarks] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleApprove() {
    setIsSaving(true);
    setError(null);
    try {
      const response = await approveApproval(approval.id, remarks || undefined);
      if (response.data) {
        onApproved(response.data);
        setOpen(false);
        setRemarks("");
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not approve. Please try again.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger render={<Button size={size} />}>
        <CheckCircle2 className="size-3.5" />
        Approve
      </DialogTrigger>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Approve this content?</DialogTitle>
          <DialogDescription>
            &quot;{approval.contentTitle}&quot; will move to Ready for Publish.
          </DialogDescription>
        </DialogHeader>
        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        <div className="flex flex-col gap-2">
          <Label>Remarks (optional)</Label>
          <Textarea rows={3} value={remarks} onChange={(e) => setRemarks(e.target.value)} placeholder="Looks great, approved!" />
        </div>
        <DialogFooter>
          <Button onClick={handleApprove} disabled={isSaving}>
            {isSaving && <Loader2 className="size-4 animate-spin" />}
            Confirm Approval
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
