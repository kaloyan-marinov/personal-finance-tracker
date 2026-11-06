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
class TransactionControllerSingleUserUpdateTests {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void update_1() {
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
            3000 + 2003,
            new BigDecimal(
                String.valueOf(-(3000 + 300.00))
            ),
            "EUR",
            "groceries"
        );

        TransactionResponse transactionResponse = Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequest
        );
        long transactionId = transactionResponse.id();

        Integer inadmissibleYear = null;
        TransactionRequest updateRequest = new TransactionRequest(
            inadmissibleYear,
            new BigDecimal("-300.00"),
            "EUR",
            "groceries"
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .put().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(updateRequest)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON1 =
            """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "year": "must not be null"
                }
            }
            """
        ;

        responseSpec
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody().json(
                expectedBodyJSON1,
                JsonCompareMode.STRICT
            )
        ;

        String expectedBodyJSON2 =
            """
            {
                id: %d,
                year: 5003,
                amount: -3300.00,
                currency: "EUR",
                description: "groceries"
            }
            """.formatted(
                transactionId
            )
        ;

        restTestClient
            .get().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBodyJSON2,
                JsonCompareMode.STRICT
            )
        ;
    }

    @Test
    void update_2() {
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
            2004,
            new BigDecimal("-400.00"),
            "EUR",
            "groceries"
        );

        long nonexistentId = 17;

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .put().uri("/api/v1/transactions/" + nonexistentId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody().isEmpty()
        ;
    }

    @Test
    void update_3() {
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
            3000 + 2005,
            new BigDecimal(
                String.valueOf(-(3000 + 500.00))
            ),
            "EUR",
            "groceries"
        );

        TransactionResponse transactionResponse = Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequest
        );
        long transactionId = transactionResponse.id();

        TransactionRequest updateTransactionRequest = new TransactionRequest(
            2005,
            new BigDecimal("-500.00"),
            "EUR",
            "groceries"
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .put().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(updateTransactionRequest)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                id: %d,
                year: 2005,
                amount: -500,
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

        restTestClient
            .get().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }
}
