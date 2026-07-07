import { apiFetch } from "@/lib/api/client";

export interface HealthStatus {
  status: string;
  service: string;
  timestamp: string;
}

export function getHealth() {
  return apiFetch<HealthStatus>("/health");
}
