"use client";

import { useCallback, useEffect, useState } from "react";
import { RefreshCw, ServerCog } from "lucide-react";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { getHealth } from "@/lib/api/health";

type Status = "checking" | "up" | "down";

export function BackendStatusCard() {
  const [status, setStatus] = useState<Status>("checking");
  const [checkedAt, setCheckedAt] = useState<string | null>(null);

  const check = useCallback(async () => {
    try {
      const response = await getHealth();
      setStatus(response.data?.status === "UP" ? "up" : "down");
    } catch {
      setStatus("down");
    } finally {
      setCheckedAt(new Date().toLocaleTimeString());
    }
  }, []);

  useEffect(() => {
    // Fetch-on-mount: setState only happens after the awaited response,
    // but the lint rule's static analysis can't see past the await and
    // flags it anyway. This is the standard React data-fetching pattern.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    check();
  }, [check]);

  const handleRecheck = () => {
    setStatus("checking");
    check();
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center gap-2">
          <ServerCog className="size-4 text-muted-foreground" />
          <CardTitle className="text-base">Backend API</CardTitle>
        </div>
        <CardDescription>
          {checkedAt ? `Last checked ${checkedAt}` : "Checking connectivity..."}
        </CardDescription>
        <CardAction>
          <Button variant="ghost" size="icon" onClick={handleRecheck} aria-label="Recheck backend status">
            <RefreshCw className="size-4" />
          </Button>
        </CardAction>
      </CardHeader>
      <CardContent>
        <Badge variant={status === "up" ? "default" : status === "down" ? "destructive" : "secondary"}>
          {status === "up" ? "Connected" : status === "down" ? "Unreachable" : "Checking"}
        </Badge>
      </CardContent>
    </Card>
  );
}
