CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(10),
    recipient VARCHAR(255),
    subject VARCHAR(255),
    content TEXT,
    created_at TEXT
);
