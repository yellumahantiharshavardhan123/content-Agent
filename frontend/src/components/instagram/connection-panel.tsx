"use client";

import { useState } from "react";
import { CheckCircle2, Loader2, Unplug, XCircle } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Alert, AlertDescription } from "@/components/ui/alert";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { connectInstagramAccount, disconnectInstagramAccount } from "@/lib/api/instagram";
import { ApiError } from "@/lib/api/client";
import type { InstagramAccount } from "@/types/instagram";

interface ConnectionPanelProps {
  account: InstagramAccount | null;
  isLoading: boolean;
  onChanged: (account: InstagramAccount) => void;
  onDisconnected: () => void;
}

export function ConnectionPanel({ account, isLoading, onChanged, onDisconnected }: ConnectionPanelProps) {
  const [open, setOpen] = useState(false);
  const [businessAccountId, setBusinessAccountId] = useState("");
  const [facebookPageId, setFacebookPageId] = useState("");
  const [accessToken, setAccessToken] = useState("");
  const [isConnecting, setIsConnecting] = useState(false);
  const [isDisconnecting, setIsDisconnecting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleConnect() {
    setIsConnecting(true);
    setError(null);
    try {
      const response = await connectInstagramAccount({
        businessAccountId,
        facebookPageId: facebookPageId || undefined,
        accessToken,
      });
      if (response.data) {
        onChanged(response.data);
        setOpen(false);
        setBusinessAccountId("");
        setFacebookPageId("");
        setAccessToken("");
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Could not connect. Please check your credentials.");
    } finally {
      setIsConnecting(false);
    }
  }

  async function handleDisconnect() {
    setIsDisconnecting(true);
    try {
      await disconnectInstagramAccount();
      onDisconnected();
    } finally {
      setIsDisconnecting(false);
    }
  }

  if (isLoading) {
    return (
      <Card>
        <CardContent className="flex items-center gap-2 py-6 text-muted-foreground">
          <Loader2 className="size-4 animate-spin" />
          Checking connection status...
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <CardTitle className="text-base">Instagram Connection</CardTitle>
          {account?.connected ? (
            <Badge>
              <CheckCircle2 className="size-3.5" />
              Connected
            </Badge>
          ) : (
            <Badge variant="outline">
              <XCircle className="size-3.5" />
              Not Connected
            </Badge>
          )}
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {account?.connected ? (
          <>
            <div className="grid gap-1 text-sm sm:grid-cols-2">
              <p>
                <span className="text-muted-foreground">Username: </span>
                {account.username ?? "-"}
              </p>
              <p>
                <span className="text-muted-foreground">Business Account ID: </span>
                {account.businessAccountId}
              </p>
              {account.facebookPageId && (
                <p>
                  <span className="text-muted-foreground">Facebook Page ID: </span>
                  {account.facebookPageId}
                </p>
              )}
              {account.connectedAt && (
                <p>
                  <span className="text-muted-foreground">Connected: </span>
                  {new Date(account.connectedAt).toLocaleString()}
                </p>
              )}
            </div>
            <AlertDialog>
              <AlertDialogTrigger render={<Button variant="outline" size="sm" className="self-start text-destructive" />}>
                <Unplug className="size-3.5" />
                Disconnect
              </AlertDialogTrigger>
              <AlertDialogContent>
                <AlertDialogHeader>
                  <AlertDialogTitle>Disconnect Instagram account?</AlertDialogTitle>
                  <AlertDialogDescription>
                    You will need to reconnect with a valid access token before publishing again.
                  </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                  <AlertDialogCancel disabled={isDisconnecting}>Cancel</AlertDialogCancel>
                  <AlertDialogAction onClick={handleDisconnect} disabled={isDisconnecting}>
                    {isDisconnecting && <Loader2 className="size-4 animate-spin" />}
                    Disconnect
                  </AlertDialogAction>
                </AlertDialogFooter>
              </AlertDialogContent>
            </AlertDialog>
          </>
        ) : (
          <>
            <p className="text-sm text-muted-foreground">
              Connect your Instagram Business Account to publish approved content directly.
            </p>
            <Dialog open={open} onOpenChange={setOpen}>
              <DialogTrigger render={<Button size="sm" className="self-start" />}>Connect Account</DialogTrigger>
              <DialogContent className="sm:max-w-md">
                <DialogHeader>
                  <DialogTitle>Connect Instagram Account</DialogTitle>
                  <DialogDescription>
                    Enter the credentials from your Meta Business app (Business Account ID and a long-lived access token).
                  </DialogDescription>
                </DialogHeader>
                {error && (
                  <Alert variant="destructive">
                    <AlertDescription>{error}</AlertDescription>
                  </Alert>
                )}
                <div className="flex flex-col gap-3">
                  <div className="flex flex-col gap-2">
                    <Label>Business Account ID</Label>
                    <Input value={businessAccountId} onChange={(e) => setBusinessAccountId(e.target.value)} />
                  </div>
                  <div className="flex flex-col gap-2">
                    <Label>Facebook Page ID (optional)</Label>
                    <Input value={facebookPageId} onChange={(e) => setFacebookPageId(e.target.value)} />
                  </div>
                  <div className="flex flex-col gap-2">
                    <Label>Access Token</Label>
                    <Input type="password" value={accessToken} onChange={(e) => setAccessToken(e.target.value)} />
                  </div>
                </div>
                <DialogFooter>
                  <Button onClick={handleConnect} disabled={isConnecting || !businessAccountId || !accessToken}>
                    {isConnecting && <Loader2 className="size-4 animate-spin" />}
                    Connect
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </>
        )}
      </CardContent>
    </Card>
  );
}
