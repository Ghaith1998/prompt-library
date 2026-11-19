CREATE TABLE categories (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(100) NOT NULL,
  created_at DATETIME(6)  NOT NULL,
  updated_at DATETIME(6)  NOT NULL,
  CONSTRAINT uq_categories_name UNIQUE (name)
);

CREATE TABLE tags (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  name       VARCHAR(50) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_tags_name UNIQUE (name)
);

CREATE TABLE prompts (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(200) NOT NULL,
  content     TEXT         NOT NULL,
  status      VARCHAR(20)  NOT NULL,
  category_id BIGINT       NOT NULL,
  version     BIGINT       NOT NULL DEFAULT 0,
  created_at  DATETIME(6)  NOT NULL,
  updated_at  DATETIME(6)  NOT NULL,
  CONSTRAINT fk_prompts_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

-- n:m join table (owned by Prompt.tags)
CREATE TABLE prompt_tags (
  prompt_id BIGINT NOT NULL,
  tag_id    BIGINT NOT NULL,
  PRIMARY KEY (prompt_id, tag_id),
  CONSTRAINT fk_pt_prompt FOREIGN KEY (prompt_id) REFERENCES prompts (id) ON DELETE CASCADE,
  CONSTRAINT fk_pt_tag    FOREIGN KEY (tag_id)    REFERENCES tags (id)    ON DELETE CASCADE
);
