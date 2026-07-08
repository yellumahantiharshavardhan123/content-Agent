"use client";

import { useEffect, useState } from "react";
import { Loader2, PlusCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { CONTENT_TYPE_LABELS } from "@/lib/content-type-labels";
import { createDraft } from "@/lib/api/drafts";
import { listContent } from "@/lib/api/ai";
import { ApiError } from "@/lib/api/client";
import type { GeneratedContent } from "@/types/ai";
import type { ContentDraft } from "@/types/drafts";

interface DraftCreateFormProps {
  onCreated: (draft: ContentDraft) => void;
}

export function DraftCreateForm({ onCreated }: DraftCreateFormProps) {
  const [sources, setSources] = useState<GeneratedContent[]>([]);
  const [sourceId, setSourceId] = useState<string>("");
  const [title, setTitle] = useState("");
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listContent(0, 100)
      .then((response) => {
        const withText = (response.data?.content ?? []).filter((item) => item.status === "SUCCESS" && item.generatedText);
        setSources(withText);
        if (withText.length > 0) setSourceId(withText[0].id);
      })
      .catch(() => setSources([]));
  }, []);

  async function handleCreate() {
    if (!sourceId) {
      setError("Generate some content first, then come back here to save it as a draft.");
      return;
    }
    setIsSaving(true);
    setError(null);
    try {
      const response = await createDraft({ generatedContentId: sourceId, title: title || undefined });
      if (response.data) {
        onCreated(response.data);
        setTitle("");
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not save draft. Please try again.");
    } finally {
      setIsSaving(false);
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Save a draft</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-2">
            <Label>Source generated content</Label>
            <Select value={sourceId} onValueChange={(value) => setSourceId(value ?? "")}>
              <SelectTrigger>
                <SelectValue placeholder="Select generated content" />
              </SelectTrigger>
              <SelectContent>
                {sources.map((item) => (
                  <SelectItem key={item.id} value={item.id}>
                    {CONTENT_TYPE_LABELS[item.contentType]} - {(item.generatedText ?? "").slice(0, 40)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="flex flex-col gap-2">
            <Label>Title (optional)</Label>
            <Input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="e.g. Regional Championship Recap" />
          </div>
        </div>
        <Button onClick={handleCreate} disabled={isSaving} className="self-start">
          {isSaving ? <Loader2 className="size-4 animate-spin" /> : <PlusCircle className="size-4" />}
          {isSaving ? "Saving..." : "Save as draft"}
        </Button>
      </CardContent>
    </Card>
  );
}
