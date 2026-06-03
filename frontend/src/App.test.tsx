import { beforeEach, describe, expect, test } from "vitest";
import { render, screen } from "@testing-library/react";
import App from "./App";

describe("App routing", () => {
  beforeEach(() => {
    window.location.hash = "#/";
  });

  test("renders landing page when hash is root", () => {
    render(<App />);
    expect(screen.getByText(/AI resume intelligence/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /build resume/i })).toBeInTheDocument();
  });

  test("shows login page when hash is /login", () => {
    window.location.hash = "#/login";
    render(<App />);
    expect(screen.getByRole("heading", { name: /Sign in to ATSForge/i })).toBeInTheDocument();
  });
});
