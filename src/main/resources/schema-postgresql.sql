-- Database schema for TroPig Backend
-- This file will be executed when the application starts

-- Create users table
CREATE TABLE member (
    id BIGSERIAL PRIMARY KEY,
    sns_id VARCHAR(255) NOT NULL,
    sns_provider VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    nickname VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    adult BOOLEAN NOT NULL DEFAULT FALSE,
    bio VARCHAR(255),
    marketing_at TIMESTAMP,
    deleted_at TIMESTAMP,
    favorite_genres VARCHAR(255),
    favorite_rules VARCHAR(255),
    profile VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- content
CREATE TABLE content (
    id BIGSERIAL PRIMARY KEY,
    alias VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    type VARCHAR(10) NOT NULL,
    member_id BIGINT NOT NULL,
    rule VARCHAR(255) NOT NULL,
    genre VARCHAR(255) NOT NULL,
    player_count_type VARCHAR(20) NOT NULL,
    term_type VARCHAR(20) NOT NULL,
    publishing_info TEXT,
    status VARCHAR(20) NOT NULL,
    adult BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    free_content TEXT,
    non_free_content TEXT,
    price DOUBLE PRECISION NOT NULL,
    `level` INTEGER NOT NULL,
    search_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 검색 gin index
CREATE INDEX IF NOT EXISTS idx_content_search_text_trgm
ON content USING gin (lower(search_text) gin_trgm_ops);

-- 최신순 정렬 인덱스
CREATE INDEX IF NOT EXISTS idx_content_latest_base
ON content(type, status, published_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_content_latest_adult_false
ON content(type, status, published_at DESC, id DESC)
WHERE adult = false;

-- 제목순 정렬 인덱스
CREATE INDEX IF NOT EXISTS idx_content_title_base
ON content(type, status, lower(title), id);

CREATE INDEX IF NOT EXISTS idx_content_title_adult_false
ON content(type, status, lower(title), id)
WHERE adult = false;

CREATE INDEX IF NOT EXISTS idx_content_alias
ON content(alias);


CREATE TABLE pick_content (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL,
    order_no INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE content_thumbnail (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL,
    path VARCHAR(255) NOT NULL,
    height INT NOT NULL,
    weight INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE content_tag (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE report_content (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    member_id BIGINT NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE bookmark_content (
    id BIGSERIAL PRIMARY KEY,
    content_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    deleted BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 중복 북마크 방지 (soft delete 포함 고려)
CREATE UNIQUE INDEX ux_bookmark_content_member
ON bookmark_content (content_id, member_id);

-- 조회 최적화
CREATE INDEX idx_bookmark_content_member
ON bookmark_content (member_id, deleted);

CREATE INDEX idx_bookmark_content
ON bookmark_content (content_id, deleted);

CREATE INDEX idx_bookmark_content_member_deleted
ON bookmark_content (content_id, member_id)
WHERE deleted = false;



CREATE TABLE tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);


-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_member_nickname ON member(nickname);
CREATE INDEX IF NOT EXISTS idx_member_email ON member(email);

CREATE UNIQUE INDEX idx_content_alias ON content(alias);