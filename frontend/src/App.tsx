import { CSSProperties, FormEvent, useEffect, useRef, useState } from "react";
import { Analysis, ApiClient, Resume, ResumeSection } from "./lib/api";

const sampleSections: ResumeSection[] = [
  { id: "personal", type: "PERSONAL", title: "Contact", order: 0, visible: true, content: "Ava Morgan | ava@portfolio.dev | Chicago, IL | linkedin.com/in/avamorgan" },
  { id: "summary", type: "SUMMARY", title: "Profile", order: 1, visible: true, content: "Product designer who turns complex B2B workflows into focused experiences. Reduced onboarding time by 38% through research-led redesigns." },
  { id: "experience", type: "EXPERIENCE", title: "Experience", order: 2, visible: true, content: "Lead Product Designer - Meridian Labs | 2021-present\n- Shipped design system adopted by 7 squads, reducing UI defects by 31%.\n- Increased activation by 18% through a guided onboarding experiment." },
  { id: "skills", type: "SKILLS", title: "Capabilities", order: 3, visible: true, content: "Product strategy, Figma, prototyping, user research, design systems, accessibility" },
  { id: "education", type: "EDUCATION", title: "Education", order: 4, visible: true, content: "BFA Interaction Design - University of Illinois" },
];

const sampleResume: Resume = {
  id: "demo",
  title: "Senior Product Designer",
  slug: "senior-product-designer",
  status: "DRAFT",
  targetRole: "Lead Product Designer",
  templateCode: "atlas",
  sections: sampleSections,
  theme: { primaryColor: "#087e78", fontFamily: "Inter", fontSize: 10 },
  versionNumber: 4,
  updatedAt: new Date().toISOString(),
};

const templates = [
  { name: "Atlas", type: "ATS-friendly", free: true, color: "#087e78" },
  { name: "Meridian", type: "Modern", free: false, color: "#334ba8" },
  { name: "Boardroom", type: "Corporate", free: false, color: "#936233" },
  { name: "Signal", type: "Minimal", free: true, color: "#172334" },
];

function App() {
  const [client] = useState(() => new ApiClient());
  const [route, setRoute] = useState(location.hash.slice(1) || "/");
  const [dark, setDark] = useState(false);
  const [selectedResume, setSelectedResume] = useState<Resume>(sampleResume);
  const [signedIn, setSignedIn] = useState(client.authenticated());

  useEffect(() => {
    const listen = () => setRoute(location.hash.slice(1) || "/");
    window.addEventListener("hashchange", listen);
    return () => window.removeEventListener("hashchange", listen);
  }, []);

  const navigate = (path: string) => { location.hash = path; };

  return (
    <div className={dark ? "app dark" : "app"}>
      <Header route={route} signedIn={signedIn} dark={dark} setDark={setDark} navigate={navigate}
        logout={() => { client.logout(); setSignedIn(false); navigate("/"); }} />
      {route === "/" && <Landing navigate={navigate} />}
      {route === "/login" && <Login client={client} navigate={navigate} onLogin={() => setSignedIn(true)} />}
      {route === "/dashboard" && <Dashboard client={client} signedIn={signedIn} navigate={navigate}
        edit={(resume) => { setSelectedResume(resume); navigate("/editor"); }} />}
      {route === "/templates" && <Templates navigate={navigate} />}
      {route === "/pricing" && <Pricing navigate={navigate} />}
      {route === "/admin" && <Admin />}
      {route === "/editor" && <Editor client={client} signedIn={signedIn} initial={selectedResume} />}
      {route !== "/editor" && <Footer />}
    </div>
  );
}

