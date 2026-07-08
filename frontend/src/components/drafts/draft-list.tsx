"use client";

import { FileText } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { DraftCard } from "@/components/drafts/draft-card";
import type { ContentDraft } from "@/types/drafts";

interface DraftListProps {
  items: ContentDraft[];
  isLoading: boolean;
  layout: "grid" | "list";
  onChanged: (updated: ContentDraft) => void;
  onDeleted: (id: string) => void;
  onDuplicated: (created: ContentDraft) => void;
}

export function DraftList({ items, isLoading, layout, onChanged, onDeleted, onDuplicated }: DraftListProps) {
  if (isLoading) {
    const wrapperClass = layout === "grid" ? "grid gap-4 sm:grid-cols-2 xl:grid-cols-3" : "flex flex-col gap-3";
    return (
      <div className={wrapperClass}>
        {Array.from({ length: 6 }).map((_, i) => (
          <Skeleton key={i} className="h-48 w-full rounded-lg" />
        ))}
      </div>
    );
  }

  if (items.length === 0) {
    return (
      <div className="flex flex-1 flex-col items-center justify-center gap-2 py-16 text-center text-muted-foreground">
        <FileText className="size-8" />
        <p className="text-sm">No drafts found.</p>
      </div>
    );
  }

  return (
    <div className={layout === "grid" ? "grid gap-4 sm:grid-cols-2 xl:grid-cols-3" : "flex flex-col gap-3"}>
      {items.map((item) => (
        <DraftCard
          key={item.id}
          draft={item}
          layout={layout}
          onChanged={onChanged}
          onDeleted={onDeleted}
          onDuplicated={onDuplicated}
        />
      ))}
    </div>
  );
}
