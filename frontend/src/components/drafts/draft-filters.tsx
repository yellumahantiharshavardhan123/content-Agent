"use client";

import { LayoutGrid, List, Trash2 } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { CONTENT_TYPE_LABELS, CONTENT_TYPES } from "@/lib/content-type-labels";
import { DRAFT_STATUS_LABELS, DRAFT_STATUSES } from "@/lib/draft-status-labels";
import type { ContentType } from "@/types/ai";
import type { DraftStatus } from "@/types/drafts";

const ALL = "__all__";

interface DraftFiltersProps {
  search: string;
  onSearchChange: (value: string) => void;
  status: DraftStatus | null;
  onStatusChange: (value: DraftStatus | null) => void;
  contentType: ContentType | null;
  onContentTypeChange: (value: ContentType | null) => void;
  showDeleted: boolean;
  onShowDeletedChange: (value: boolean) => void;
  layout: "grid" | "list";
  onLayoutChange: (value: "grid" | "list") => void;
}

export function DraftFilters({
  search,
  onSearchChange,
  status,
  onStatusChange,
  contentType,
  onContentTypeChange,
  showDeleted,
  onShowDeletedChange,
  layout,
  onLayoutChange,
}: DraftFiltersProps) {
  return (
    <div className="flex flex-wrap items-center gap-3">
      <Input
        className="w-full sm:w-64"
        placeholder="Search drafts..."
        value={search}
        onChange={(e) => onSearchChange(e.target.value)}
      />

      <Select value={status ?? ALL} onValueChange={(value) => onStatusChange(value === ALL ? null : (value as DraftStatus))}>
        <SelectTrigger className="w-full sm:w-48">
          <SelectValue placeholder="All statuses" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ALL}>All statuses</SelectItem>
          {DRAFT_STATUSES.map((s) => (
            <SelectItem key={s} value={s}>
              {DRAFT_STATUS_LABELS[s]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Select
        value={contentType ?? ALL}
        onValueChange={(value) => onContentTypeChange(value === ALL ? null : (value as ContentType))}
      >
        <SelectTrigger className="w-full sm:w-56">
          <SelectValue placeholder="All content types" />
        </SelectTrigger>
        <SelectContent>
          <SelectItem value={ALL}>All content types</SelectItem>
          {CONTENT_TYPES.map((type) => (
            <SelectItem key={type} value={type}>
              {CONTENT_TYPE_LABELS[type]}
            </SelectItem>
          ))}
        </SelectContent>
      </Select>

      <Button
        variant={showDeleted ? "secondary" : "outline"}
        size="sm"
        onClick={() => onShowDeletedChange(!showDeleted)}
      >
        <Trash2 className="size-3.5" />
        {showDeleted ? "Showing trash" : "Show trash"}
      </Button>

      <div className="ml-auto flex items-center gap-1 rounded-md border p-0.5">
        <Button
          variant={layout === "grid" ? "secondary" : "ghost"}
          size="icon-sm"
          aria-label="Card view"
          onClick={() => onLayoutChange("grid")}
        >
          <LayoutGrid className="size-4" />
        </Button>
        <Button
          variant={layout === "list" ? "secondary" : "ghost"}
          size="icon-sm"
          aria-label="List view"
          onClick={() => onLayoutChange("list")}
        >
          <List className="size-4" />
        </Button>
      </div>
    </div>
  );
}
