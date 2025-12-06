ALTER TABLE shift_assignments
    ADD COLUMN IF NOT EXISTS skill_id BIGINT;

ALTER TABLE shift_assignments
    ADD CONSTRAINT IF NOT EXISTS fk_shift_assignments_skill
        FOREIGN KEY (skill_id) REFERENCES skills(id);

CREATE INDEX IF NOT EXISTS idx_shift_assignments_skill
    ON shift_assignments(skill_id);
