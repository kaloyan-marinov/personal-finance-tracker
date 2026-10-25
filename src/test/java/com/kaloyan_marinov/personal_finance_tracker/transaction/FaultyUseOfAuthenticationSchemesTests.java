package com.kaloyan_marinov.personal_finance_tracker.transaction;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

import com.kaloyan_marinov.personal_finance_tracker.user.UserRequestCreate;

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
class FaultyUseOfAuthenticationSchemesTests {
    
    @Autowired
    private RestTestClient restTestClient;

    /**
     * Sending Basic Auth credentials to a GET endpoint protected with Bearer Auth
     * should be rejected with 401,
     * because the `securityFilterChainForBearerAuth` ignores the `Basic` scheme entirely.
     */
    @Test
    void retrieveAll_1() {
        // Arrange.
        String wrongUsername = "wrong-u";
        String wrongPassword = "wrong-p";

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/transactions")
            .headers(
                h -> h.setBasicAuth(
                    wrongUsername,
                    wrongPassword
                )
            )
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isUnauthorized()
            .expectHeader().valueEquals(
                "WWW-Authenticate",
                "Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""
            )
        ;
    }

    /**
     * Sending Basic Auth credentials to a GET endpoint protected with Bearer Auth
     * should be rejected with 401,
     * because the `securityFilterChainForBearerAuth` ignores the `Basic` scheme entirely.
     */
    @Test
    void retrieveAll_2() {
        // Arrange.
        UserRequestCreate userRequestCreate = new UserRequestCreate(
            "jd",
            "john.doe@protonmail.com",
            "123"
        );
        restTestClient
            .post().uri("/api/v1/users")
            .body(userRequestCreate)
            .exchange()
            .expectStatus().isCreated()
        ;

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/transactions")
            .headers(
                h -> h.setBasicAuth(
                    userRequestCreate.email(),
                    userRequestCreate.password()
                )
            )
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isUnauthorized()
            .expectHeader().valueEquals(
                "WWW-Authenticate",
                "Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""
            )
        ;
    }

    /**
     * Sending no credentials to a GET endpoint protected with Bearer Auth
     * should be rejected with 401.
     */
    @Test
    void retrieveAll_3() {
        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/transactions")
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isUnauthorized()
            .expectHeader().valueEquals(
                "WWW-Authenticate",
                "Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""
            )
        ;
    }

    /**
     * A `GET` is a safe method, so CSRF protection never checks it.
     * If someone turned CSRF protection (for this project's application) back on,
     * the preceding test case would still pass.
     * 
     * To close that gap,
     * the following test case issues a `POST` without credentials.
     */
    @Test
    void create_1() {
        // Arrange.
        int year = 2008;
        BigDecimal amount = new BigDecimal("-800.00");
        String currency = "CHF";
        String description = "groceries";
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );
        
        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/transactions")
            .body(transactionRequest)
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isUnauthorized()
            .expectHeader().valueEquals(
                "WWW-Authenticate",
                "Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""
            )
        ;
    }

    /**
     * Sending no credentials to a path that no endpoint serves
     * should be rejected with 401 (and not 404),
     * because the `securityFilterChainForBearerAuth` denies by default:
     * it requires a Bearer token for every request
     * not matched by the `securityFilterChainForBasicAuth`.
     *
     * A side benefit is that
     * it doesn't reveal which URLs exist to someone who isn't logged in.
     */
    @Test
    void retrieveNonexistent_1() {
        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/nonexistent")
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isUnauthorized()
            .expectHeader().valueEquals(
                "WWW-Authenticate",
                "Bearer resource_metadata=\"http://localhost/.well-known/oauth-protected-resource\""
            )
        ;
    }
}
