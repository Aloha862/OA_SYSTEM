ALTER TABLE oa_schedule
    ADD INDEX idx_schedule_lifecycle (deleted, status, end_time);

ALTER TABLE oa_approval
    ADD INDEX idx_approval_overdue (deleted, status, submitted_at);
