"use client";

import { useEffect, useState } from "react";
import { Loader2, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { CONTENT_TYPE_LABELS, CONTENT_TYPES } from "@/lib/content-type-labels";
import { generateContent } from "@/lib/api/ai";
import { listMedia } from "@/lib/api/media";
import { ApiError } from "@/lib/api/client";
import type { ContentType, GeneratedContent } from "@/types/ai";
import type { MediaItem } from "@/types/media";

interface GenerationFormProps {
  onGenerated: (content: GeneratedContent) => void;
}

const NO_MEDIA = "__none__";

export function GenerationForm({ onGenerated }: GenerationFormProps) {
  const [media, setMedia] = useState<MediaItem[]>([]);
  const [mediaId, setMediaId] = useState<string>(NO_MEDIA);
  const [contentType, setContentType] = useState<ContentType>("INSTAGRAM_CAPTION");
  const [manualNotes, setManualNotes] = useState("");
  const [eventDetails, setEventDetails] = useState("");
  const [achievement, setAchievement] = useState("");
  const [competitionResults, setCompetitionResults] = useState("");
  const [trainingSession, setTrainingSession] = useState("");
  const [coachNotes, setCoachNotes] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listMedia(0, 100)
      .then((response) => setMedia(response.data?.content ?? []))
      .catch(() => setMedia([]));
  }, []);

  async function handleGenerate() {
    setIsGenerating(true);
    setError(null);
    try {
      const response = await generateContent({
        mediaId: mediaId === NO_MEDIA ? null : mediaId,
        contentType,
        manualNotes: manualNotes || undefined,
        eventDetails: eventDetails || undefined,
        achievement: achievement || undefined,
        competitionResults: competitionResults || undefined,
        trainingSession: trainingSession || undefined,
        coachNotes: coachNotes || undefined,
      });
      if (response.data) {
        onGenerated(response.data);
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Generation failed. Please try again.");
    } finally {
      setIsGenerating(false);
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Generate content</CardTitle>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {error && (
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-2">
            <Label>Source media (optional)</Label>
            <Select value={mediaId} onValueChange={(value) => setMediaId(value ?? NO_MEDIA)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NO_MEDIA}>No media - use context only</SelectItem>
                {media.map((item) => (
                  <SelectItem key={item.id} value={item.id}>
                    {item.fileName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex flex-col gap-2">
            <Label>Content type</Label>
            <Select value={contentType} onValueChange={(value) => setContentType(value as ContentType)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {CONTENT_TYPES.map((type) => (
                  <SelectItem key={type} value={type}>
                    {CONTENT_TYPE_LABELS[type]}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2">
          <div className="flex flex-col gap-2">
            <Label>Manual notes</Label>
            <Textarea rows={2} value={manualNotes} onChange={(e) => setManualNotes(e.target.value)} />
          </div>
          <div className="flex flex-col gap-2">
            <Label>Event details</Label>
            <Textarea rows={2} value={eventDetails} onChange={(e) => setEventDetails(e.target.value)} />
          </div>
          <div className="flex flex-col gap-2">
            <Label>Achievement</Label>
            <Textarea rows={2} value={achievement} onChange={(e) => setAchievement(e.target.value)} />
          </div>
          <div className="flex flex-col gap-2">
            <Label>Competition results</Label>
            <Textarea rows={2} value={competitionResults} onChange={(e) => setCompetitionResults(e.target.value)} />
          </div>
          <div className="flex flex-col gap-2">
            <Label>Training session</Label>
            <Textarea rows={2} value={trainingSession} onChange={(e) => setTrainingSession(e.target.value)} />
          </div>
          <div className="flex flex-col gap-2">
            <Label>Coach notes</Label>
            <Textarea rows={2} value={coachNotes} onChange={(e) => setCoachNotes(e.target.value)} />
          </div>
        </div>

        <Button onClick={handleGenerate} disabled={isGenerating} className="self-start">
          {isGenerating ? <Loader2 className="size-4 animate-spin" /> : <Sparkles className="size-4" />}
          {isGenerating ? "Generating..." : "Generate"}
        </Button>
      </CardContent>
    </Card>
  );
}
