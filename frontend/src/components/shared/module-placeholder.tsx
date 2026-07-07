import type { LucideIcon } from "lucide-react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";

interface ModulePlaceholderProps {
  icon: LucideIcon;
  title: string;
  description: string;
  moduleNumber: number;
}

export function ModulePlaceholder({ icon: Icon, title, description, moduleNumber }: ModulePlaceholderProps) {
  return (
    <div className="flex flex-1 items-center justify-center p-6">
      <Card className="w-full max-w-lg">
        <CardHeader className="items-center text-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-full bg-muted">
            <Icon className="size-6 text-muted-foreground" />
          </div>
          <CardTitle className="text-xl">{title}</CardTitle>
          <CardDescription>{description}</CardDescription>
        </CardHeader>
        <CardContent className="flex justify-center">
          <Badge variant="secondary">Ships in Module {moduleNumber}</Badge>
        </CardContent>
      </Card>
    </div>
  );
}
