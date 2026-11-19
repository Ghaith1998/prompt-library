-- A typical "evolve the schema" migration: add a column with a safe default.
ALTER TABLE prompts ADD COLUMN is_favorite BOOLEAN NOT NULL DEFAULT FALSE;
