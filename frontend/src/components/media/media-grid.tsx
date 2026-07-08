"use client";

import { ImageOff } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { MediaCard } from "@/components/media/media-card";
import type { MediaItem } from "@/types/media";

interface MediaGridProps {
  items: MediaItem[];
  isLoading: boolean;
  onDeleted: () => void;
}

export function MediaGrid({ items, isLoading, onDeleted }: MediaGridProps) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
        {Array.from({ length: 10 }).map((_, i) => (
          <Skeleton key={i} className="aspect-square w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-2 py-16 text-center text-muted-foreground">
        <ImageOff className="size-8" />
        <p className="text-sm">No media uploaded yet.</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5">
      {items.map((item) => (
        <MediaCard key={item.id} media={item} onDeleted={onDeleted} />
      ))}
    </div>
  );
}
