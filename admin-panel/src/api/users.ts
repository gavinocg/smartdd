import { get, put } from "./client";
import type { User } from "./auth";

interface UsersResponse {
  users: User[];
  pagination: { page: number; limit: number; total: number; totalPages: number };
}

interface UserDetailResponse {
  user: User & {
    qrCodes: unknown[];
    sessions: unknown[];
  };
}

export async function getUsers(page = 1, search?: string, plan?: string): Promise<UsersResponse> {
  const params = new URLSearchParams({ page: String(page), limit: "20" });
  if (search) params.set("search", search);
  if (plan) params.set("plan", plan);
  return get(`/admin/users?${params}`);
}

export async function getUser(id: string): Promise<UserDetailResponse> {
  return get(`/admin/users/${id}`);
}

export async function changePlan(id: string, plan: string): Promise<void> {
  await put(`/admin/users/${id}/plan`, { plan });
}

export async function toggleSuspend(id: string): Promise<void> {
  await put(`/admin/users/${id}/suspend`);
}
