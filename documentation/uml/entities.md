# UML: persistence model and repositories

## JPA domain classes

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        +Long id
        +Instant createdAt
        +Instant updatedAt
        +getId() Long
    }

    class Category {
        +String name
        +Set<Prompt> prompts
        +getName() String
        +equals(Object o) boolean
        +hashCode() int
    }

    class Folder {
        <<static>> DEFAULT_NAME: String = "default"
        +String name
        +String ownerId
        +Folder parent
        +Set<Folder> children
        +Set<Prompt> prompts
        +isDefaultFolder() boolean
    }

    class Tag {
        +String name
        +String ownerId
        +Set<Prompt> prompts
        +normalizeName(name) String$
        +equals(Object o) boolean
        +hashCode() int
    }

    class PromptStatus {
        <<enumeration>>
        DRAFT
        PUBLISHED
        ARCHIVED
    }

    class Prompt {
        +String title
        +String content
        +PromptStatus status
        +boolean favorite
        +String ownerId
        +Category category
        +Folder folder
        +Set<Tag> tags
        +long version
        +addTag(Tag tag)
        +removeTag(Tag tag)
    }

    BaseEntity <|-- Category
    BaseEntity <|-- Folder
    BaseEntity <|-- Tag
    BaseEntity <|-- Prompt
    Prompt ..> PromptStatus : status is string-mapped
    Prompt "1" --> "0..*" Tag : owning side via prompt_tags
    Tag "1" --> "0..*" Prompt : inverse mirror prompts
    Category "1" --> "0..*" Prompt : prompts
    Prompt "1" --> "1" Category : category_id (required FK)
    Folder "1" --> "0..*" Prompt : contents
    Prompt "1" --> "1" Folder : folder_id (required FK)
    Folder "0..1" --> "0..*" Folder : nullable parent_id (self FK)
```

### Cardinality and ownership details

- `BaseEntity` defines database ID (IDENTITY/AUTO_INCREMENT) and timestamps; it is not a user model.
- `Prompt.ownerId` is the Keycloak subject, a string, not an association to an application `User` entity.
- `Prompt.folder ↔ Folder.prompts` is the required **N:1 folder organization** relationship. `Prompt.category ↔ Category.prompts` is the required **N:1 classification** relationship and is global (not user-owned).
- `Prompt.tags ↔ Tag.prompts` is the N:M relation stored in `prompt_tags` with `(prompt_id, tag_id)` PK and both delete cascades. The entity only writes the Prompt side; the Tag side is a read-only mirror.
- `Folder.parent` is nullable self-reference. `children` is the inverse 1:N mapping. Trees are traversed and constrained by `FolderService`, not by cascade rules.
- `PromptStatus` is stored with `EnumType.STRING`, so enum reordering is safe for existing DB values.

## Repository UML by type

```mermaid
classDiagram
    class PanacheRepository {
        <<external>> PanacheRepository<E>
        find(...)
        findByIdOptional(id)
        persist(entity)
        delete(entity)
        count(...)
    }

    class PromptRepository {
        -FETCH: String
        +search(ownerId, q) List<Prompt>
        +findDetailed(id) Optional<Prompt>
        +countByCategoryId(id) long
        +countByTagId(id) long
        +countByFolderId(id) long
    }
    class CategoryRepository {
        +findByName(name) Optional<Category>
    }
    class FolderRepository {
        +findDefault(ownerId) Optional<Folder>
        +findByOwnerAndName(ownerId, name) Optional<Folder>
        +listByOwner(ownerId) List<Folder>
        +countChildren(parentId) long
    }
    class TagRepository {
        +findByName(ownerId, name) Optional<Tag>
        +findOrCreate(ownerId, name) Tag
        +search(ownerId, q) List<Tag>
    }

    PanacheRepository <|.. PromptRepository
    PanacheRepository <|.. CategoryRepository
    PanacheRepository <|.. FolderRepository
    PanacheRepository <|.. TagRepository
    PromptRepository ..> Prompt : reads and counts
    CategoryRepository ..> Category : find by name
    FolderRepository ..> Folder : owner/default/children
    TagRepository ..> Tag : normalize, find/create, search
```

### Repository notes

- Database access is type-safe and route-independent. `PanacheRepository` is an external Quarkus Panache type shown only to explain implemented contracts.
- `PromptRepository.search` dynamically adds `ownerId` and the optional q pattern via bound parameters. `findDetailed` uses FETCH loading of category/folder/tags.
- `CategoryRepository` and `FolderRepository` key lookups include owner or name. The default folder query specifically requires `name = "default" AND parent IS NULL`.
- Specialized find/create semantics for tags uses normalized name first (`trim().toLowerCase(Locale.ROOT)`), then per-owner lookup, not a process-global name.