function Header({ route, signedIn, dark, setDark, navigate, logout }: {
  route: string; signedIn: boolean; dark: boolean; setDark: (value: boolean) => void;
  navigate: (path: string) => void; logout: () => void;
}) {
  return (
    <header className="topbar">
      <button className="brand" onClick={() => navigate("/")} aria-label="ATSForge home">
        <span className="brand-mark">A</span><span>ATS<strong>Forge</strong></span>
      </button>
      <nav className="nav">
        <button className={route === "/templates" ? "active" : ""} onClick={() => navigate("/templates")}>Templates</button>
        <button className={route === "/pricing" ? "active" : ""} onClick={() => navigate("/pricing")}>Pricing</button>
        {signedIn && <button className={route === "/dashboard" ? "active" : ""} onClick={() => navigate("/dashboard")}>Workspace</button>}
      </nav>
      <div className="top-actions">
        <button className="theme-toggle" aria-label="Toggle color theme" onClick={() => setDark(!dark)}>{dark ? "Light" : "Dark"}</button>
        {signedIn ? <button className="ghost" onClick={logout}>Sign out</button> :
          <button className="ghost" onClick={() => navigate("/login")}>Sign in</button>}
        <button className="primary compact" onClick={() => navigate(signedIn ? "/dashboard" : "/login")}>Build resume</button>
      </div>
    </header>
  );
}

function Landing({ navigate }: { navigate: (path: string) => void }) {
  return (
    <main>
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">AI resume intelligence for modern careers</p>
          <h1>Craft a resume that gets <span>through the screen.</span></h1>
          <p className="hero-lead">Design beautiful, ATS-readable resumes with real-time guidance, job-match scoring, and writing that sounds like you on your best day.</p>
          <div className="hero-buttons">
            <button className="primary" onClick={() => navigate("/editor")}>Start building free</button>
            <button className="secondary" onClick={() => navigate("/templates")}>Explore templates</button>
          </div>
          <div className="social-proof">
            <div><strong>92%</strong><small>average ATS readiness</small></div>
            <div><strong>40k+</strong><small>resumes refined</small></div>
            <div><strong>4.9</strong><small>creator rating</small></div>
          </div>
        </div>
        <div className="hero-product">
          <div className="score-card">
            <span>ATS score</span><strong>92</strong><div className="ring"></div>
            <p>+14 after optimization</p>
          </div>
          <ResumePage resume={sampleResume} condensed />
          <div className="tip-card"><b>AI suggestion</b><p>Add measurable impact to your design system bullet.</p><button>Apply rewrite</button></div>
        </div>
      </section>
      <section className="workflow">
        <p className="eyebrow center">A calmer way to apply</p>
        <h2>From blank page to matched application</h2>
        <div className="steps">
          {[
            ["01", "Build", "Compose sections with flexible templates and instant preview."],
            ["02", "Analyze", "Compare against a job description and find missing signals."],
            ["03", "Polish", "Accept precise rewrites and export in recruiter-ready formats."],
          ].map(([number, title, text]) => <article key={number}><span>{number}</span><h3>{title}</h3><p>{text}</p></article>)}
        </div>
      </section>
    </main>
  );
}

function Login({ client, navigate, onLogin }: { client: ApiClient; navigate: (path: string) => void; onLogin: () => void }) {
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true); setError("");
    const data = new FormData(event.currentTarget);
    try {
      await client.login(String(data.get("email")), String(data.get("password")));
      onLogin(); navigate("/dashboard");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Could not sign in.");
    } finally { setBusy(false); }
  }
  return (
    <main className="auth-wrap">
      <form className="auth-card" onSubmit={submit}>
        <p className="eyebrow">Welcome back</p>
        <h1>Sign in to ATSForge</h1>
        <label>Email<input name="email" type="email" placeholder="you@company.com" required /></label>
        <label>Password<input name="password" type="password" placeholder="At least 12 characters" required /></label>
        {error && <p className="error">{error}</p>}
        <button className="primary full" disabled={busy}>{busy ? "Signing in..." : "Sign in"}</button>
        <a className="google" href="/oauth2/authorization/google">Continue with Google</a>
        <p className="fine">New accounts are verified by email before resume data can be accessed.</p>
      </form>
    </main>
  );
}

