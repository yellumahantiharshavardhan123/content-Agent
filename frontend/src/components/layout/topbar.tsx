"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { Bell, KeyRound, LogOut, Settings, UserRound } from "lucide-react";
import { NAV_ITEMS } from "@/lib/nav-config";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import { MobileNav } from "@/components/layout/mobile-nav";
import { ThemeToggle } from "@/components/layout/theme-toggle";
import { ChangePasswordDialog } from "@/components/layout/change-password-dialog";
import { logout } from "@/lib/api/auth";
import type { User } from "@/types/user";

interface TopbarProps {
  user: User;
}

export function Topbar({ user }: TopbarProps) {
  const pathname = usePathname();
  const currentPage = NAV_ITEMS.find((item) => item.href === pathname);
  const [changePasswordOpen, setChangePasswordOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  const initials = `${user.firstName[0] ?? ""}${user.lastName[0] ?? ""}`.toUpperCase();

  async function handleLogout() {
    setLoggingOut(true);
    try {
      await logout();
    } finally {
      window.location.assign("/login");
    }
  }

  return (
    <header className="flex h-16 items-center gap-3 border-b px-4 md:px-6">
      <MobileNav />
      <h1 className="text-lg font-semibold tracking-tight">{currentPage?.label ?? "Dashboard"}</h1>
      <div className="ml-auto flex items-center gap-1">
        <Tooltip>
          <TooltipTrigger render={<Button variant="ghost" size="icon" aria-label="Notifications" />}>
            <Bell className="size-4" />
          </TooltipTrigger>
          <TooltipContent>Notification Center ships in Module 12</TooltipContent>
        </Tooltip>
        <ThemeToggle />
        <DropdownMenu>
          <DropdownMenuTrigger
            render={<Button variant="ghost" size="icon" className="ml-1" aria-label="Account menu" />}
          >
            <Avatar className="size-8">
              <AvatarFallback>{initials || <UserRound className="size-4" />}</AvatarFallback>
            </Avatar>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuGroup>
              <DropdownMenuLabel className="flex flex-col">
                <span>{user.firstName} {user.lastName}</span>
                <span className="text-xs font-normal text-muted-foreground">{user.email}</span>
              </DropdownMenuLabel>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuItem render={<Link href="/settings" />}>
              <Settings className="size-4" />
              Settings
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => setChangePasswordOpen(true)}>
              <KeyRound className="size-4" />
              Change password
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem variant="destructive" disabled={loggingOut} onClick={handleLogout}>
              <LogOut className="size-4" />
              {loggingOut ? "Logging out..." : "Logout"}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
      <ChangePasswordDialog open={changePasswordOpen} onOpenChange={setChangePasswordOpen} />
    </header>
  );
}
