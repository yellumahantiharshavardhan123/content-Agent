"use client";

import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { cn } from "@/lib/utils";
import { APPROVAL_FILTER_TABS } from "@/lib/approval-status-labels";
import type { ApprovalStatus } from "@/types/approval";

interface ApprovalFiltersProps {
  status: ApprovalStatus;
  onStatusChange: (value: ApprovalStatus) => void;
  search: string;
  onSearchChange: (value: string) => void;
  dateFrom: string;
  onDateFromChange: (value: string) => void;
  dateTo: string;
  onDateToChange: (value: string) => void;
}

export function ApprovalFilters({
  status,
  onStatusChange,
  search,
  onSearchChange,
  dateFrom,
  onDateFromChange,
  dateTo,
  onDateToChange,
}: ApprovalFiltersProps) {
  return (
    <div className="flex flex-col gap-3">
      <div className="flex flex-wrap gap-1 rounded-md border p-1 sm:w-fit">
        {APPROVAL_FILTER_TABS.map((tab) => (
          <Button
            key={tab.value}
            type="button"
            variant={status === tab.value ? "secondary" : "ghost"}
            size="sm"
            className={cn("rounded-sm")}
            onClick={() => onStatusChange(tab.value)}
          >
            {tab.label}
          </Button>
        ))}
      </div>

      <div className="flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1.5">
          <Label className="text-xs text-muted-foreground">Search</Label>
          <Input
            className="w-full sm:w-64"
            placeholder="Search by content title..."
            value={search}
            onChange={(e) => onSearchChange(e.target.value)}
          />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label className="text-xs text-muted-foreground">From</Label>
          <Input type="date" className="w-40" value={dateFrom} onChange={(e) => onDateFromChange(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1.5">
          <Label className="text-xs text-muted-foreground">To</Label>
          <Input type="date" className="w-40" value={dateTo} onChange={(e) => onDateToChange(e.target.value)} />
        </div>
      </div>
    </div>
  );
}
