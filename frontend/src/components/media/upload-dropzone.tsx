"use client";

import { useRef, useState, type DragEvent } from "react";
import { Loader2, UploadCloud } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { uploadMedia } from "@/lib/api/media";
import { ApiError } from "@/lib/api/client";
import { formatFileSize } from "@/lib/format";
import { cn } from "@/lib/utils";

interface UploadDropzoneProps {
  onUploaded: () => void;
}

export function UploadDropzone({ onUploaded }: UploadDropzoneProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [description, setDescription] = useState("");
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function handleDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setIsDragging(false);
    const file = event.dataTransfer.files?.[0];
    if (file) setSelectedFile(file);
  }

  async function handleUpload() {
    if (!selectedFile) return;
    setIsUploading(true);
    setError(null);
    try {
      await uploadMedia(selectedFile, description || undefined);
      setSelectedFile(null);
      setDescription("");
      if (inputRef.current) inputRef.current.value = "";
      onUploaded();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Upload failed. Please try again.");
    } finally {
      setIsUploading(false);
    }
  }

  return (
    <div className="flex flex-col gap-3">
      {error && (
        <Alert variant="destructive">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      <div
        onDragOver={(e) => {
          e.preventDefault();
          setIsDragging(true);
        }}
        onDragLeave={() => setIsDragging(false)}
        onDrop={handleDrop}
        onClick={() => inputRef.current?.click()}
        className={cn(
          "flex cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-8 text-center transition-colors",
          isDragging ? "border-primary bg-accent" : "border-border hover:bg-accent/50"
        )}
      >
        <UploadCloud className="size-8 text-muted-foreground" />
        {selectedFile ? (
          <div className="text-sm">
            <span className="font-medium">{selectedFile.name}</span>{" "}
            <span className="text-muted-foreground">({formatFileSize(selectedFile.size)})</span>
          </div>
        ) : (
          <div className="text-sm text-muted-foreground">
            Drag and drop a file here, or click to browse
            <div className="mt-1 text-xs">Images, videos, PDFs, or text notes</div>
          </div>
        )}
        <input
          ref={inputRef}
          type="file"
          className="hidden"
          accept="image/jpeg,image/png,image/webp,image/gif,video/mp4,video/quicktime,video/x-matroska,video/webm,application/pdf,text/plain"
          onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
        />
      </div>

      {selectedFile && (
        <>
          <Textarea
            placeholder="Optional context (e.g. event name, achievement, training session)..."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={isUploading}
            rows={2}
          />
          <div className="flex gap-2">
            <Button onClick={handleUpload} disabled={isUploading}>
              {isUploading && <Loader2 className="size-4 animate-spin" />}
              Upload
            </Button>
            <Button
              variant="ghost"
              disabled={isUploading}
              onClick={() => {
                setSelectedFile(null);
                setDescription("");
                if (inputRef.current) inputRef.current.value = "";
              }}
            >
              Cancel
            </Button>
          </div>
        </>
      )}
    </div>
  );
}
