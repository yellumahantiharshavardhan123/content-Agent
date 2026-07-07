import { Bell } from "lucide-react";
import { ModulePlaceholder } from "@/components/shared/module-placeholder";

export default function NotificationsPage() {
  return (
    <ModulePlaceholder
      icon={Bell}
      title="Notification Center"
      description="View in-app notifications for generation, approval, and publishing events."
      moduleNumber={12}
    />
  );
}
