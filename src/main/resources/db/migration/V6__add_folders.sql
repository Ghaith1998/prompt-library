-- Folders are user-owned, nest through parent_id, and collect a user's prompts.

CREATE TABLE folders (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  owner_id   VARCHAR(36) NOT NULL,
  parent_id  BIGINT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_folders_owner_name UNIQUE (owner_id, name),
  CONSTRAINT fk_folders_parent FOREIGN KEY (parent_id) REFERENCES folders (id)
);

CREATE INDEX idx_folders_owner ON folders (owner_id);
CREATE INDEX idx_folders_parent ON folders (parent_id);

-- Every existing prompt owner gets a root default folder.
INSERT INTO folders (name, owner_id, parent_id, created_at, updated_at)
SELECT 'default', p.owner_id, NULL, NOW(6), NOW(6)
FROM (SELECT DISTINCT owner_id FROM prompts) p;

ALTER TABLE prompts ADD COLUMN folder_id BIGINT NULL;
CREATE INDEX idx_prompts_folder ON prompts (folder_id);

-- Existing prompts move into their owner's default folder.
UPDATE prompts p
JOIN folders f
  ON f.owner_id = p.owner_id
 AND f.name = 'default'
 AND f.parent_id IS NULL
SET p.folder_id = f.id
WHERE p.folder_id IS NULL;

ALTER TABLE prompts MODIFY folder_id BIGINT NOT NULL;
ALTER TABLE prompts ADD CONSTRAINT fk_prompts_folder
  FOREIGN KEY (folder_id) REFERENCES folders (id);
