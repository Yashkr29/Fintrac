-- Optional demo account for portfolio walkthroughs.
-- Username: demo
-- Password: Demo@123

USE fintrac;

INSERT INTO users (username, email, password, full_name)
VALUES (
    'demo',
    'demo@fintrac.local',
    '$2a$10$MNoZBfGV58r.ycaW9xx8XuMFHzWSzm0ZwKVkeayODGRExcFhSeY/.',
    'Demo User'
)
ON DUPLICATE KEY UPDATE
    email = VALUES(email),
    full_name = VALUES(full_name);
