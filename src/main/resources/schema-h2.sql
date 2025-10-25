-- H2 Database schema for TroPig Backend with auto-updated `updated_at`

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Trigger for users.updated_at
CREATE TRIGGER users_updated_at_trigger
AFTER UPDATE ON users
FOR EACH ROW
CALL "org.h2.samples.TriggerUpdatedAt";

-- Create posts table
CREATE TABLE IF NOT EXISTS posts (
    id BIGINT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content CLOB,
    author_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Trigger for posts.updated_at
CREATE TRIGGER posts_updated_at_trigger
AFTER UPDATE ON posts
FOR EACH ROW
CALL "org.h2.samples.TriggerUpdatedAt";

-- Create comments table
CREATE TABLE IF NOT EXISTS comments (
    id BIGINT PRIMARY KEY,
    content CLOB NOT NULL,
    post_id BIGINT,
    author_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Trigger for comments.updated_at
CREATE TRIGGER comments_updated_at_trigger
AFTER UPDATE ON comments
FOR EACH ROW
CALL "org.h2.samples.TriggerUpdatedAt";

-- Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_posts_author_id ON posts(author_id);
CREATE INDEX idx_posts_created_at ON posts(created_at);
CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_author_id ON comments(author_id);
