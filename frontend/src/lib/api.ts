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

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  user: { displayName: string; role: string };
};

const baseUrl = import.meta.env.VITE_API_URL ?? "";

export class ApiClient {
  private token = localStorage.getItem("atsforge_access_token");

  authenticated() {
    return Boolean(this.token);
  }

  async login(email: string, password: string) {
    const result = await this.request<AuthResponse>("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    });
    this.token = result.accessToken;
    localStorage.setItem("atsforge_access_token", result.accessToken);
    localStorage.setItem("atsforge_refresh_token", result.refreshToken);
    return result;
  }

  logout() {
    this.token = null;
    localStorage.removeItem("atsforge_access_token");
    localStorage.removeItem("atsforge_refresh_token");
  }

  listResumes() {
    return this.request<{ content: Resume[] }>("/api/v1/resumes?size=20&sort=updatedAt,desc");
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
    const response = await fetch(`${baseUrl}/api/v1/resumes/${id}/export?format=${format}`, {
      headers: { Authorization: `Bearer ${this.token}` },
    });
    if (!response.ok) throw new Error("Export failed.");
    const blob = await response.blob();
    const disposition = response.headers.get("content-disposition") ?? `filename="resume.${format}"`;
    const filename = disposition.match(/filename="(.+)"/)?.[1] ?? `resume.${format}`;
    const link = document.createElement("a");
    link.href = URL.createObjectURL(blob);
    link.download = filename;
    link.click();
    URL.revokeObjectURL(link.href);
  }

  private async request<T>(path: string, options: RequestInit = {}): Promise<T> {
    const response = await fetch(baseUrl + path, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...(this.token ? { Authorization: `Bearer ${this.token}` } : {}),
        ...options.headers,
      },
    });
    if (!response.ok) {
      const problem = await response.json().catch(() => ({ message: "Request failed." }));
      throw new Error(problem.message ?? "Request failed.");
    }
    return response.json() as Promise<T>;
  }
}
