-- Sample data for TroPig Backend
-- This file will be executed when the application starts (only in dev environment)

-- Insert sample users
INSERT INTO users (username, email, password_hash, first_name, last_name) VALUES
('admin', 'admin@tropig.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'Admin', 'User'),
('john_doe', 'john@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'John', 'Doe'),
('jane_smith', 'jane@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi', 'Jane', 'Smith')
ON CONFLICT (username) DO NOTHING;

-- Insert sample posts
INSERT INTO posts (title, content, author_id) VALUES
('Welcome to TroPig!', 'This is the first post on TroPig platform. Welcome everyone!', 1),
('Getting Started with TroPig', 'Here are some tips to get started with our platform...', 2),
('Advanced Features', 'Let me show you some advanced features you can use...', 3)
ON CONFLICT DO NOTHING;

-- Insert sample comments
INSERT INTO comments (content, post_id, author_id) VALUES
('Great post! Looking forward to more content.', 1, 2),
('Thanks for sharing this information.', 1, 3),
('This is really helpful, thank you!', 2, 1),
('I have a question about this feature...', 2, 3),
('Amazing work! Keep it up.', 3, 1),
('This is exactly what I was looking for.', 3, 2)
ON CONFLICT DO NOTHING;

