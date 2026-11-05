CREATE TABLE location_sharing_sessions (
    session_id CHAR(36) NOT NULL,
    user_id INT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    latitude DECIMAL(10,8) NULL,
    longitude DECIMAL(11,8) NULL,
    accuracy_meters DECIMAL(10,2) NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    PRIMARY KEY (session_id),
    UNIQUE KEY uk_location_sharing_sessions_token_hash (token_hash),
    KEY idx_location_sharing_sessions_user_status (user_id, status, created_at),
    KEY idx_location_sharing_sessions_expiry_status (expires_at, status),
    CONSTRAINT fk_location_sharing_sessions_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);
