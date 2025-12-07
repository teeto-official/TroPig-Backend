-- H2 Database schema for TroPig Backend with auto-updated `updated_at`

-- Create member table
CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
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

-- Create member table
CREATE TABLE IF NOT EXISTS member_account (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    alias VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,

    type VARCHAR(255) NOT NULL,
    member_id BIGINT NOT NULL,
    rule VARCHAR(255) NOT NULL,
    genre VARCHAR(255) NOT NULL,
    player_count_type VARCHAR(255) NOT NULL,
    term_type VARCHAR(255) NOT NULL,

    publishing_info CLOB,
    status VARCHAR(255) NOT NULL,
    adult BOOLEAN NOT NULL DEFAULT FALSE,

    published_at TIMESTAMP,
    free_content CLOB,
    non_free_content CLOB,

    price DOUBLE NOT NULL,
    level INT NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP(),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
);

CREATE TABLE pick_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    content_id BIGINT NOT NULL,
    order_no INT NOT NULL,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP()
);

-- Indexes
CREATE INDEX idx_users_nickname ON member(nickname);
CREATE INDEX idx_users_email ON member(email);
