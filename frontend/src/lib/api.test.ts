import { describe, expect, test, beforeEach, vi } from "vitest";
import { ApiClient } from "./api";

describe("ApiClient", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  test("stores auth tokens after login", async () => {
    const apiResponse = {
      accessToken: "access-token",
      refreshToken: "refresh-token",
      expiresInSeconds: 3600,
      user: { id: "user-id", email: "test@example.com", displayName: "Test User", role: "USER", emailVerified: true },
    };

    vi.stubGlobal("fetch", vi.fn(() =>
      Promise.resolve(new Response(JSON.stringify(apiResponse), { status: 200, headers: { "Content-Type": "application/json" } }))
    ));

    const client = new ApiClient();
    await client.login("test@example.com", "password123");

    expect(localStorage.getItem("atsforge:access_token")).toBe("access-token");
    expect(localStorage.getItem("atsforge:refresh_token")).toBe("refresh-token");
    expect(client.authenticated()).toBe(true);
  });

  test("clears auth tokens on logout", async () => {
    localStorage.setItem("atsforge:access_token", "access-token");
    localStorage.setItem("atsforge:refresh_token", "refresh-token");

    vi.stubGlobal("fetch", vi.fn(() => Promise.resolve(new Response(null, { status: 200 }))));

    const client = new ApiClient();
    await client.logout();

    expect(localStorage.getItem("atsforge:access_token")).toBeNull();
    expect(localStorage.getItem("atsforge:refresh_token")).toBeNull();
    expect(client.authenticated()).toBe(false);
  });

  test("refresh throws when no refresh token is available", async () => {
    const client = new ApiClient();
    await expect(client.refresh()).rejects.toThrow("Authentication expired.");
  });
});
