import { CheckSquare } from "lucide-react";
import { ModulePlaceholder } from "@/components/shared/module-placeholder";

export default function ApprovalsPage() {
  return (
    <ModulePlaceholder
      icon={CheckSquare}
      title="Approval Workflow"
      description="Preview drafts and approve or reject content before it is published."
      moduleNumber={6}
    />
  );
}
