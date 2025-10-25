-- H2 Database schema for TroPig Backend with auto-updated `updated_at`

-- Create member table
CREATE TABLE IF NOT EXISTS member (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trigger for member.updated_at
CREATE TRIGGER member_updated_at_trigger
AFTER UPDATE ON member
FOR EACH ROW
CALL "org.h2.samples.TriggerUpdatedAt";

-- Indexes
CREATE INDEX idx_users_username ON member(username);
CREATE INDEX idx_users_email ON member(email);