function Dashboard({ client, signedIn, navigate, edit }: {
  client: ApiClient; signedIn: boolean; navigate: (path: string) => void; edit: (resume: Resume) => void;
}) {
  const [resumes, setResumes] = useState<Resume[]>([sampleResume]);
  useEffect(() => {
    if (signedIn) client.listResumes().then(result => setResumes(result.content)).catch(() => setResumes([sampleResume]));
  }, [client, signedIn]);
  async function create() {
    if (!signedIn) { edit(sampleResume); return; }
    const resume = await client.createResume("Untitled Resume");
    edit(resume);
  }
  return (
    <main className="dashboard">
      <div className="dashboard-head">
        <div><p className="eyebrow">Workspace</p><h1>Your resumes</h1><p>Manage versions, tailoring and recruiter activity in one place.</p></div>
        <button className="primary" onClick={create}>+ New resume</button>
      </div>
      {!signedIn && <div className="demo-banner">Preview workspace mode. <button onClick={() => navigate("/login")}>Sign in</button> to persist drafts and exports.</div>}
      <section className="stat-grid">
        <Metric label="Resume views" value="148" change="+24%" />
        <Metric label="Downloads" value="31" change="+8%" />
        <Metric label="Average ATS score" value="89" change="+6 pts" />
        <Metric label="AI improvements" value="46" change="this month" />
      </section>
      <section className="resume-grid">
        {resumes.map(resume => (
          <article className="resume-tile" key={resume.id} onClick={() => edit(resume)}>
            <div className="mini-page"><div></div><b></b><span></span><span></span><span></span></div>
            <h3>{resume.title}</h3>
            <p>{resume.targetRole || "No target role"} <em>{resume.status}</em></p>
            <footer><span>v{resume.versionNumber}</span><button>Edit</button></footer>
          </article>
        ))}
        <article className="create-tile" onClick={create}><strong>+</strong><p>Start a tailored resume</p></article>
      </section>
    </main>
  );
}

function Metric({ label, value, change }: { label: string; value: string; change: string }) {
  return <article className="metric"><p>{label}</p><strong>{value}</strong><small>{change}</small></article>;
}

