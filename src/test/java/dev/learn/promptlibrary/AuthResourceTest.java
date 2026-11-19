package dev.learn.promptlibrary;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.http.ContentType;

import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthResourceTest {

    // These two never reach Keycloak: bean validation rejects the payload first.

    @Test
    void register_withInvalidPayload_returns400() {
        given().contentType(ContentType.JSON)
                .body("{\"username\":\"\",\"email\":\"not-an-email\",\"password\":\"short\"}")
                .when().post("/api/auth/register")
                .then().statusCode(400);
    }

    @Test
    void login_withMissingPassword_returns400() {
        given().contentType(ContentType.JSON)
                .body("{\"username\":\"alice\"}")
                .when().post("/api/auth/login")
                .then().statusCode(400);
    }

    @Test
    void me_withoutToken_returns401() {
        given().when().get("/api/me").then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "alice", roles = "admin")
    void me_returnsRolesOfTheCaller() {
        given().when().get("/api/me")
                .then().statusCode(200)
                .body("roles", hasItem("admin"));
    }
}
