import { redirect } from "next/navigation";
import { Sidebar } from "@/components/layout/sidebar";
import { Topbar } from "@/components/layout/topbar";
import { getCurrentUserServerSide } from "@/lib/api/server-auth";

export default async function DashboardLayout({ children }: { children: React.ReactNode }) {
  const user = await getCurrentUserServerSide();

  if (!user) {
    redirect("/login");
  }

  return (
    <div className="flex min-h-screen w-full">
      <Sidebar />
      <div className="flex flex-1 flex-col">
        <Topbar user={user} />
        <main className="flex flex-1 flex-col">{children}</main>
      </div>
    </div>
  );
}
