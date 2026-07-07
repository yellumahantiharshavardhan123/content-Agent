import { CalendarClock } from "lucide-react";
import { ModulePlaceholder } from "@/components/shared/module-placeholder";

export default function SchedulerPage() {
  return (
    <ModulePlaceholder
      icon={CalendarClock}
      title="Scheduler"
      description="Schedule approved content for automatic publishing at a future date and time."
      moduleNumber={9}
    />
  );
}
