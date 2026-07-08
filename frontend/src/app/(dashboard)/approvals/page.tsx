"use client";

import { useCallback, useEffect, useState } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { ApprovalFilters } from "@/components/approvals/approval-filters";
import { ApprovalQueue } from "@/components/approvals/approval-queue";
import { listApprovals } from "@/lib/api/approval";
import type { Approval, ApprovalStatus } from "@/types/approval";

const PAGE_SIZE = 12;

export default function ApprovalsPage() {
  const [items, setItems] = useState<Approval[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  const [status, setStatus] = useState<ApprovalStatus>("PENDING_APPROVAL");
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");

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

  function handleStatusChange(value: ApprovalStatus) {
    setStatus(value);
    setPage(0);
  }

  function handleDateFromChange(value: string) {
    setDateFrom(value);
    setPage(0);
  }

  function handleDateToChange(value: string) {
    setDateTo(value);
    setPage(0);
  }

  const refresh = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await listApprovals({
        page,
        size: PAGE_SIZE,
        status,
        search: search || undefined,
        dateFrom: dateFrom ? `${dateFrom}T00:00:00Z` : undefined,
        dateTo: dateTo ? `${dateTo}T23:59:59Z` : undefined,
      });
      setItems(response.data?.content ?? []);
      setTotalPages(response.data?.totalPages ?? 0);
      setTotalElements(response.data?.totalElements ?? 0);
    } finally {
      setIsLoading(false);
    }
  }, [page, status, search, dateFrom, dateTo]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    refresh();
  }, [refresh]);

  function handleChanged(updated: Approval) {
    if (updated.status !== status) {
      setItems((prev) => prev.filter((item) => item.id !== updated.id));
      setTotalElements((prev) => Math.max(0, prev - 1));
      return;
    }
    setItems((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
  }

  return (
    <div className="flex flex-1 flex-col gap-6 p-6">
      <div>
        <h2 className="text-2xl font-semibold tracking-tight">Approval Workflow</h2>
        <p className="text-muted-foreground">
          Review submitted drafts, approve or reject them, and track every decision.
        </p>
      </div>

      <div className="flex flex-col gap-4">
        <ApprovalFilters
          status={status}
          onStatusChange={handleStatusChange}
          search={searchInput}
          onSearchChange={setSearchInput}
          dateFrom={dateFrom}
          onDateFromChange={handleDateFromChange}
          dateTo={dateTo}
          onDateToChange={handleDateToChange}
        />

        <ApprovalQueue items={items} isLoading={isLoading} onChanged={handleChanged} />

        {totalPages > 1 && (
          <div className="flex items-center justify-between pt-2">
            <p className="text-sm text-muted-foreground">
              Page {page + 1} of {totalPages} - {totalElements} item{totalElements === 1 ? "" : "s"}
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
