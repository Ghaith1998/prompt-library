-- Step 1 of the safe 3-step pattern for NOT NULL columns on tables that may already contain data:
--   (1) add the column as NULLable  (2) backfill  (3) tighten to NOT NULL  (see V4)
-- owner_id = the Keycloak user id (the "sub" claim of the token). There is no users table on purpose.
ALTER TABLE prompts ADD COLUMN owner_id VARCHAR(36) NULL;
CREATE INDEX idx_prompts_owner ON prompts (owner_id);
