package dev.learn.promptlibrary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.contains;

import java.util.UUID;

import dev.learn.promptlibrary.category.Category;
import dev.learn.promptlibrary.category.CategoryRepository;
import dev.learn.promptlibrary.folder.Folder;
import dev.learn.promptlibrary.folder.FolderService;
import dev.learn.promptlibrary.prompt.Prompt;
import dev.learn.promptlibrary.prompt.PromptRepository;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

/**
 * Runs against a throw-away MySQL (+ Keycloak) started by Quarkus Dev Services -> Docker must be running.
 * @TestSecurity fakes the logged-in user, so no real token is needed.
 */
@QuarkusTest
class PromptResourceTest {

    @Inject
    CategoryRepository categories;

    @Inject
    PromptRepository prompts;

    @Inject
    FolderService folders;

    @Test
    void list_withoutToken_returns401() {
        given().when().get("/api/prompts").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice")
    void list_asAuthenticatedUser_returns200() {
        given().when().get("/api/prompts").then().statusCode(200).contentType(ContentType.JSON);
    }

    @Test
    @TestSecurity(user = "alice")
    void create_withBlankTitle_returns400() {
        given().contentType(ContentType.JSON)
                .body("{\"title\":\"\",\"content\":\"x\",\"categoryId\":1}")
                .when().post("/api/prompts")
                .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "bob")
    void createCategory_asNormalUser_returns403() {
        given().contentType(ContentType.JSON)
                .body("{\"name\":\"nope\"}")
                .when().post("/api/categories")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void fullFlow_createReadUpdateDelete() {
        String categoryName = "cat-" + UUID.randomUUID();

        int categoryId = given().contentType(ContentType.JSON)
                .body("{\"name\":\"%s\"}".formatted(categoryName))
                .when().post("/api/categories")
                .then().statusCode(201)
                .body("name", equalTo(categoryName))
                .extract().path("id");

        int promptId = given().contentType(ContentType.JSON)
                .body("""
                        {"title":"Review helper","content":"Review this code","categoryId":%d,"tags":["Java","review"]}
                        """.formatted(categoryId))
                .when().post("/api/prompts")
                .then().statusCode(201)
                .header("Location", containsString("/api/prompts/"))
                .body("status", equalTo("DRAFT"))
                .body("tags", contains("java", "review"))   // normalized to lowercase + sorted
                .body("ownerId", notNullValue())
                .extract().path("id");

        given().when().get("/api/prompts/" + promptId)
                .then().statusCode(200)
                .body("title", equalTo("Review helper"))
                .body("categoryName", equalTo(categoryName));

        given().contentType(ContentType.JSON)
                .body("""
                        {"title":"Review helper v2","content":"Review this code","categoryId":%d,"status":"PUBLISHED","favorite":true,"tags":["java"]}
                        """.formatted(categoryId))
                .when().put("/api/prompts/" + promptId)
                .then().statusCode(200)
                .body("status", equalTo("PUBLISHED"))
                .body("favorite", equalTo(true))
                .body("tags", contains("java"));

        given().when().delete("/api/prompts/" + promptId).then().statusCode(204);
        given().when().get("/api/prompts/" + promptId).then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void create_withoutFolder_usesDefaultFolder() {
        int categoryId = createCategoryViaApi();

        given().contentType(ContentType.JSON)
                .body("""
                        {"title":"Default folder test","content":"x","categoryId":%d}
                        """.formatted(categoryId))
                .when().post("/api/prompts")
                .then().statusCode(201)
                .body("folderId", notNullValue())
                .body("folderName", equalTo("default"));
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void create_withFolder_andMoveToAnotherFolder() {
        int categoryId = createCategoryViaApi();
        int firstFolder = createFolderViaApi();
        int secondFolder = createFolderViaApi();

        int promptId = given().contentType(ContentType.JSON)
                .body("""
                        {"title":"Folder test","content":"x","categoryId":%d,"folderId":%d}
                        """.formatted(categoryId, firstFolder))
                .when().post("/api/prompts")
                .then().statusCode(201)
                .body("folderId", equalTo(firstFolder))
                .extract().path("id");

        given().contentType(ContentType.JSON)
                .body("""
                        {"title":"Folder test updated","content":"x","categoryId":%d}
                        """.formatted(categoryId))
                .when().put("/api/prompts/" + promptId)
                .then().statusCode(200)
                .body("folderId", equalTo(firstFolder));

        given().contentType(ContentType.JSON)
                .body("{\"folderId\":%d}".formatted(secondFolder))
                .when().put("/api/prompts/" + promptId + "/move")
                .then().statusCode(200)
                .body("folderId", equalTo(secondFolder));

        given().when().delete("/api/prompts/" + promptId).then().statusCode(204);
        given().when().delete("/api/folders/" + firstFolder).then().statusCode(204);
        given().when().delete("/api/folders/" + secondFolder).then().statusCode(204);
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void create_withFolderOfAnotherOwner_returns404() {
        int categoryId = createCategoryViaApi();
        int bobDefaultFolder = folders.ensureDefaultFolder("bob").getId().intValue();

        given().contentType(ContentType.JSON)
                .body("""
                        {"title":"Cross owner folder","content":"x","categoryId":%d,"folderId":%d}
                        """.formatted(categoryId, bobDefaultFolder))
                .when().post("/api/prompts")
                .then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @TestSecurity(user = "bob")
    void user_cannotSeeSomeoneElsesPrompt() {
        long id = createPromptOwnedBy("someone-else");
        given().when().get("/api/prompts/" + id).then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "carol", roles = "admin")
    void admin_canSeeAnyonesPrompt() {
        long id = createPromptOwnedBy("someone-else");
        given().when().get("/api/prompts/" + id).then().statusCode(200);
    }

    private int createCategoryViaApi() {
        return given().contentType(ContentType.JSON)
                .body("{\"name\":\"%s\"}".formatted(uniqueName("cat")))
                .when().post("/api/categories")
                .then().statusCode(201)
                .extract().path("id");
    }

    private int createFolderViaApi() {
        return given().contentType(ContentType.JSON)
                .body("{\"name\":\"%s\"}".formatted(uniqueName("folder")))
                .when().post("/api/folders")
                .then().statusCode(201)
                .extract().path("id");
    }

    private static String uniqueName(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    /** Test data goes straight through the repositories, in its own transaction. */
    private long createPromptOwnedBy(String ownerId) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Category category = new Category();
            category.setName("cat-" + UUID.randomUUID());
            categories.persist(category);

            Folder folder = folders.ensureDefaultFolder(ownerId);

            Prompt prompt = new Prompt();
            prompt.setTitle("Secret");
            prompt.setContent("x");
            prompt.setOwnerId(ownerId);
            prompt.setCategory(category);
            prompt.setFolder(folder);
            prompts.persist(prompt);
            return prompt.getId();
        });
    }
}
