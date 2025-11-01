-- Database schema for TroPig Backend
-- This file will be executed when the application starts

-- Create users table
CREATE TABLE IF NOT EXISTS member (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_member_username ON member(username);
CREATE INDEX IF NOT EXISTS idx_member_email ON member(email);
