CREATE TABLE IF NOT EXISTS recruitment
(
    id BIGSERIAL PRIMARY KEY,
    writer_member_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'RECRUITING',
    deadline_at TIMESTAMP NOT NULL,
    play_time_hours INTEGER,
    play_time_text VARCHAR(255),
    overview TEXT,
    caution TEXT,
    notice TEXT,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    completion_message TEXT,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recruitment_list
    ON recruitment (status, deadline_at, id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_recruitment_writer
    ON recruitment (writer_member_id, id)
    WHERE deleted_at IS NULL;

-- 목록 필터 쿼리(details -> 'rules' @> ..., details -> 'environments' @> ...)와
-- 동일한 표현식에 GIN 인덱스를 걸어야 인덱스가 사용된다.
CREATE INDEX IF NOT EXISTS idx_recruitment_details_rules_gin
    ON recruitment USING GIN ((details -> 'rules'));

CREATE INDEX IF NOT EXISTS idx_recruitment_details_environments_gin
    ON recruitment USING GIN ((details -> 'environments'));

-- 제목/닉네임 부분 일치 검색용 트라이그램 인덱스
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_recruitment_title_trgm
    ON recruitment USING GIN (title gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_member_nickname_trgm
    ON member USING GIN (nickname gin_trgm_ops);

CREATE TABLE IF NOT EXISTS recruitment_application
(
    id BIGSERIAL PRIMARY KEY,
    recruitment_id BIGINT NOT NULL,
    applicant_member_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    selected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_recruitment_application_member
    ON recruitment_application (recruitment_id, applicant_member_id);

CREATE INDEX IF NOT EXISTS idx_recruitment_application_applicant
    ON recruitment_application (applicant_member_id, id);

CREATE TABLE IF NOT EXISTS recruit_alert
(
    member_id BIGINT PRIMARY KEY,
    last_checked_hosting_at TIMESTAMP,
    last_checked_applied_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
