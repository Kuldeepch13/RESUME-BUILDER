CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255),
    display_name VARCHAR(120) NOT NULL,
    avatar_url VARCHAR(500),
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    auth_provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locale VARCHAR(12) NOT NULL DEFAULT 'en-US',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE auth_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    token_type VARCHAR(30) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    device_info VARCHAR(255),
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_auth_tokens_user_type ON auth_tokens(user_id, token_type, expires_at);

CREATE TABLE templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(30) NOT NULL,
    description VARCHAR(300) NOT NULL,
    thumbnail_url VARCHAR(500),
    html_layout TEXT NOT NULL,
    css_styles TEXT NOT NULL,
    premium BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE resumes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    template_id UUID REFERENCES templates(id),
    title VARCHAR(160) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    target_role VARCHAR(120),
    sections_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    theme_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    version_number INTEGER NOT NULL DEFAULT 1,
    last_exported_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_resume_owner_slug UNIQUE(owner_id, slug)
);
CREATE INDEX idx_resumes_owner_updated ON resumes(owner_id, updated_at DESC);
CREATE INDEX idx_resumes_sections_gin ON resumes USING GIN(sections_json);

CREATE TABLE resume_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    sections_json JSONB NOT NULL,
    theme_json JSONB NOT NULL,
    change_summary VARCHAR(240),
    created_by UUID NOT NULL REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_resume_version UNIQUE(resume_id, version_number)
);
CREATE INDEX idx_versions_resume_created ON resume_versions(resume_id, created_at DESC);

CREATE TABLE resume_shares (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    public_token VARCHAR(80) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    password_hash VARCHAR(255),
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE resume_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    share_id UUID REFERENCES resume_shares(id) ON DELETE SET NULL,
    event_type VARCHAR(30) NOT NULL,
    visitor_hash CHAR(64),
    referrer VARCHAR(500),
    user_agent VARCHAR(300),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_events_resume_time ON resume_events(resume_id, occurred_at DESC);

CREATE TABLE ai_analyses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    requested_by UUID NOT NULL REFERENCES app_users(id),
    analysis_type VARCHAR(30) NOT NULL,
    job_description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    ats_score INTEGER,
    match_percentage INTEGER,
    result_json JSONB,
    model VARCHAR(80),
    input_tokens INTEGER,
    output_tokens INTEGER,
    error_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);
CREATE INDEX idx_ai_resume_created ON ai_analyses(resume_id, created_at DESC);
CREATE INDEX idx_ai_user_created ON ai_analyses(requested_by, created_at DESC);

CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    plan VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    stripe_customer_id VARCHAR(100),
    stripe_subscription_id VARCHAR(100) UNIQUE,
    current_period_end TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_subscription_user_status ON subscriptions(user_id, status);

CREATE TABLE payment_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stripe_event_id VARCHAR(120) NOT NULL UNIQUE,
    event_type VARCHAR(80) NOT NULL,
    payload_json JSONB NOT NULL,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE export_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    resume_id UUID NOT NULL REFERENCES resumes(id) ON DELETE CASCADE,
    requested_by UUID NOT NULL REFERENCES app_users(id),
    format VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    object_key VARCHAR(500),
    error_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);
CREATE INDEX idx_exports_user_created ON export_jobs(requested_by, created_at DESC);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id UUID REFERENCES app_users(id),
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_address VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_entity_created ON audit_logs(entity_type, entity_id, created_at DESC);

INSERT INTO templates (code, name, category, description, html_layout, css_styles, premium) VALUES
('atlas', 'Atlas', 'ATS_FRIENDLY', 'A clean, parsing-first layout for technical applications.', '<main class="resume"><header><h1>{{name}}</h1><p>{{headline}}</p></header><section>{{content}}</section></main>', '.resume{font-family:Arial;color:#172334} h1{font-size:30px}', FALSE),
('meridian', 'Meridian', 'MODERN', 'Elegant two-column storytelling for experienced professionals.', '<main class="resume meridian"><header><h1>{{name}}</h1></header><section>{{content}}</section></main>', '.meridian{font-family:Inter;color:#111827} h1{color:#115e59}', TRUE),
('boardroom', 'Boardroom', 'CORPORATE', 'Executive style with measured typography and hierarchy.', '<main class="resume corporate"><header><h1>{{name}}</h1></header><section>{{content}}</section></main>', '.corporate{font-family:Georgia;color:#111827} h1{border-bottom:2px solid #bf8a4b}', TRUE);

