"use client";

import { Sparkles } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { GeneratedContentCard } from "@/components/ai/generated-content-card";
import type { GeneratedContent } from "@/types/ai";

interface GeneratedContentListProps {
  items: GeneratedContent[];
  isLoading: boolean;
  onChanged: (updated: GeneratedContent) => void;
  onDeleted: (id: string) => void;
}

export function GeneratedContentList({ items, isLoading, onChanged, onDeleted }: GeneratedContentListProps) {
  if (isLoading) {
    return (
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-48 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-2 py-16 text-center text-muted-foreground">
        <Sparkles className="size-8" />
        <p className="text-sm">No content generated yet.</p>
      </div>
    );
  }

  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {items.map((item) => (
        <GeneratedContentCard key={item.id} content={item} onChanged={onChanged} onDeleted={onDeleted} />
      ))}
    </div>
  );
}
