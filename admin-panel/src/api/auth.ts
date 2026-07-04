import { post, get } from "./client";

export interface User {
  id: string;
  name: string;
  email: string;
  plan: string;
  role: string;
  active: boolean;
  createdAt: string;
  config?: {
    defaultMode: string;
    chatEnabled: boolean;
    audioEnabled: boolean;
    videoEnabled: boolean;
  };
}

interface LoginResponse {
  user: User;
  token: string;
  refreshToken: string;
}

interface MeResponse {
  user: User;
}

export async function login(email: string, password: string): Promise<LoginResponse> {
  const data = await post<LoginResponse>("/auth/login", { email, password });
  localStorage.setItem("token", data.token);
  localStorage.setItem("refreshToken", data.refreshToken);
  return data;
}

export async function getMe(): Promise<User> {
  const data = await get<MeResponse>("/auth/me");
  return data.user;
}

export function logout(): void {
  localStorage.removeItem("token");
  localStorage.removeItem("refreshToken");
  window.location.href = "/login";
}
