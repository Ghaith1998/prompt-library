-- Tags become user-owned: unique per owner instead of globally unique.

-- 1. Add the owner column while the old rows are still present.
ALTER TABLE tags ADD COLUMN owner_id VARCHAR(36) NULL;
ALTER TABLE tags DROP INDEX uq_tags_name;

-- 2. Give every used tag one owner. Shared tags keep this owner on the original row.
UPDATE tags t
JOIN (
    SELECT pt.tag_id, MIN(p.owner_id) AS owner_id
    FROM prompt_tags pt
    JOIN prompts p ON p.id = pt.prompt_id
    GROUP BY pt.tag_id
) x ON x.tag_id = t.id
SET t.owner_id = x.owner_id;

-- 3. Duplicate shared tags for the other owners.
INSERT INTO tags (name, owner_id, created_at, updated_at)
SELECT t.name, p.owner_id, NOW(6), NOW(6)
FROM tags t
JOIN prompt_tags pt ON pt.tag_id = t.id
JOIN prompts p ON p.id = pt.prompt_id
WHERE t.owner_id IS NOT NULL
  AND p.owner_id <> t.owner_id
GROUP BY t.id, t.name, p.owner_id;

-- 4. Point each prompt at the tag copy owned by the prompt owner.
UPDATE prompt_tags pt
JOIN prompts p ON p.id = pt.prompt_id
JOIN tags t ON t.id = pt.tag_id
JOIN tags copy ON copy.name = t.name AND copy.owner_id = p.owner_id
SET pt.tag_id = copy.id
WHERE t.owner_id <> p.owner_id;

-- 5. Unused tags have no prompt owner; keep them under the legacy owner.
UPDATE tags SET owner_id = 'unknown' WHERE owner_id IS NULL;

-- 6. Enforce ownership and per-owner uniqueness.
ALTER TABLE tags MODIFY owner_id VARCHAR(36) NOT NULL;
CREATE UNIQUE INDEX uq_tags_owner_name ON tags (owner_id, name);
CREATE INDEX idx_tags_owner ON tags (owner_id);
