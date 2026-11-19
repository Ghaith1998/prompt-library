package dev.learn.promptlibrary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.Test;

@QuarkusTest
class CategoryResourceTest {

    @Test
    @TestSecurity(user = "bob")
    void update_asNormalUser_returns403() {
        given().contentType(ContentType.JSON)
                .body(categoryBody(uniqueName()))
                .when().put("/api/categories/1")
                .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "bob")
    void delete_asNormalUser_returns403() {
        given().when().delete("/api/categories/1").then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void update_unknownCategory_returns404() {
        given().contentType(ContentType.JSON)
                .body(categoryBody(uniqueName()))
                .when().put("/api/categories/999999999")
                .then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void delete_unknownCategory_returns404() {
        given().when().delete("/api/categories/999999999")
                .then().statusCode(404)
                .body("code", equalTo("NOT_FOUND"));
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void update_duplicateName_returns409() {
        String firstName = uniqueName();
        int firstId = createCategory(firstName);
        int secondId = createCategory(uniqueName());

        given().contentType(ContentType.JSON)
                .body(categoryBody(firstName))
                .when().put("/api/categories/" + secondId)
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));

        given().when().delete("/api/categories/" + secondId).then().statusCode(204);
        given().when().delete("/api/categories/" + firstId).then().statusCode(204);
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void update_validName_returns200AndPreservesId() {
        int id = createCategory(uniqueName());
        String updatedName = uniqueName();

        given().contentType(ContentType.JSON)
                .body(categoryBody(updatedName))
                .when().put("/api/categories/" + id)
                .then().statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo(updatedName));

        given().when().delete("/api/categories/" + id).then().statusCode(204);
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void delete_emptyCategory_returns204() {
        int id = createCategory(uniqueName());

        given().when().delete("/api/categories/" + id).then().statusCode(204);
        given().when().delete("/api/categories/" + id).then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void delete_categoryInUse_returns409() {
        int categoryId = createCategory(uniqueName());

        int promptId = given().contentType(ContentType.JSON)
                .body("""
                        {"title":"Category delete test","content":"x","categoryId":%d}
                        """.formatted(categoryId))
                .when().post("/api/prompts")
                .then().statusCode(201)
                .extract().path("id");

        given().when().delete("/api/categories/" + categoryId)
                .then().statusCode(409)
                .body("code", equalTo("CONFLICT"));

        given().when().delete("/api/prompts/" + promptId).then().statusCode(204);
        given().when().delete("/api/categories/" + categoryId).then().statusCode(204);
    }

    private int createCategory(String name) {
        return given().contentType(ContentType.JSON)
                .body(categoryBody(name))
                .when().post("/api/categories")
                .then().statusCode(201)
                .extract().path("id");
    }

    private static String categoryBody(String name) {
        return "{\"name\":\"%s\"}".formatted(name);
    }

    private static String uniqueName() {
        return "cat-" + UUID.randomUUID();
    }
}
