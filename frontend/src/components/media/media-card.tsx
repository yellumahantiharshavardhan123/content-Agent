"use client";

import { useState } from "react";
import { FileText, Loader2, Trash2, Video as VideoIcon } from "lucide-react";
import { Card, CardContent, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
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
import { deleteMedia } from "@/lib/api/media";
import { formatFileSize } from "@/lib/format";
import type { MediaItem } from "@/types/media";

interface MediaCardProps {
  media: MediaItem;
  onDeleted: () => void;
}

export function MediaCard({ media, onDeleted }: MediaCardProps) {
  const [isDeleting, setIsDeleting] = useState(false);

  async function handleDelete() {
    setIsDeleting(true);
    try {
      await deleteMedia(media.id);
      onDeleted();
    } finally {
      setIsDeleting(false);
    }
  }

  return (
    <Card className="overflow-hidden py-0">
      <a href={media.url} target="_blank" rel="noopener noreferrer" className="block">
        <div className="relative flex aspect-square items-center justify-center bg-muted">
          {media.mediaType === "IMAGE" && (
            // eslint-disable-next-line @next/next/no-img-element
            <img src={media.url} alt={media.fileName} className="size-full object-cover" />
          )}
          {media.mediaType === "VIDEO" && (
            <>
              <video src={media.url} className="size-full object-cover" muted preload="metadata" />
              <VideoIcon className="absolute size-6 text-white drop-shadow" />
            </>
          )}
          {(media.mediaType === "PDF" || media.mediaType === "TEXT_NOTE") && (
            <FileText className="size-10 text-muted-foreground" />
          )}
        </div>
      </a>
      <CardContent className="px-3 pt-3">
        <p className="truncate text-sm font-medium" title={media.fileName}>
          {media.fileName}
        </p>
        <p className="text-xs text-muted-foreground">{formatFileSize(media.fileSizeBytes)}</p>
        {media.description && (
          <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">{media.description}</p>
        )}
      </CardContent>
      <CardFooter className="px-3 pb-3">
        <AlertDialog>
          <AlertDialogTrigger render={<Button variant="ghost" size="sm" className="ml-auto text-destructive" />}>
            <Trash2 className="size-3.5" />
            Delete
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Delete this file?</AlertDialogTitle>
              <AlertDialogDescription>
                &ldquo;{media.fileName}&rdquo; will be permanently removed from storage. This cannot be undone.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={isDeleting}>Cancel</AlertDialogCancel>
              <AlertDialogAction onClick={handleDelete} disabled={isDeleting}>
                {isDeleting && <Loader2 className="size-4 animate-spin" />}
                Delete
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </CardFooter>
    </Card>
  );
}
