"use client";

import { useCallback, useEffect, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { DraftCreateForm } from "@/components/drafts/draft-create-form";
import { DraftFilters } from "@/components/drafts/draft-filters";
import { DraftList } from "@/components/drafts/draft-list";
import { listDrafts } from "@/lib/api/drafts";
import type { ContentType } from "@/types/ai";
import type { ContentDraft, DraftStatus } from "@/types/drafts";

const PAGE_SIZE = 12;

export default function DraftsPage() {
  const [items, setItems] = useState<ContentDraft[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [layout, setLayout] = useState<"grid" | "list">("grid");

  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<DraftStatus | null>(null);
  const [contentType, setContentType] = useState<ContentType | null>(null);
  const [showDeleted, setShowDeleted] = useState(false);

  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  useEffect(() => {
    const handle = setTimeout(() => {
      setSearch(searchInput);
      setPage(0);
    }, 300);
    return () => clearTimeout(handle);
  }, [searchInput]);

  function handleStatusChange(value: DraftStatus | null) {
    setStatus(value);
    setPage(0);
  }

  function handleContentTypeChange(value: ContentType | null) {
    setContentType(value);
    setPage(0);
  }

  function handleShowDeletedChange(value: boolean) {
    setShowDeleted(value);
    setPage(0);
  }

  const refresh = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await listDrafts({
        page,
        size: PAGE_SIZE,
        search: search || undefined,
        status: status ?? undefined,
        contentType: contentType ?? undefined,
        includeDeleted: showDeleted,
      });
      setItems(response.data?.content ?? []);
      setTotalPages(response.data?.totalPages ?? 0);
      setTotalElements(response.data?.totalElements ?? 0);
    } finally {
      setIsLoading(false);
    }
  }, [page, search, status, contentType, showDeleted]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    refresh();
  }, [refresh]);

  function handleCreated(draft: ContentDraft) {
    setPage(0);
    setItems((prev) => [draft, ...prev]);
    setTotalElements((prev) => prev + 1);
  }

  function handleChanged(updated: ContentDraft) {
    if (!showDeleted && updated.deleted) {
      setItems((prev) => prev.filter((item) => item.id !== updated.id));
      return;
    }
    setItems((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
  }

  function handleDeleted(id: string) {
    if (showDeleted) {
      refresh();
      return;
    }
    setItems((prev) => prev.filter((item) => item.id !== id));
  }

  function handleDuplicated(created: ContentDraft) {
    setItems((prev) => [created, ...prev]);
    setTotalElements((prev) => prev + 1);
  }

  return (
    <div className="flex flex-1 flex-col gap-6 p-6">
      <div>
        <h2 className="text-2xl font-semibold tracking-tight">Content Drafts</h2>
        <p className="text-muted-foreground">
          Review, edit, and manage generated content before it goes to approval and publication.
        </p>
      </div>

      <DraftCreateForm onCreated={handleCreated} />

      <div className="flex flex-col gap-4">
        <DraftFilters
          search={searchInput}
          onSearchChange={setSearchInput}
          status={status}
          onStatusChange={handleStatusChange}
          contentType={contentType}
          onContentTypeChange={handleContentTypeChange}
          showDeleted={showDeleted}
          onShowDeletedChange={handleShowDeletedChange}
          layout={layout}
          onLayoutChange={setLayout}
        />

        <DraftList
          items={items}
          isLoading={isLoading}
          layout={layout}
          onChanged={handleChanged}
          onDeleted={handleDeleted}
          onDuplicated={handleDuplicated}
        />

        {totalPages > 1 && (
          <div className="flex items-center justify-between pt-2">
            <p className="text-sm text-muted-foreground">
              Page {page + 1} of {totalPages} - {totalElements} draft{totalElements === 1 ? "" : "s"}
            </p>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                <ChevronLeft className="size-4" />
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
                <ChevronRight className="size-4" />
              </Button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
