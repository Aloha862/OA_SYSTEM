SET @event_column_count = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notification' AND COLUMN_NAME = 'event_id'
);
SET @event_column_sql = IF(
    @event_column_count = 0,
    'ALTER TABLE sys_notification ADD COLUMN event_id VARCHAR(64) NULL COMMENT ''消息幂等ID'' AFTER id',
    'SELECT 1'
);
PREPARE event_column_statement FROM @event_column_sql;
EXECUTE event_column_statement;
DEALLOCATE PREPARE event_column_statement;

SET @event_index_count = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_notification' AND INDEX_NAME = 'uk_event_receiver'
);
SET @event_index_sql = IF(
    @event_index_count = 0,
    'ALTER TABLE sys_notification ADD UNIQUE KEY uk_event_receiver (event_id, receiver_id)',
    'SELECT 1'
);
PREPARE event_index_statement FROM @event_index_sql;
EXECUTE event_index_statement;
DEALLOCATE PREPARE event_index_statement;

CREATE TABLE IF NOT EXISTS sys_ai_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL DEFAULT '新对话',
    mode VARCHAR(40) NOT NULL DEFAULT 'QA',
    model_name VARCHAR(100),
    last_message_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_ai_conversation_user (user_id, deleted, last_message_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话';

CREATE TABLE IF NOT EXISTS sys_ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    client_message_id VARCHAR(64),
    parent_message_id BIGINT,
    content LONGTEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    model_name VARCHAR(100),
    cost_time_ms BIGINT,
    error_code VARCHAR(50),
    error_message VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    UNIQUE KEY uk_ai_client_message (user_id, client_message_id),
    INDEX idx_ai_message_conversation (conversation_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI会话消息';
