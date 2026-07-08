"use client";

import { useCallback, useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { UploadDropzone } from "@/components/media/upload-dropzone";
import { MediaGrid } from "@/components/media/media-grid";
import { listMedia } from "@/lib/api/media";
import type { MediaItem, MediaType } from "@/types/media";

const TYPE_FILTERS: { label: string; value: MediaType | "ALL" }[] = [
  { label: "All types", value: "ALL" },
  { label: "Images", value: "IMAGE" },
  { label: "Videos", value: "VIDEO" },
  { label: "PDFs", value: "PDF" },
  { label: "Text notes", value: "TEXT_NOTE" },
];

export default function MediaPage() {
  const [items, setItems] = useState<MediaItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [typeFilter, setTypeFilter] = useState<MediaType | "ALL">("ALL");

  const refresh = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await listMedia(0, 48, typeFilter === "ALL" ? undefined : typeFilter);
      setItems(response.data?.content ?? []);
    } finally {
      setIsLoading(false);
    }
  }, [typeFilter]);

  useEffect(() => {
    // Initial load and reload whenever the type filter changes.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    refresh();
  }, [refresh]);

  return (
    <div className="flex flex-1 flex-col gap-6 p-6">
      <div>
        <h2 className="text-2xl font-semibold tracking-tight">Media Library</h2>
        <p className="text-muted-foreground">
          Upload images, videos, PDFs, and text notes to use as source material for AI content generation.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Upload new media</CardTitle>
        </CardHeader>
        <CardContent>
          <UploadDropzone onUploaded={refresh} />
        </CardContent>
      </Card>

      <div className="flex items-center justify-between">
        <h3 className="text-lg font-medium">Library</h3>
        <Select value={typeFilter} onValueChange={(value) => setTypeFilter(value as MediaType | "ALL")}>
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            {TYPE_FILTERS.map((filter) => (
              <SelectItem key={filter.value} value={filter.value}>
                {filter.label}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <MediaGrid items={items} isLoading={isLoading} onDeleted={refresh} />
    </div>
  );
}
