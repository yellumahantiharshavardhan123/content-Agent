import { UploadCloud } from "lucide-react";
import { ModulePlaceholder } from "@/components/shared/module-placeholder";

export default function MediaPage() {
  return (
    <ModulePlaceholder
      icon={UploadCloud}
      title="Media Upload"
      description="Upload images, videos, PDFs, and text notes for AI content generation."
      moduleNumber={3}
    />
  );
}
