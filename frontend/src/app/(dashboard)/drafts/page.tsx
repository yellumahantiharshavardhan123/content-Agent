import { FileText } from "lucide-react";
import { ModulePlaceholder } from "@/components/shared/module-placeholder";

export default function DraftsPage() {
  return (
    <ModulePlaceholder
      icon={FileText}
      title="Content Drafts"
      description="Review, edit, and manage versioned AI-generated content before it goes to approval."
      moduleNumber={5}
    />
  );
}
