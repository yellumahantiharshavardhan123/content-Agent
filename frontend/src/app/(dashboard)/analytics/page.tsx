import { BarChart3 } from "lucide-react";
import { ModulePlaceholder } from "@/components/shared/module-placeholder";

export default function AnalyticsPage() {
  return (
    <ModulePlaceholder
      icon={BarChart3}
      title="Content History & Analytics"
      description="Track published content history and view engagement analytics."
      moduleNumber={10}
    />
  );
}
