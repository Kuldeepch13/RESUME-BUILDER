export type ResumeSection = {
  id: string;
  type: string;
  title: string;
  order: number;
  visible: boolean;
  content: string | Record<string, string> | string[];
};

export type Resume = {
  id: string;
  title: string;
  slug: string;
  status: string;
  targetRole: string;
  templateCode: string;
  sections: ResumeSection[];
  theme: { primaryColor: string; fontFamily: string; fontSize: number };
  versionNumber: number;
  updatedAt: string;
};

export type Analysis = {
  id: string;
  status: string;
  atsScore?: number;
  matchPercentage?: number;
  result?: {
    missingKeywords: string[];
    suggestions: string[];
    strongSections: string[];
    weakSections: string[];
  };
};

export type UserProfile = {
  id: string;
  email: string;
  displayName: string;
  role: string;
  emailVerified: boolean;
};

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: UserProfile;
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

const baseUrl = import.meta.env.VITE_API_URL ?? "";

function storageKey(key: string) {
  return `atsforge:${key}`;
}

export class ApiClient {
  private token = localStorage.getItem(storageKey("access_token"));
  private refreshToken = localStorage.getItem(storageKey("refresh_token"));

  authenticated() {
    return Boolean(this.token);
  }

  async login(email: string, password: string) {
    const result = await this.request<AuthResponse>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    this.saveAuth(result.accessToken, result.refreshToken);
    return result;
  }

  async register(email: string, password: string, displayName: string) {
    return this.request<{ message: string }>("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify({ email, password, displayName }),
    });
  }

  async logout() {
    if (this.refreshToken) {
      await fetch(baseUrl + "/api/v1/auth/logout", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: this.refreshToken }),
      }).catch(() => null);
    }
    this.clearAuth();
  }

  async refresh() {
    if (!this.refreshToken) {
      throw new Error("Authentication expired.");
    }
    const result = await this.request<AuthResponse>("/api/v1/auth/refresh", {
      method: "POST",
      body: JSON.stringify({ refreshToken: this.refreshToken }),
    });
    this.saveAuth(result.accessToken, result.refreshToken);
    return result;
  }

  listResumes() {
    return this.request<Page<Resume>>("/api/v1/resumes?size=20&sort=updatedAt,desc");
  }

  getResume(id: string) {
    return this.request<Resume>(`/api/v1/resumes/${id}`);
  }

  createResume(title: string) {
    return this.request<Resume>("/api/v1/resumes", {
      method: "POST",
      body: JSON.stringify({ title, templateCode: "atlas", targetRole: "Product Designer" }),
    });
  }

  autosave(resume: Resume) {
    return this.request<Resume>(`/api/v1/resumes/${resume.id}/autosave`, {
      method: "PUT",
      body: JSON.stringify({
        title: resume.title,
        templateCode: resume.templateCode,
        targetRole: resume.targetRole,
        sections: resume.sections,
        theme: resume.theme,
      }),
    });
  }

  duplicateResume(id: string) {
    return this.request<Resume>(`/api/v1/resumes/${id}/duplicate`, { method: "POST" });
  }

  deleteResume(id: string) {
    return this.request<void>(`/api/v1/resumes/${id}`, { method: "DELETE" });
  }

  analyze(id: string, jobDescription: string) {
    return this.request<Analysis>(`/api/v1/resumes/${id}/analyses`, {
      method: "POST",
      body: JSON.stringify({ jobDescription }),
    });
  }

  analysis(id: string) {
    return this.request<Analysis>(`/api/v1/analyses/${id}`);
  }

  async export(id: string, format: string) {
    if (!this.token) {
      throw new Error("You must be signed in to export.");
    }
    const response = await fetch(`${baseUrl}/api/v1/resumes/${id}/export?format=${format}`, {
      headers: { Authorization: `Bearer ${this.token}` },
    });
    if (!response.ok) {
      const problem = await response.text().catch(() => "Export failed.");
      throw new Error(problem || "Export failed.");
    }
    const blob = await response.blob();
    const disposition = response.headers.get("content-disposition") ?? `filename=resume.${format}`;
    const filename = disposition.match(/filename="(.+)"/)?.[1] ?? `resume.${format}`;
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
    URL.revokeObjectURL(link.href);
  }

  private async request<T>(path: string, options: RequestInit = {}, retry = true): Promise<T> {
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
      ...(options.headers as Record<string, string>),
    };
    if (this.token) {
      headers.Authorization = `Bearer ${this.token}`;
    }

    const response = await fetch(baseUrl + path, { ...options, headers });
    if (response.status === 401 && retry && this.refreshToken) {
      await this.refresh();
      return this.request(path, options, false);
    }

    if (!response.ok) {
      const problem = await response.json().catch(() => ({ message: "Request failed." }));
      throw new Error(problem.message ?? "Request failed.");
    }

    return response.json() as Promise<T>;
  }

  private saveAuth(accessToken: string, refreshToken: string) {
    this.token = accessToken;
    this.refreshToken = refreshToken;
    localStorage.setItem(storageKey("access_token"), accessToken);
    localStorage.setItem(storageKey("refresh_token"), refreshToken);
  }

  private clearAuth() {
    this.token = null;
    this.refreshToken = null;
    localStorage.removeItem(storageKey("access_token"));
    localStorage.removeItem(storageKey("refresh_token"));
  }
}
