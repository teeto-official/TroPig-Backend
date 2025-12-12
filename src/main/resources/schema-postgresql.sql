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
    level INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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

CREATE TABLE tag (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    parent_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);


-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_member_nickname ON member(nickname);
CREATE INDEX IF NOT EXISTS idx_member_email ON member(email);

CREATE UNIQUE INDEX idx_content_alias ON content(alias);