function Editor({ client, signedIn, initial }: { client: ApiClient; signedIn: boolean; initial: Resume }) {
  const [resume, setResume] = useState(initial);
  const [selected, setSelected] = useState(initial.sections[1].id);
  const [saved, setSaved] = useState("All changes saved");
  const [dragging, setDragging] = useState<string>();
  const [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [jobDescription, setJobDescription] = useState("Lead product designer with design systems, accessibility, user research and measurable product outcomes.");
  const firstUpdate = useRef(true);
  const active = resume.sections.find(section => section.id === selected) ?? resume.sections[0];

  useEffect(() => {
    if (firstUpdate.current) { firstUpdate.current = false; return; }
    setSaved("Saving...");
    const timer = window.setTimeout(async () => {
      if (signedIn && resume.id !== "demo") {
        try { await client.autosave(resume); setSaved("Saved just now"); } catch { setSaved("Could not save"); }
      } else setSaved("Preview draft");
    }, 650);
    return () => window.clearTimeout(timer);
  }, [client, resume, signedIn]);

  function updateContent(content: string) {
    setResume(current => ({ ...current, sections: current.sections.map(section => section.id === active.id ? { ...section, content } : section) }));
  }
  function drop(onId: string) {
    if (!dragging || dragging === onId) return;
    setResume(current => {
      const items = [...current.sections];
      const sourceIndex = items.findIndex(item => item.id === dragging);
      const destinationIndex = items.findIndex(item => item.id === onId);
      const [item] = items.splice(sourceIndex, 1);
      items.splice(destinationIndex, 0, item);
      return { ...current, sections: items.map((section, index) => ({ ...section, order: index })) };
    });
    setDragging(undefined);
  }
  async function analyze() {
    if (signedIn && resume.id !== "demo") {
      const queued = await client.analyze(resume.id, jobDescription);
      setAnalysis(queued);
      window.setTimeout(async () => setAnalysis(await client.analysis(queued.id)), 1300);
      return;
    }
    setAnalysis({ id: "sample", status: "COMPLETED", atsScore: 92, matchPercentage: 86, result: {
      missingKeywords: ["workshop facilitation", "WCAG"],
      suggestions: ["Add an accessibility audit outcome to your experience.", "Name the activation metric baseline in the onboarding bullet."],
      strongSections: ["Experience"], weakSections: ["Skills"],
    } });
  }
  function addSection() {
    const id = crypto.randomUUID();
    setResume(current => ({ ...current, sections: [...current.sections,
      { id, type: "CUSTOM", title: "New section", visible: true, order: current.sections.length, content: "" }] }));
    setSelected(id);
  }
  return (
    <main className="editor">
      <div className="editor-toolbar">
        <div><button className="back" onClick={() => { location.hash = "/dashboard"; }}>←</button>
          <input className="title-input" value={resume.title} onChange={e => setResume({ ...resume, title: e.target.value })} />
          <span className="save-state">{saved}</span></div>
        <div className="export-actions">
          {["PDF", "DOCX", "TXT"].map(format => <button key={format} onClick={() => signedIn && resume.id !== "demo" && client.export(resume.id, format)}>{format}</button>)}
          <button className="primary compact">Share</button>
        </div>
      </div>
      <div className="editor-workbench">
        <aside className="sections-panel">
          <header><h2>Sections</h2><button onClick={addSection}>+</button></header>
          {resume.sections.map(section => (
            <button draggable key={section.id} className={selected === section.id ? "section-row active" : "section-row"}
              onDragStart={() => setDragging(section.id)} onDragOver={event => event.preventDefault()} onDrop={() => drop(section.id)}
              onClick={() => setSelected(section.id)}>
              <span className="handle">⋮⋮</span>{section.title}<small>{section.visible ? "On" : "Off"}</small>
            </button>
          ))}
          <div className="palette">
            <label>Accent color<input type="color" value={resume.theme.primaryColor}
              onChange={event => setResume({ ...resume, theme: { ...resume.theme, primaryColor: event.target.value } })} /></label>
            <label>Template<select value={resume.templateCode} onChange={event => setResume({ ...resume, templateCode: event.target.value })}>
              <option value="atlas">Atlas</option><option value="meridian">Meridian</option><option value="boardroom">Boardroom</option>
            </select></label>
          </div>
        </aside>
        <section className="content-panel">
          <p className="eyebrow">Edit content</p>
          <input className="section-title" value={active.title} onChange={event =>
            setResume(current => ({ ...current, sections: current.sections.map(item => item.id === active.id ? { ...item, title: event.target.value } : item) }))} />
          <textarea value={typeof active.content === "string" ? active.content : JSON.stringify(active.content)}
            onChange={event => updateContent(event.target.value)} />
          <div className="writing-tools">
            <button>Improve writing</button><button>Shorten</button><button>Quantify impact</button>
          </div>
        </section>
        <section className="preview-panel"><ResumePage resume={resume} /></section>
        <aside className="analysis-panel">
          <h2>AI analysis</h2>
          <label>Job description<textarea value={jobDescription} onChange={event => setJobDescription(event.target.value)} /></label>
          <button className="primary full" onClick={analyze}>Analyze match</button>
          {analysis?.status === "QUEUED" || analysis?.status === "PROCESSING" ? <p className="processing">Analysis running...</p> : analysis?.result && (
            <div className="analysis-result">
              <div className="score"><strong>{analysis.atsScore}</strong><span>ATS score</span><b>{analysis.matchPercentage}% match</b></div>
              <h3>Opportunities</h3>
              {analysis.result.suggestions.map(item => <p key={item}>{item}</p>)}
              <h3>Missing keywords</h3>
              <div className="tags">{analysis.result.missingKeywords.map(item => <span key={item}>{item}</span>)}</div>
            </div>
          )}
        </aside>
      </div>
    </main>
  );
}

function ResumePage({ resume, condensed = false }: { resume: Resume; condensed?: boolean }) {
  return (
    <article className={condensed ? "resume-page condensed" : "resume-page"} style={{ "--accent": resume.theme.primaryColor } as CSSProperties}>
      <h1>{resume.title}</h1>
      {resume.sections.filter(section => section.visible).map(section => (
        <section key={section.id}>
          <h2>{section.title}</h2>
          <p>{typeof section.content === "string" ? section.content : JSON.stringify(section.content)}</p>
        </section>
      ))}
    </article>
  );
}

function Templates({ navigate }: { navigate: (path: string) => void }) {
  return (
    <main className="catalog">
      <p className="eyebrow center">Template gallery</p><h1>Built for people and parsers</h1>
      <div className="filters"><button className="selected">All</button><button>ATS-friendly</button><button>Modern</button><button>Corporate</button><button>Minimal</button></div>
      <section className="template-grid">
        {templates.map(template => <article key={template.name}>
          <div className="template-page" style={{ "--accent": template.color } as CSSProperties}><b></b><span></span><span></span><span></span><i></i></div>
          <div><h2>{template.name}</h2><p>{template.type}</p></div>
          <label>{template.free ? "Free" : "PRO"}</label><button onClick={() => navigate("/editor")}>Use template</button>
        </article>)}
      </section>
    </main>
  );
}

function Pricing({ navigate }: { navigate: (path: string) => void }) {
  const plans = [
    { name: "Free", price: "$0", features: ["2 active resumes", "5 PDF exports monthly", "3 AI checks hourly"] },
    { name: "Pro", price: "$14", featured: true, features: ["Unlimited resumes & exports", "AI rewrites and job matching", "Premium templates and analytics"] },
    { name: "Enterprise", price: "Talk to us", features: ["Team review workflow", "SSO and controls", "Usage reporting and SLA"] },
  ];
  return (
    <main className="pricing">
      <p className="eyebrow center">Plans</p><h1>Career tools that grow with ambition</h1>
      <div className="billing-toggle">Monthly <span>Yearly · save 20%</span></div>
      <section>
        {plans.map(plan => <article key={plan.name} className={plan.featured ? "featured" : ""}>
          {plan.featured && <label>Most popular</label>}<h2>{plan.name}</h2><strong>{plan.price}</strong>{plan.price.startsWith("$") && <small>/ month</small>}
          {plan.features.map(feature => <p key={feature}>✓ {feature}</p>)}
          <button className={plan.featured ? "primary full" : "secondary full"} onClick={() => navigate("/login")}>
            {plan.name === "Enterprise" ? "Contact sales" : `Choose ${plan.name}`}
          </button>
        </article>)}
      </section>
    </main>
  );
}

function Admin() {
  return (
    <main className="dashboard admin">
      <div className="dashboard-head"><div><p className="eyebrow">Operations</p><h1>Platform overview</h1></div><button className="secondary">Export report</button></div>
      <section className="stat-grid"><Metric label="Active users" value="24,890" change="+12%" /><Metric label="Pro MRR" value="$83.4k" change="+9.3%" /><Metric label="AI requests" value="1.2m" change="99.94% healthy" /><Metric label="PDF jobs" value="98.7k" change="p95 1.4 sec" /></section>
      <div className="admin-grid">
        <article><h2>Usage health</h2><div className="bars"><i></i><i></i><i></i><i></i><i></i><i></i><i></i></div><p>AI analysis calls over seven days</p></article>
        <article><h2>Recent audit events</h2><p><b>Template published</b> Atlas revision 12</p><p><b>Webhook reconciled</b> subscription.updated</p><p><b>Role changed</b> User upgraded to Pro</p></article>
      </div>
    </main>
  );
}

function Footer() {
  return <footer className="site-footer"><div className="brand"><span className="brand-mark">A</span>ATS<strong>Forge</strong></div><p>Secure resume intelligence for purposeful applications.</p><span>© 2026 ATSForge</span></footer>;
}

export default App;
