-- Sample data for TroPig Backend
-- This file will be executed when the application starts (only in dev environment)

-- Insert sample users
INSERT INTO member (username, email, password_hash, first_name, last_name) VALUES
('admin', 'admin@tropig.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'Admin', 'User'),
('john_doe', 'john@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'John', 'Doe'),
('jane_smith', 'jane@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'Jane', 'Smith')
ON CONFLICT (username) DO NOTHING;