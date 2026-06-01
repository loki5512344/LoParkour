-- Создание таблицы для адаптивных метрик
CREATE TABLE IF NOT EXISTS loparkour_player_stats (
    player_uuid VARCHAR(36) PRIMARY KEY,
    skill_rating DOUBLE DEFAULT 1.0,
    sessions_count INT DEFAULT 0,
    total_jumps INT DEFAULT 0,
    total_falls INT DEFAULT 0,
    longest_streak INT DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_skill_rating (skill_rating),
    INDEX idx_last_updated (last_updated)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Миграция существующих данных (если есть старая таблица)
INSERT INTO loparkour_player_stats (player_uuid, sessions_count, last_updated)
SELECT player_uuid, COUNT(*) as sessions, MAX(timestamp) as last_updated
FROM loparkour_scores
GROUP BY player_uuid
ON DUPLICATE KEY UPDATE sessions_count = VALUES(sessions_count);
