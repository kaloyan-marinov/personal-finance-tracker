package com.kaloyan_marinov.personal_finance_tracker.transaction;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:test",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.mysql.sql",
        "pft.security.user.name=application-user-for-test-suite-u",
        "pft.security.user.password=application-user-for-test-suite-p"
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
class PublicEndpointTests {
    
    @Autowired
    private RestTestClient restTestClient;

    /**
     * If a client sends wrong Basic credentials to a `permitAll()` endpoint,
     * `BasicAuthenticationFilter` still rejects it with 401.
     * 
     * That's probably acceptable, but it's worth demonstrating via this test case.
     */
    @Test
    void retrieveAll_1() {
        // Arrange.
        String wrongUsername = "wrong-u";
        String wrongPassword = "wrong-p";

        // Act + Assert.
        restTestClient
            .get().uri("/api/v1/transactions")
            .headers(
                h -> h.setBasicAuth(
                    wrongUsername,
                    wrongPassword
                )
            )
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().valueEquals(
                "WWW-Authenticate",
                "Basic realm=\"Realm\", charset=\"UTF-8\""
            )
        ;
    }

    /**
     * If a client doesn't send Basic credentials to a `permitAll()` endpoint,
     * `BasicAuthenticationFilter` doesn't attempt authentication,
     * and the request goes through as anonymous.
     */
    @Test
    void retrieveAll_2() {
        // Act + Assert.
        restTestClient
            .get().uri("/api/v1/transactions")
            .exchange()
            .expectStatus().isOk()
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
        
        // Act + Assert.
        restTestClient
            .post().uri("/api/v1/transactions")
            .body(transactionRequest)
            .exchange()
            .expectStatus().isCreated()
        ;
    }
}
