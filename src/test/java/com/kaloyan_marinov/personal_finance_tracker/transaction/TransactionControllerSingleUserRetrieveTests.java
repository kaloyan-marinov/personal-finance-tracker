package com.kaloyan_marinov.personal_finance_tracker.transaction;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

import com.kaloyan_marinov.personal_finance_tracker.Utilities;
import com.kaloyan_marinov.personal_finance_tracker.user.UserRequestCreate;
import com.kaloyan_marinov.personal_finance_tracker.user.UserResponseCreate;

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
class TransactionControllerSingleUserRetrieveTests {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void retrieveOne_1() {
        // Arrange.
        UserRequestCreate userRequestCreate = new UserRequestCreate(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );
        UserResponseCreate userResponseCreate = Utilities.createUser(
            restTestClient,
            userRequestCreate
        );

        String accessToken = Utilities.obtainAccessToken(
            restTestClient,
            userRequestCreate.email(),
            userRequestCreate.password()
        );

        long nonexistentId = 17;

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/transactions/" + nonexistentId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody().isEmpty()
        ;
    }

    @Test
    void retrieveOne_2() {
        // Arrange.
        UserRequestCreate userRequestCreate = new UserRequestCreate(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );
        UserResponseCreate userResponseCreate = Utilities.createUser(
            restTestClient,
            userRequestCreate
        );

        String accessToken = Utilities.obtainAccessToken(
            restTestClient,
            userRequestCreate.email(),
            userRequestCreate.password()
        );

        TransactionRequest transactionRequest = new TransactionRequest(
            2002,
            new BigDecimal("-200.00"),
            "EUR",
            "groceries"
        );

        TransactionResponse transactionResponse = Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequest
        );
        long transactionId = transactionResponse.id();

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                id: %d,
                year: 2002,
                amount: -200,
                currency: "EUR",
                description: "groceries"
            }
            """.formatted(
                transactionId
            )
        ;

        responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }

    @Test
    void retrieveAll_1() {
        // Arrange.
        UserRequestCreate userRequestCreate = new UserRequestCreate(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );
        UserResponseCreate userResponseCreate = Utilities.createUser(
            restTestClient,
            userRequestCreate
        );

        String accessToken = Utilities.obtainAccessToken(
            restTestClient,
            userRequestCreate.email(),
            userRequestCreate.password()
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            []
            """
        ;

        responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }

    @Test
    void retrieveAll_2() {
        // Arrange.
        UserRequestCreate userRequestCreate = new UserRequestCreate(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );
        UserResponseCreate userResponseCreate = Utilities.createUser(
            restTestClient,
            userRequestCreate
        );

        String accessToken = Utilities.obtainAccessToken(
            restTestClient,
            userRequestCreate.email(),
            userRequestCreate.password()
        );

        TransactionRequest transactionRequest6 = new TransactionRequest(
            2006,
            new BigDecimal("-600.00"),
            "EUR",
            "groceries"
        );

        TransactionRequest transactionRequest7 = new TransactionRequest(
            2007,
            new BigDecimal("-700.00"),
            "EUR",
            "groceries"
        );

        TransactionResponse transactionResponse6 = Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequest6
        );

        TransactionResponse transactionResponse7 = Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequest7
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            [
                {
                    id: %d,
                    year: 2006,
                    amount: -600.00,
                    currency: "EUR",
                    description: "groceries"
                },
                {
                    id: %d,
                    year: 2007,
                    amount: -700,
                    currency: "EUR",
                    description: "groceries"
                }
            ]
            """.formatted(
                transactionResponse6.id(),
                transactionResponse7.id()
            )
        ;

        responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }
}
