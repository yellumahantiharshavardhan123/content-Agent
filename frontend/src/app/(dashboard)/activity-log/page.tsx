import { History } from "lucide-react";
import { ModulePlaceholder } from "@/components/shared/module-placeholder";

export default function ActivityLogPage() {
  return (
    <ModulePlaceholder
      icon={History}
      title="Activity Log"
      description="Search and filter the audit trail of who generated, edited, approved, published, or deleted content."
      moduleNumber={11}
    />
  );
}
