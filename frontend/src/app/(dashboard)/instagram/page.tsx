import { Camera } from "lucide-react";
import { ModulePlaceholder } from "@/components/shared/module-placeholder";

export default function InstagramPublisherPage() {
  return (
    <ModulePlaceholder
      icon={Camera}
      title="Instagram Publisher"
      description="Publish approved content directly to the academy's Instagram Business account via the Meta Graph API."
      moduleNumber={7}
    />
  );
}
