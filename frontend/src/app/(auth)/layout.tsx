import { Target } from "lucide-react";
import { ThemeToggle } from "@/components/layout/theme-toggle";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen flex-col">
      <header className="flex h-16 items-center justify-between px-6">
        <div className="flex items-center gap-2">
          <Target className="size-5 text-primary" />
          <span className="font-semibold tracking-tight">Arjun Sports AI</span>
        </div>
        <ThemeToggle />
      </header>
      <main className="flex flex-1 items-center justify-center p-6">{children}</main>
    </div>
  );
}
