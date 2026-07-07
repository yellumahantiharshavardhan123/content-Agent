import { Target } from "lucide-react";
import { ScrollArea } from "@/components/ui/scroll-area";
import { NavLinks } from "@/components/layout/nav-links";

export function Sidebar() {
  return (
    <aside className="hidden md:flex md:w-64 md:flex-col border-r bg-sidebar text-sidebar-foreground">
      <div className="flex h-16 items-center gap-2 border-b px-6">
        <Target className="size-5 text-sidebar-primary" />
        <span className="font-semibold tracking-tight">Arjun Sports AI</span>
      </div>
      <ScrollArea className="flex-1">
        <NavLinks />
      </ScrollArea>
    </aside>
  );
}
