CREATE TABLE users (
    id                    BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(120) NOT NULL,
    cpf                   VARCHAR(11)  NOT NULL UNIQUE,
    email                 VARCHAR(160) NOT NULL UNIQUE,
    password_hash         VARCHAR(100) NOT NULL,
    birth_date            DATE         NOT NULL,
    role                  VARCHAR(20)  NOT NULL CHECK (role IN ('CUSTOMER', 'ADMIN')),
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE quotes (
    id                  BIGSERIAL PRIMARY KEY,
    customer_id         BIGINT        NOT NULL REFERENCES users (id),
    line                VARCHAR(20)   NOT NULL CHECK (line IN ('AUTO', 'RESIDENCIAL', 'VIDA')),
    insured_amount      NUMERIC(14,2) NOT NULL CHECK (insured_amount > 0),
    customer_age        INT           NOT NULL,
    base_rate           NUMERIC(6,4)  NOT NULL,
    age_factor          NUMERIC(5,2)  NOT NULL,
    discount            NUMERIC(5,2)  NOT NULL DEFAULT 0,
    annual_premium      NUMERIC(14,2) NOT NULL,
    monthly_premium     NUMERIC(14,2) NOT NULL,
    origin              VARCHAR(20)   NOT NULL CHECK (origin IN ('DIRECT', 'PORTABILITY')),
    external_policy_id  VARCHAR(60),
    status              VARCHAR(20)   NOT NULL CHECK (status IN ('PENDING', 'CONVERTED')),
    created_at          TIMESTAMPTZ   NOT NULL,
    expires_at          TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_quotes_portability UNIQUE (customer_id, external_policy_id)
);
CREATE INDEX idx_quotes_customer ON quotes (customer_id);

CREATE SEQUENCE policy_number_seq START 1;

CREATE TABLE policies (
    id                  BIGSERIAL PRIMARY KEY,
    number              VARCHAR(30)   NOT NULL UNIQUE,
    customer_id         BIGINT        NOT NULL REFERENCES users (id),
    quote_id            BIGINT        NOT NULL UNIQUE REFERENCES quotes (id),
    line                VARCHAR(20)   NOT NULL CHECK (line IN ('AUTO', 'RESIDENCIAL', 'VIDA')),
    insured_amount      NUMERIC(14,2) NOT NULL,
    annual_premium      NUMERIC(14,2) NOT NULL,
    monthly_premium     NUMERIC(14,2) NOT NULL,
    status              VARCHAR(20)   NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED')),
    start_date          DATE          NOT NULL,
    end_date            DATE          NOT NULL,
    cancellation_reason VARCHAR(255),
    cancelled_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_policies_dates CHECK (end_date > start_date)
);
CREATE INDEX idx_policies_customer ON policies (customer_id);
-- RN-04: uma apólice ACTIVE por linha por cliente, garantido também no banco.
CREATE UNIQUE INDEX uq_policies_active_line ON policies (customer_id, line) WHERE status = 'ACTIVE';

CREATE SEQUENCE claim_protocol_seq START 1;

CREATE TABLE claims (
    id               BIGSERIAL PRIMARY KEY,
    protocol         VARCHAR(30)   NOT NULL UNIQUE,
    policy_id        BIGINT        NOT NULL REFERENCES policies (id),
    customer_id      BIGINT        NOT NULL REFERENCES users (id),
    type             VARCHAR(30)   NOT NULL,
    occurrence_date  DATE          NOT NULL,
    amount           NUMERIC(14,2) NOT NULL CHECK (amount > 0),
    approved_amount  NUMERIC(14,2),
    description      VARCHAR(1000) NOT NULL,
    status           VARCHAR(20)   NOT NULL CHECK (status IN ('OPEN', 'IN_ANALYSIS', 'APPROVED', 'REJECTED', 'PAID', 'CANCELLED')),
    rejection_reason VARCHAR(500),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_claims_policy ON claims (policy_id);
CREATE INDEX idx_claims_status ON claims (status);

CREATE TABLE consents (
    id          BIGSERIAL PRIMARY KEY,
    customer_id BIGINT      NOT NULL REFERENCES users (id),
    status      VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    granted_at  TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    -- Momento da última sincronização bem-sucedida com a OPIN (base do cache da RN-08)
    last_sync_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX uq_consents_active ON consents (customer_id) WHERE status = 'ACTIVE';

CREATE TABLE external_policies_cache (
    id             BIGSERIAL PRIMARY KEY,
    customer_id    BIGINT        NOT NULL REFERENCES users (id),
    external_id    VARCHAR(60)   NOT NULL,
    insurer        VARCHAR(120)  NOT NULL,
    line           VARCHAR(30)   NOT NULL,
    policy_number  VARCHAR(60)   NOT NULL,
    insured_amount NUMERIC(14,2) NOT NULL,
    annual_premium NUMERIC(14,2) NOT NULL,
    start_date     DATE          NOT NULL,
    end_date       DATE          NOT NULL,
    fetched_at     TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uq_external_cache UNIQUE (customer_id, external_id)
);

CREATE TABLE audit_log (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT REFERENCES users (id),
    action     VARCHAR(40)  NOT NULL,
    entity     VARCHAR(40),
    entity_id  VARCHAR(60),
    details    VARCHAR(1000),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_user ON audit_log (user_id);
CREATE INDEX idx_audit_action ON audit_log (action);
