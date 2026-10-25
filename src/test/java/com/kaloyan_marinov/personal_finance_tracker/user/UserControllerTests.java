package com.kaloyan_marinov.personal_finance_tracker.user;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

import com.kaloyan_marinov.personal_finance_tracker.token.TokenResponse;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:test",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.mysql.sql",
        "pft.security.secret.key=secret-value-that-must-be-of-at-least-256-bits-aka-32-bytes"
    }
)
@AutoConfigureRestTestClient
/*
The following annotation is required,
because it creates a `MockMvc` bean that has the security filter chain,
and then the `RestTestClient` binds to the created `MockMvc` bean.
*/
@AutoConfigureMockMvc
@Transactional
public class UserControllerTests {
    
    @Autowired
    private RestTestClient restTestClient;

    /**
     * Attempting to create a user without providing a password
     * should be rejected.
     */
    @Test
    void create_1() {
        // Arrange.
        String username = "jd";
        String email = "john.doe@protonmail.com";

        Map<String, String> body = Map.of(
            "username", username,
            "email", email
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/users")
            .body(body)
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isBadRequest()
        ;

        String expectedJSONString = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "password": "must not be blank"
                }
            }    
            """
        ;
        responseSpec
            .expectBody().json(
                expectedJSONString,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Attempting to create a user without providing an email
     * should be rejected.
     */
    @Test
    void create_2() {
        // Arrange.
        String username = "jd";
        String password = "123";

        Map<String, String> body = Map.of(
            "username", username,
            "password", password
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/users")
            .body(body)
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isBadRequest()
        ;

        String expectedJSONString = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "email": "must not be blank"
                }
            }    
            """
        ;
        responseSpec
            .expectBody().json(
                expectedJSONString,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Attempting to create a user without providing a username
     * should be rejected.
     */
    @Test
    void create_3() {
        // Arrange.
        String email = "john.doe@protonmail.com";
        String password = "123";

        Map<String, String> body = Map.of(
            "email", email,
            "password", password
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/users")
            .body(body)
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isBadRequest()
        ;

        String expectedJSONString = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "username": "must not be blank"
                }
            }    
            """
        ;
        responseSpec
            .expectBody().json(
                expectedJSONString,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Attempting to create a user with a password that is over 72 bytes in UTF-8
     * should be rejected.
     */
    @Test
    void create_4() {
        // Arrange.
        String username = "test-" + "jd";
        String email = "test-" + "john.doe@protonmail.com";
        String password = "a".repeat(72 + 1);

        UserRequestCreate userRequestCreate = new UserRequestCreate(
            username,
            email,
            password
        );
    
        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/users")
            .body(userRequestCreate)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "passwordAtMost72Bytes": "password must be at most 72 bytes in UTF-8"
                }
            }
            """
        ;
        responseSpec
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * This test case exercised the happy path.
     */
    @Test
    void create_5() {
        // Arrange.
        String username = "jd";
        String email = "john.doe@protonmail.com";
        String password = "123";

        UserRequestCreate userRequestCreate = new UserRequestCreate(
            username,
            email,
            password
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/users")
            .body(userRequestCreate)
            .exchange()
        ;

        // Assert.
        UserResponseCreate userResponseCreate = responseSpec
            .expectStatus().isCreated()
            .expectBody(UserResponseCreate.class)
            .returnResult()
            .getResponseBody()
        ;

        long userId = userResponseCreate.id();
        String expectedBodyJSON = """
            {
                id: %d,
                username: %s,
                email: %s
            }
            """.formatted(
                userId,
                username,
                email
            )
        ;

        responseSpec
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;

        // The following statement is an alternative to the former statement
        // (and avoids the need for a JSON string.)
        responseSpec
            .expectBody()
            .jsonPath("$.id").isNumber()
            .jsonPath("$.username").isEqualTo(username)
            .jsonPath("$.email").isEqualTo(email)
            .jsonPath("$.password").doesNotExist()
        ;
    }

    /**
     * The `@Size(max = 72)` annotation on `UserRequestCreate.password`
     * counts characters counts characters, not bytes.
     * 
     * That enables the controller to accept a password that
     * is <= 72 characters long but > 72 bytes in UTF-8,
     * which `BCryptPasswordEncoder.encode` will reject,
     * causing the client to get a 500 response.
     */
    @Test
    void create_6() {
        // Arrange.
        String username = "jd";
        String email = "john.doe@protonmail.com";
        // The following value is <= 72 characters long (and thus passes validation),
        // but it is 144 bytes in UTF-8.
        String password = "é".repeat(72);

        UserRequestCreate userRequestCreate = new UserRequestCreate(
            username,
            email,
            password
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/users")
            .body(userRequestCreate)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "passwordAtMost72Bytes": "password must be at most 72 bytes in UTF-8"
                }
            }
            """
        ;
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Attempting to create a user with an email that is not well-formed
     * should be rejected.
     */
    @Test
    void create_7() {
        // Arrange.
        String username = "jd";
        String email = "x";
        String password = "123";

        UserRequestCreate userRequestCreate = new UserRequestCreate(
            username,
            email,
            password
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/users")
            .body(userRequestCreate)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "email": "must be a well-formed email address"
                }
            }
            """
        ;
        responseSpec
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Attempting to create a user with a malformed request body
     * should be rejected
     * without echoing the details of the parsing error.
     *
     * 
     * 
     * «Spring MVC»'s `DefaultHandlerExceptionResolver` turns
     * the ensuing `HttpMessageNotReadableException` into `response.sendError(400)`.
     *
     * `MockMvc` does not perform the ensuing ERROR dispatch to `/error`,
     * so the response body that this test observes is empty.
     *
     * A real running server does perform that ERROR dispatch,
     * so the client would get «Spring Boot»'s default error body (in JSON) instead;
     * only a test that is based on a running server
     * (e.g. `@SpringBootTest(webEnvironment = RANDOM_PORT)`)
     * could observe that.
     */
    @Test
    void create_8() {
        // Arrange.
        String malformedBody = """
            {
                "username": "jd",
                "email":
            """
        ;

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .body(malformedBody)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON = """
            {
                "error": "Malformed request body"
            }
            """
        ;
        responseSpec
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Attempts to delete a user with Bearer Auth
     * should be rejected.
     */
    @Test
    void delete_1() {
        // Arrange.
        String username = "jd";
        String email = "john.doe@protonmail.com";
        String password = "123";

        UserRequestCreate userRequestCreate = new UserRequestCreate(
            username,
            email,
            password
        );

        UserResponseCreate userResponseCreate = restTestClient
            .post().uri("/api/v1/users")
            .body(userRequestCreate)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(UserResponseCreate.class)
            .returnResult()
            .getResponseBody()
        ;

        TokenResponse tokenResponse = restTestClient
            .post().uri("/api/v1/tokens")
            .headers(
                h -> h.setBasicAuth(
                    userRequestCreate.email(),
                    userRequestCreate.password()
                )
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody(TokenResponse.class)
            .returnResult()
            .getResponseBody()
        ;
        String accessToken = tokenResponse.accessToken();

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/users/" + userResponseCreate.id())
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;
        
        // Assert.
        responseSpec
            .expectStatus().isUnauthorized()
            .expectHeader().valueEquals(
                "WWW-Authenticate",
                "Basic realm=\"Realm\", charset=\"UTF-8\""
            )
        ;
    }

    /**
     * A request for deleting a user with correct Basic Auth credentials
     * should not be allowed to delete a user different from the authenticated one.
     */
    @Test
    void delete_2() {
        // Arrange.
        UserRequestCreate userRequestCreate1 = new UserRequestCreate(
            "jd",
            "john.doe@protonmail.com",
            "123"
        );

        UserResponseCreate userResponseCreate1 = restTestClient
            .post().uri("/api/v1/users")
            .body(userRequestCreate1)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(UserResponseCreate.class)
            .returnResult()
            .getResponseBody()
        ;

        UserRequestCreate userRequestCreate2 = new UserRequestCreate(
            "ms",
            "mary.smith@protonmail.com",
            "456"
        );

        UserResponseCreate userResponseCreate2 = restTestClient
            .post().uri("/api/v1/users")
            .body(userRequestCreate2)
            .exchange()
            .expectBody(UserResponseCreate.class)
            .returnResult()
            .getResponseBody()
        ;

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/users/" + userResponseCreate1.id())
            .headers(
                h -> h.setBasicAuth(
                    userRequestCreate2.email(),
                    userRequestCreate2.password()
                )
            )
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isForbidden()
        ;

        responseSpec
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("error").isEqualTo(
                "not allowed to delete a user other than the authenticated one"
            )
        ;
    }

    /**
     * This test case exercised the happy path.
     */
    @Test
    void delete_3() {
        // Arrange.
        String username = "jd";
        String email = "john.doe@protonmail.com";
        String password = "123";

        UserRequestCreate userRequestCreate = new UserRequestCreate(
            username,
            email,
            password
        );

        UserResponseCreate userResponseCreate = restTestClient
            .post().uri("/api/v1/users")
            .body(userRequestCreate)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(UserResponseCreate.class)
            .returnResult()
            .getResponseBody()
        ;

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/users/" + userResponseCreate.id())
            .headers(
                h -> h.setBasicAuth(
                    email,
                    password
                )
            )
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isNoContent()
            .expectBody().isEmpty()
        ;
    }
}
