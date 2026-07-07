import { Globe } from "lucide-react";
import { ModulePlaceholder } from "@/components/shared/module-placeholder";

export default function WebsitePublisherPage() {
  return (
    <ModulePlaceholder
      icon={Globe}
      title="Website Publisher"
      description="Publish approved blog content to the academy website."
      moduleNumber={8}
    />
  );
}
