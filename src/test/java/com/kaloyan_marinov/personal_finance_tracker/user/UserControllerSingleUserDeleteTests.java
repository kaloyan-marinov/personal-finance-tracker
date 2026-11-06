package com.kaloyan_marinov.personal_finance_tracker.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

import com.kaloyan_marinov.personal_finance_tracker.Utilities;

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
class UserControllerSingleUserDeleteTests {

    @Autowired
    private RestTestClient restTestClient;

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
        UserResponseCreate userResponseCreate = Utilities.createUser(
            restTestClient,
            userRequestCreate
        );

        String accessToken = Utilities.obtainAccessToken(
            restTestClient,
            email,
            password
        );

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
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
            .expectHeader().valueEquals(
                "WWW-Authenticate",
                "Basic realm=\"Realm\", charset=\"UTF-8\""
            )
        ;
    }

    /**
     * This test case exercised the happy path.
     */
    @Test
    void delete_2() {
        // Arrange.
        String username = "jd";
        String email = "john.doe@protonmail.com";
        String password = "123";

        UserRequestCreate userRequestCreate = new UserRequestCreate(
            username,
            email,
            password
        );
        UserResponseCreate userResponseCreate = Utilities.createUser(
            restTestClient,
            userRequestCreate
        );

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
            .expectStatus().isEqualTo(HttpStatus.NO_CONTENT)
            .expectBody().isEmpty()
        ;
    }
}
