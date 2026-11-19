-- Steps 2 + 3: backfill old rows, then make the column mandatory.
UPDATE prompts SET owner_id = 'unknown' WHERE owner_id IS NULL;
ALTER TABLE prompts MODIFY owner_id VARCHAR(36) NOT NULL;
