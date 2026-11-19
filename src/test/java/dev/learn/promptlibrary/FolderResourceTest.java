package dev.learn.promptlibrary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import java.util.UUID;

import dev.learn.promptlibrary.category.Category;
import dev.learn.promptlibrary.category.CategoryRepository;
import dev.learn.promptlibrary.folder.Folder;
import dev.learn.promptlibrary.folder.FolderRepository;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

@QuarkusTest
class FolderResourceTest {

    @Inject
    FolderRepository folders;

    @Inject
    CategoryRepository categories;

    @Test
    void list_withoutToken_returns401() {
        given().when().get("/api/folders").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "bob")
    void list_createsOneDefaultFolder() {
        given().when().get("/api/folders")
                .then().statusCode(200)
                .body("name", hasItem("default"));

        given().when().get("/api/folders")
                .then().statusCode(200)
                .body("findAll { it.name == 'default' }.size()", equalTo(1))
                .body("find { it.name == 'default' }.ownerId", equalTo("bob"))
                .body("find { it.name == 'default' }.isDefault", equalTo(true));
    }

    @Test
    @TestSecurity(user = "bob")
    void create_rootAndNestedFolders() {
        String rootName = uniqueName("root");
        int rootId = given().contentType(ContentType.JSON)
                .body(folderBody(rootName, null))
                .when().post("/api/folders")
                .then().statusCode(201)
                .header("Location", containsString("/api/folders/"))
                .body("parentId", equalTo(null))
                .body("isDefault", equalTo(false))
                .extract().path("id");

        String childName = uniqueName("child");
        int childId = given().contentType(ContentType.JSON)
                .body(folderBody(childName, rootId))
                .when().post("/api/folders")
                .then().statusCode(201)
                .body("parentId", equalTo(rootId))
                .body("ownerId", equalTo("bob"))
                .extract().path("id");

        deleteFolder(childId);
        deleteFolder(rootId);
    }

