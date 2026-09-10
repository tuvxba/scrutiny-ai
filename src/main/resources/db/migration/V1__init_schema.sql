-- V1__init_schema.sql
-- Scrutiny AI - ilk şema

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    language VARCHAR(50) NOT NULL,
    code_snippet TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    score INTEGER,
    ai_summary TEXT,
    user_id BIGINT NOT NULL,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE issues (
    id BIGSERIAL PRIMARY KEY,
    severity VARCHAR(20) NOT NULL,
    line_number INTEGER,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    suggestion TEXT,
    review_id BIGINT NOT NULL,
    CONSTRAINT fk_issues_review FOREIGN KEY (review_id) REFERENCES reviews (id)
);

CREATE TABLE ai_fixes (
    id BIGSERIAL PRIMARY KEY,
    original_snippet TEXT NOT NULL,
    fixed_snippet TEXT NOT NULL,
    applied BOOLEAN NOT NULL DEFAULT FALSE,
    issue_id BIGINT NOT NULL,
    CONSTRAINT fk_ai_fixes_issue FOREIGN KEY (issue_id) REFERENCES issues (id)
);

CREATE TABLE generated_tests (
    id BIGSERIAL PRIMARY KEY,
    test_code TEXT NOT NULL,
    review_id BIGINT NOT NULL,
    CONSTRAINT fk_generated_tests_review FOREIGN KEY (review_id) REFERENCES reviews (id)
);

-- Sık kullanılan sorgular için indexler
CREATE INDEX idx_reviews_user_id ON reviews (user_id);
CREATE INDEX idx_issues_review_id ON issues (review_id);
CREATE INDEX idx_ai_fixes_issue_id ON ai_fixes (issue_id);
CREATE INDEX idx_generated_tests_review_id ON generated_tests (review_id);