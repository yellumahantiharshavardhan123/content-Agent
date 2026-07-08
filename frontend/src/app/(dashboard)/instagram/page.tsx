"use client";

import { useCallback, useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ConnectionPanel } from "@/components/instagram/connection-panel";
import { PublishingQueue } from "@/components/instagram/publishing-queue";
import { InstagramHistoryList } from "@/components/instagram/instagram-history-list";
import { getInstagramStatus, getInstagramHistory } from "@/lib/api/instagram";
import { listApprovals } from "@/lib/api/approval";
import type { Approval } from "@/types/approval";
import type { InstagramAccount, InstagramHistoryEntry, InstagramPost } from "@/types/instagram";

export default function InstagramPublisherPage() {
  const [account, setAccount] = useState<InstagramAccount | null>(null);
  const [isAccountLoading, setIsAccountLoading] = useState(true);

  const [queue, setQueue] = useState<Approval[]>([]);
  const [isQueueLoading, setIsQueueLoading] = useState(true);

  const [history, setHistory] = useState<InstagramHistoryEntry[]>([]);
  const [isHistoryLoading, setIsHistoryLoading] = useState(true);

  const refreshAccount = useCallback(async () => {
    setIsAccountLoading(true);
    try {
      const response = await getInstagramStatus();
      setAccount(response.data ?? null);
    } finally {
      setIsAccountLoading(false);
    }
  }, []);

  const refreshQueue = useCallback(async () => {
    setIsQueueLoading(true);
    try {
      const response = await listApprovals({ status: "READY_FOR_PUBLISH", size: 50 });
      setQueue(response.data?.content ?? []);
    } finally {
      setIsQueueLoading(false);
    }
  }, []);

  const refreshHistory = useCallback(async () => {
    setIsHistoryLoading(true);
    try {
      const response = await getInstagramHistory(0, 50);
      setHistory(response.data?.content ?? []);
    } finally {
      setIsHistoryLoading(false);
    }
  }, []);

  const refreshAll = useCallback(async () => {
    await Promise.all([refreshAccount(), refreshQueue(), refreshHistory()]);
  }, [refreshAccount, refreshQueue, refreshHistory]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    refreshAll();
  }, [refreshAll]);

  function handleAccountChanged(updated: InstagramAccount) {
    setAccount(updated);
    refreshHistory();
  }

  function handleDisconnected() {
    setAccount({ connected: false, id: null, businessAccountId: null, facebookPageId: null, username: null, connectedAt: null, disconnectedAt: null });
    refreshHistory();
  }

  function handlePublished(post: InstagramPost) {
    setQueue((prev) => prev.filter((item) => item.id !== post.approvalId));
    refreshHistory();
  }

  return (
    <div className="flex flex-1 flex-col gap-6 p-6">
      <div>
        <h2 className="text-2xl font-semibold tracking-tight">Instagram Publisher</h2>
        <p className="text-muted-foreground">
          Connect your Instagram Business Account and publish approved content directly to it.
        </p>
      </div>

      <ConnectionPanel
        account={account}
        isLoading={isAccountLoading}
        onChanged={handleAccountChanged}
        onDisconnected={handleDisconnected}
      />

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Publishing Queue</CardTitle>
        </CardHeader>
        <CardContent>
          <PublishingQueue
            items={queue}
            isLoading={isQueueLoading}
            connected={account?.connected ?? false}
            onPublished={handlePublished}
          />
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Publish History</CardTitle>
        </CardHeader>
        <CardContent>
          <InstagramHistoryList entries={history} isLoading={isHistoryLoading} />
        </CardContent>
      </Card>
    </div>
  );
}
