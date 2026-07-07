export type Role = "ADMIN";

export type UserStatus = "ACTIVE" | "INACTIVE" | "SUSPENDED";

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  role: Role;
  status: UserStatus;
  active: boolean;
  lastLogin: string | null;
  createdAt: string;
  updatedAt: string;
}
