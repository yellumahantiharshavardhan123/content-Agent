import type { LucideIcon } from "lucide-react";
import {
  LayoutDashboard,
  UploadCloud,
  Sparkles,
  FileText,
  CheckSquare,
  Camera,
  Globe,
  CalendarClock,
  BarChart3,
  History,
  Bell,
  Settings,
} from "lucide-react";

export interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  /** Module number from the build plan, shown on placeholder pages until the module ships. */
  moduleNumber: number;
  status: "available" | "upcoming";
}

export const NAV_ITEMS: NavItem[] = [
  { label: "Dashboard", href: "/dashboard", icon: LayoutDashboard, moduleNumber: 0, status: "available" },
  { label: "Media Upload", href: "/media", icon: UploadCloud, moduleNumber: 2, status: "available" },
  { label: "AI Content Generator", href: "/content", icon: Sparkles, moduleNumber: 3, status: "available" },
  { label: "Drafts", href: "/drafts", icon: FileText, moduleNumber: 5, status: "upcoming" },
  { label: "Approvals", href: "/approvals", icon: CheckSquare, moduleNumber: 6, status: "upcoming" },
  { label: "Instagram Publisher", href: "/instagram", icon: Camera, moduleNumber: 7, status: "upcoming" },
  { label: "Website Publisher", href: "/website", icon: Globe, moduleNumber: 8, status: "upcoming" },
  { label: "Scheduler", href: "/scheduler", icon: CalendarClock, moduleNumber: 9, status: "upcoming" },
  { label: "Analytics", href: "/analytics", icon: BarChart3, moduleNumber: 10, status: "upcoming" },
  { label: "Activity Log", href: "/activity-log", icon: History, moduleNumber: 11, status: "upcoming" },
  { label: "Notifications", href: "/notifications", icon: Bell, moduleNumber: 12, status: "upcoming" },
  { label: "Settings", href: "/settings", icon: Settings, moduleNumber: 2, status: "upcoming" },
];
