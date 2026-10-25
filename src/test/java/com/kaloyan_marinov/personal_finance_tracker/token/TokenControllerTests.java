package com.kaloyan_marinov.personal_finance_tracker.token;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.junit.jupiter.api.Assertions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import com.kaloyan_marinov.personal_finance_tracker.user.UserRequestCreate;
import com.kaloyan_marinov.personal_finance_tracker.user.UserResponseCreate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.client.RestTestClient;

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
class TokenControllerTests {
    
    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void create_1() {
        // Act + Assert.
        restTestClient
            .post().uri("/api/v1/tokens")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().doesNotExist("Set-Cookie")
        ;
    }

    /*
    The last assertion within `create_1` cannot detect a session,
    because `MockMvc` has no «servlet container» to write `Set-Cookie: JSESSIONID=...`.

    So this test inspects the server-side request instead.
    */
    @Test
    void create_2() throws Exception {
        // Act.
        MvcResult mvcResult = mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/v1/tokens")
            )
            .andReturn()
        ;

        // Assert.
        Assertions.assertEquals(
            HttpStatus.UNAUTHORIZED.value(),
            mvcResult.getResponse().getStatus()
        );
        Assertions.assertNull(
            mvcResult.getRequest().getSession(false)
        );
    }

    private UserResponseCreate createUser(
        String username,
        String email,
        String password
    ) {
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

        return userResponseCreate;
    }

    @Test
    void create_3() {
        // Arrange.
        String username = "test-" + "jd";
        String email = "test-" + "john.doe@protonmail.com";
        String password = "test-" + "123";
        UserResponseCreate userResponseCreate = createUser(
            username,
            email,
            password
        );

        String wrongUsername = "wrong-" + email;
        String wrongPassword = "wrong-" + password;

        // Act + Assert.
        restTestClient
            .post().uri("/api/v1/tokens")
            .headers(
                h -> h.setBasicAuth(
                    wrongUsername,
                    wrongPassword
                )
            )
            .exchange()
            .expectStatus().isUnauthorized()
        ;
    }

    @Test
    void create_4() {
        // Arrange.
        String username = "test-" + "jd";
        String email = "test-" + "john.doe@protonmail.com";
        String password = "test-" + "123";
        UserResponseCreate userResponseCreate = createUser(
            username,
            email,
            password
        );

        String wrongPassword = "wrong-" + password;

        // Act + Assert.
        restTestClient
            .post().uri("/api/v1/tokens")
            .headers(
                h -> h.setBasicAuth(
                    email,
                    wrongPassword
                )
            )
            .exchange()
            .expectStatus().isUnauthorized()
        ;
    }

    @Test
    void create_5() {
        // Arrange.
        String username = "test-" + "jd";
        String email = "test-" + "john.doe@protonmail.com";
        String password = "test-" + "123";
        UserResponseCreate userResponseCreate = createUser(
            username,
            email,
            password
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/tokens")
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
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$.accessToken").value(
                v -> Assertions.assertInstanceOf(
                    String.class,
                    v
                )
            )
        ;
    }

    @Test
    void create_6() {
        // Arrange.
        String username = "test-" + "jd";
        String email = "test-" + "john.doe@protonmail.com";
        String password = "test-" + "123";
        UserResponseCreate userResponseCreate = createUser(
            username,
            email,
            password
        );

        Instant instantBefore = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // Act.
        TokenResponse tokenResponse = restTestClient
            .post().uri("/api/v1/tokens")
            .headers(
                h -> h.setBasicAuth(
                    email,
                    password
                )
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody(TokenResponse.class)
            .returnResult()
            .getResponseBody()
        ;
        String accessToken = tokenResponse.accessToken();

        Instant instantAfter = Instant.now();

        Jwt jwt = jwtDecoder.decode(accessToken);

        // Assert.
        Assertions.assertEquals(
            "HS256",
            jwt.getHeaders().get("alg")
        );
        Assertions.assertEquals(
            userResponseCreate.id(),
            (
                (Number) jwt.getClaim("userId")
            ).longValue()
        );
        Assertions.assertEquals(
            userResponseCreate.username(),
            jwt.getSubject()
        );

        Instant instantOfExpiration = jwt.getExpiresAt();

        Assertions.assertFalse(
            instantOfExpiration.isBefore(
                instantBefore.plus(Duration.ofHours(1))
            )
        );
        Assertions.assertFalse(
            instantOfExpiration.isAfter(
                instantAfter.plus(Duration.ofHours(1))
            )
        );
    }

    /*
    The payload of a JWT is only `base64`-encoded (not encrypted),
    so it must not contain the password of the application user.
    */
    @Test
    void create_7() {
        // Arrange.
        String username = "test-" + "jd";
        String email = "test-" + "john.doe@protonmail.com";
        String password = "test-" + "123";
        UserResponseCreate userResponseCreate = createUser(
            username,
            email,
            password
        );

        // Act.
        TokenResponse tokenResponse = restTestClient
            .post().uri("/api/v1/tokens")
            .headers(
                h -> h.setBasicAuth(
                    email,
                    password
                )
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody(TokenResponse.class)
            .returnResult()
            .getResponseBody()
        ;
        String accessToken = tokenResponse.accessToken();

        // Assert.
        String encodedPayload = accessToken.split("\\.")[1];
        String payload = new String(
            Base64.getUrlDecoder().decode(encodedPayload),
            StandardCharsets.UTF_8
        );

        Assertions.assertTrue(
            payload.contains(username)
        );
        Assertions.assertTrue(
            payload.contains(userResponseCreate.id().toString())
        );
        Assertions.assertFalse(
            payload.contains(email)
        );
        Assertions.assertFalse(
            payload.contains(password)
        );
    }

}
