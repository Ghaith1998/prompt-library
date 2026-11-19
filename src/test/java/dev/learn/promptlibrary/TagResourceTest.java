package dev.learn.promptlibrary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

import java.util.UUID;

import dev.learn.promptlibrary.category.Category;
import dev.learn.promptlibrary.category.CategoryRepository;
import dev.learn.promptlibrary.tag.Tag;
import dev.learn.promptlibrary.tag.TagRepository;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

@QuarkusTest
class TagResourceTest {

    @Inject
    TagRepository tags;

    @Inject
    CategoryRepository categories;

    @Test
    void list_withoutToken_returns401() {
        given().when().get("/api/tags").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "bob")
    void list_asNormalUser_returnsOnlyOwnTags() {
        String ownName = uniqueName("own");
        String otherName = uniqueName("other");
        createTag(ownName);
        createTagFor("alice", otherName);

        given().when().get("/api/tags")
                .then().statusCode(200)
                .body("name", hasItem(ownName))
                .body("name", not(hasItem(otherName)));

        deleteTag(createTagId(ownName));
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void list_asAdmin_seesAllTags() {
        String aliceTag = uniqueName("alice");
        String bobTag = uniqueName("bob");
        createTagFor("alice", aliceTag);
        createTagFor("bob", bobTag);

        given().when().get("/api/tags")
                .then().statusCode(200)
                .body("name", hasItems(aliceTag, bobTag));
    }

    @Test
    @TestSecurity(user = "bob")
    void list_filtersByQ() {
        String prefix = "q-" + UUID.randomUUID();
        String first = prefix + "-one";
        String second = prefix + "-two";
        createTag(first);
        createTag(second);

        given().queryParam("q", prefix)
                .when().get("/api/tags")
                .then().statusCode(200)
                .body("name", hasItems(first, second));

        deleteTag(createTagId(first));
        deleteTag(createTagId(second));
    }

    @Test
    @TestSecurity(user = "bob")
    void create_normalizesNameAndReturnsLocation() {
        String rawName = "  Mix-" + UUID.randomUUID() + "  ";
        String expectedName = rawName.trim().toLowerCase();

        int id = given().contentType(ContentType.JSON)
                .body(tagBody(rawName))
                .when().post("/api/tags")
                .then().statusCode(201)
                .header("Location", containsString("/api/tags/"))
                .body("name", equalTo(expectedName))
                .body("ownerId", equalTo("bob"))
                .extract().path("id");

        deleteTag(id);
    }

    @Test
    @TestSecurity(user = "bob")
    void create_duplicateName_returns409() {
        String name = uniqueName("dup");
        int id = createTag(name);

        given().contentType(ContentType.JSON)
                .body(tagBody(name))
                .when().post("/api/tags")
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));

        deleteTag(id);
    }

    @Test
    @TestSecurity(user = "bob")
    void sameNameDifferentOwners_isAllowed() {
        String name = uniqueName("shared");
        createTagFor("alice", name);

        int id = given().contentType(ContentType.JSON)
                .body(tagBody(name))
                .when().post("/api/tags")
                .then().statusCode(201)
                .body("name", equalTo(name))
                .body("ownerId", equalTo("bob"))
                .extract().path("id");

        deleteTag(id);
    }

    @Test
    @TestSecurity(user = "bob")
    void get_otherUsersTag_returns404() {
        int id = createTagFor("alice", uniqueName("hidden"));

        given().when().get("/api/tags/" + id)
                .then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @TestSecurity(user = "bob")
    void update_ownTag_returns200() {
        int id = createTag(uniqueName("update"));
        String updatedName = uniqueName("updated");

        given().contentType(ContentType.JSON)
                .body(tagBody(updatedName))
                .when().put("/api/tags/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo(updatedName))
                .body("ownerId", equalTo("bob"));

        deleteTag(id);
    }

    @Test
    @TestSecurity(user = "bob")
    void update_duplicateName_returns409() {
        String firstName = uniqueName("first");
        String secondName = uniqueName("second");
        int firstId = createTag(firstName);
        int secondId = createTag(secondName);

        given().contentType(ContentType.JSON)
                .body(tagBody(firstName))
                .when().put("/api/tags/" + secondId)
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));

        deleteTag(firstId);
        deleteTag(secondId);
    }

    @Test
    @TestSecurity(user = "bob")
    void update_otherUsersTag_returns404() {
        int id = createTagFor("alice", uniqueName("foreign"));

        given().contentType(ContentType.JSON)
                .body(tagBody(uniqueName("renamed")))
                .when().put("/api/tags/" + id)
                .then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void admin_canUpdateOtherUsersTag() {
        int id = createTagFor("bob", uniqueName("bobtag"));
        String updatedName = uniqueName("admin-updated");

        given().contentType(ContentType.JSON)
                .body(tagBody(updatedName))
                .when().put("/api/tags/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo(updatedName))
                .body("ownerId", equalTo("bob"));

        deleteTag(id);
    }

    @Test
    @TestSecurity(user = "bob")
    void delete_otherUsersTag_returns404() {
        int id = createTagFor("alice", uniqueName("foreign-delete"));

        given().when().delete("/api/tags/" + id)
                .then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @TestSecurity(user = "bob")
    void delete_unusedTag_returns204() {
        int id = createTag(uniqueName("delete"));

        given().when().delete("/api/tags/" + id).then().statusCode(204);
        given().when().delete("/api/tags/" + id).then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "bob")
    void delete_tagInUse_returns409() {
        String tagName = uniqueName("in-use");
        int tagId = createTag(tagName);
        int categoryId = createCategory();

        int promptId = given().contentType(ContentType.JSON)
                .body("""
                        {"title":"Tag delete test","content":"x","categoryId":%d,"tags":["%s"]}
                        """.formatted(categoryId, tagName))
                .when().post("/api/prompts")
                .then().statusCode(201)
                .extract().path("id");

        given().when().delete("/api/tags/" + tagId)
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));

        given().when().delete("/api/prompts/" + promptId).then().statusCode(204);
        deleteTag(tagId);
    }

    private int createTag(String name) {
        return given().contentType(ContentType.JSON)
                .body(tagBody(name))
                .when().post("/api/tags")
                .then().statusCode(201)
                .extract().path("id");
    }

    private int createTagId(String name) {
        return tags.findByName("bob", name).orElseThrow().getId().intValue();
    }

    private int createTagFor(String ownerId, String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            Tag tag = new Tag();
            tag.setOwnerId(ownerId);
            tag.setName(name);
            tags.persist(tag);
            return tag.getId().intValue();
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

    private static void deleteTag(int id) {
        given().when().delete("/api/tags/" + id).then().statusCode(204);
    }

    private static String tagBody(String name) {
        return "{\"name\":\"%s\"}".formatted(name);
    }

    private static String uniqueName(String prefix) {
        String base = prefix.length() > 12 ? prefix.substring(0, 12) : prefix;
        return base + "-" + UUID.randomUUID();
    }
}
