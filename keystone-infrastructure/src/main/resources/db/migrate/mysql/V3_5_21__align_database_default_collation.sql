-- Align the current schema default with the application connection and table DDL.
-- Existing tables still need explicit conversion; V3_5_20 covers the legacy API tables.

ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
