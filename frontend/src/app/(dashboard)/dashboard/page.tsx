import { CheckCircle2, Circle } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { NAV_ITEMS } from "@/lib/nav-config";
import { BackendStatusCard } from "@/components/dashboard/backend-status-card";

const TOTAL_MODULES = 15;

export default function DashboardOverviewPage() {
  const upcomingModules = NAV_ITEMS.filter((item) => item.status === "upcoming");

  return (
    <div className="flex flex-1 flex-col gap-6 p-6">
      <div>
        <h2 className="text-2xl font-semibold tracking-tight">Welcome back</h2>
        <p className="text-muted-foreground">
          Foundation &amp; scaffolding is complete. Each module below ships incrementally.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <BackendStatusCard />
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Modules shipped</CardTitle>
            <CardDescription>Foundation &amp; Scaffolding</CardDescription>
          </CardHeader>
          <CardContent className="text-2xl font-semibold">1 / {TOTAL_MODULES}</CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Build roadmap</CardTitle>
          <CardDescription>Each module is confirmed with the academy before it&apos;s built.</CardDescription>
        </CardHeader>
        <CardContent>
          <ul className="flex flex-col gap-3">
            <li className="flex items-center gap-3 text-sm">
              <CheckCircle2 className="size-4 text-primary" />
              <span className="font-medium">Foundation &amp; Scaffolding</span>
              <span className="text-muted-foreground">— monorepo, security baseline, audit log, notifications, settings scaffold</span>
            </li>
            {upcomingModules.map((item) => (
              <li key={item.href} className="flex items-center gap-3 text-sm">
                <Circle className="size-4 text-muted-foreground" />
                <span className="font-medium">{item.label}</span>
                <span className="text-muted-foreground">— Module {item.moduleNumber}</span>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  );
}
