import { Settings } from "lucide-react";
import { ModulePlaceholder } from "@/components/shared/module-placeholder";

export default function SettingsPage() {
  return (
    <ModulePlaceholder
      icon={Settings}
      title="Application Settings"
      description="Configure the AI provider, generation parameters, scheduling defaults, storage, and integrations from one place."
      moduleNumber={2}
    />
  );
}