    @Test
    @TestSecurity(user = "bob")
    void create_duplicateName_returns409() {
        String name = uniqueName("dup");
        int id = createFolder(name, null);

        given().contentType(ContentType.JSON)
                .body(folderBody(name, null))
                .when().post("/api/folders")
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));

        deleteFolder(id);
    }

    @Test
    @TestSecurity(user = "bob")
    void create_defaultName_returns409() {
        given().contentType(ContentType.JSON)
                .body(folderBody("default", null))
                .when().post("/api/folders")
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));
    }

    @Test
    @TestSecurity(user = "bob")
    void create_underOtherUsersFolder_returns404() {
        int otherFolder = createFolderFor("alice", uniqueName("foreign"));

        given().contentType(ContentType.JSON)
                .body(folderBody(uniqueName("child"), otherFolder))
                .when().post("/api/folders")
                .then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @TestSecurity(user = "bob")
    void get_otherUsersFolder_returns404() {
        int otherFolder = createFolderFor("alice", uniqueName("hidden"));

        given().when().get("/api/folders/" + otherFolder)
                .then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void admin_canSeeAndRenameAnotherUsersFolder() {
        String original = uniqueName("bob-folder");
        int id = createFolderFor("bob", original);
        String renamed = uniqueName("admin-rename");

        given().when().get("/api/folders")
                .then().statusCode(200)
                .body("name", hasItem(original));

        given().contentType(ContentType.JSON)
                .body("{\"name\":\"%s\"}".formatted(renamed))
                .when().put("/api/folders/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo(renamed))
                .body("ownerId", equalTo("bob"));

        deleteFolder(id);
    }

    @Test
    @TestSecurity(user = "bob")
    void rename_defaultFolder_returns409() {
        given().when().get("/api/folders").then().statusCode(200);
        int defaultId = defaultFolderId("bob");

        given().contentType(ContentType.JSON)
                .body("{\"name\":\"renamed-default\"}")
                .when().put("/api/folders/" + defaultId)
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));
    }

    @Test
    @TestSecurity(user = "bob")
    void rename_duplicateName_returns409() {
        String firstName = uniqueName("first");
        String secondName = uniqueName("second");
        int firstId = createFolder(firstName, null);
        int secondId = createFolder(secondName, null);

        given().contentType(ContentType.JSON)
                .body("{\"name\":\"%s\"}".formatted(firstName))
                .when().put("/api/folders/" + secondId)
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));

        deleteFolder(secondId);
        deleteFolder(firstId);
    }

    @Test
    @TestSecurity(user = "bob")
    void move_folderToRootAndRejectCycles() {
        int rootId = createFolder(uniqueName("root"), null);
        int childId = createFolder(uniqueName("child"), rootId);
        int grandChildId = createFolder(uniqueName("grandchild"), childId);

        given().contentType(ContentType.JSON)
                .body("{\"parentId\":%d}".formatted(rootId))
                .when().put("/api/folders/" + grandChildId + "/move")
                .then().statusCode(200)
                .body("parentId", equalTo(rootId));

        given().contentType(ContentType.JSON)
                .body("{\"parentId\":%d}".formatted(childId))
                .when().put("/api/folders/" + rootId + "/move")
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));

        given().contentType(ContentType.JSON)
                .body("{\"parentId\":null}")
                .when().put("/api/folders/" + childId + "/move")
                .then().statusCode(200)
                .body("parentId", equalTo(null));

        deleteFolder(grandChildId);
        deleteFolder(childId);
        deleteFolder(rootId);
    }

    @Test
    @TestSecurity(user = "bob")
    void delete_defaultFolder_returns409() {
        given().when().get("/api/folders").then().statusCode(200);
        int defaultId = defaultFolderId("bob");

        given().when().delete("/api/folders/" + defaultId)
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));
    }

    @Test
    @TestSecurity(user = "bob")
    void delete_folderWithPrompts_returns409() {
        int folderId = createFolder(uniqueName("with-prompt"), null);
        int categoryId = createCategory();

        int promptId = given().contentType(ContentType.JSON)
                .body("""
                        {"title":"Folder delete test","content":"x","categoryId":%d,"folderId":%d}
                        """.formatted(categoryId, folderId))
                .when().post("/api/prompts")
                .then().statusCode(201)
                .extract().path("id");

        given().when().delete("/api/folders/" + folderId)
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));

        given().when().delete("/api/prompts/" + promptId).then().statusCode(204);
        deleteFolder(folderId);
    }

    @Test
    @TestSecurity(user = "bob")
    void delete_folderWithChildren_returns409() {
        int parentId = createFolder(uniqueName("parent"), null);
        int childId = createFolder(uniqueName("child"), parentId);

        given().when().delete("/api/folders/" + parentId)
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));

        deleteFolder(childId);
        deleteFolder(parentId);
    }

    @Test
    @TestSecurity(user = "bob")
    void delete_emptyFolder_returns204() {
        int id = createFolder(uniqueName("empty"), null);

        given().when().delete("/api/folders/" + id).then().statusCode(204);
        given().when().delete("/api/folders/" + id).then().statusCode(404);
    }

    private int createFolder(String name, Integer parentId) {
        return given().contentType(ContentType.JSON)
                .body(folderBody(name, parentId))
                .when().post("/api/folders")
                .then().statusCode(201)
                .extract().path("id");
    }

    private int createFolderFor(String ownerId, String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Folder folder = new Folder();
            folder.setOwnerId(ownerId);
            folder.setName(name);
            folders.persist(folder);
            return folder.getId().intValue();
        });
    }

    private int createCategory() {
        return QuarkusTransaction.requiringNew().call(() -> {
            Category category = new Category();
            category.setName("cat-" + UUID.randomUUID());
            categories.persist(category);
            return category.getId().intValue();
        });
    }

    private int defaultFolderId(String ownerId) {
        return QuarkusTransaction.requiringNew().call(() ->
                folders.findDefault(ownerId).orElseThrow().getId().intValue());
    }

    private static void deleteFolder(int id) {
        given().when().delete("/api/folders/" + id).then().statusCode(204);
    }

    private static String folderBody(String name, Integer parentId) {
        return parentId == null
                ? "{\"name\":\"%s\"}".formatted(name)
                : "{\"name\":\"%s\",\"parentId\":%d}".formatted(name, parentId);
    }

    private static String uniqueName(String prefix) {
        String base = prefix.length() > 12 ? prefix.substring(0, 12) : prefix;
        return base + "-" + UUID.randomUUID();
    }
}
