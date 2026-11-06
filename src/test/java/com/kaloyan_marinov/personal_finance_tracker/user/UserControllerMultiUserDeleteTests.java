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
class UserControllerMultiUserDeleteTests {

    @Autowired
    private RestTestClient restTestClient;

    /**
     * A request for deleting a user with correct Basic Auth credentials
     * should not be allowed to delete a user different from the authenticated one.
     */
    @Test
    void delete_1() {
        // Arrange.
        UserRequestCreate jdUserRequestCreate = new UserRequestCreate(
            "jd",
            "john.doe@protonmail.com",
            "123"
        );
        UserResponseCreate jdUserResponseCreate = Utilities.createUser(
            restTestClient,
            jdUserRequestCreate
        );

        UserRequestCreate msUserRequestCreate = new UserRequestCreate(
            "ms",
            "mary.smith@protonmail.com",
            "456"
        );
        UserResponseCreate msUserResponseCreate = Utilities.createUser(
            restTestClient,
            msUserRequestCreate
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/users/" + jdUserResponseCreate.id())
            .headers(
                h -> h.setBasicAuth(
                    msUserRequestCreate.email(),
                    msUserRequestCreate.password()
                )
            )
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.FORBIDDEN)
        ;

        responseSpec
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("error").isEqualTo(
                "not allowed to delete a user other than the authenticated one"
            )
        ;
    }
}
