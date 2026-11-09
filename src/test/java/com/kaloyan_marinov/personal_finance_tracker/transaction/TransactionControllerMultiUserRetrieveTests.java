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
class TransactionControllerMultiUserRetrieveTests {

    @Autowired
    private RestTestClient restTestClient;

    /**
     * Given two distinct users (User 1 and User 2),
     * User 2 should not be able to retrieve
     * any transaction that refers to (= is owned by) User 1.
     */
    @Test
    void retrieveOne_1() {
        // Arrange.
        UserRequestCreate jdUserRequestCreate = new UserRequestCreate(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );
        UserResponseCreate jdUserResponseCreate = Utilities.createUser(
            restTestClient,
            jdUserRequestCreate
        );

        String jdAccessToken = Utilities.obtainAccessToken(
            restTestClient,
            jdUserRequestCreate.email(),
            jdUserRequestCreate.password()
        );

        UserRequestCreate msUserRequestCreate = new UserRequestCreate(
            "test-" + "ms",
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );
        UserResponseCreate msUserResponseCreate = Utilities.createUser(
            restTestClient,
            msUserRequestCreate
        );

        String msAccessToken = Utilities.obtainAccessToken(
            restTestClient,
            msUserRequestCreate.email(),
            msUserRequestCreate.password()
        );

        TransactionRequest jdTransactionRequest = new TransactionRequest(
            2006,
            new BigDecimal("-600.00"),
            "EUR",
            "groceries"
        );
        TransactionResponse jdTransactionResponse = Utilities.createTransaction(
            restTestClient,
            jdAccessToken,
            jdTransactionRequest
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/transactions/" + jdTransactionResponse.id())
            .headers(
                h -> h.setBearerAuth(msAccessToken)
            )
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody().isEmpty()
        ;
    }

    /**
     * Given two distinct users (User 1 and User 2),
     * each of whom has/owns >=1 transaction.
     *
     * When User 2 issues `GET /api/v1/transactions`,
     * the `resources` in the response body should:
     * (a) consists of the transactions owned by User 2;
     * (b) not contain any transaction owned by User 1.
     * Similarly, the `paginationInfo` in the response body should
     * summarize only the transactions owned by User 2.
     */
    @Test
    void retrieveAll_1() {
        // Arrange.
        UserRequestCreate jdUserRequestCreate = new UserRequestCreate(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );
        UserResponseCreate jdUserResponseCreate = Utilities.createUser(
            restTestClient,
            jdUserRequestCreate
        );

        String jdAccessToken = Utilities.obtainAccessToken(
            restTestClient,
            jdUserRequestCreate.email(),
            jdUserRequestCreate.password()
        );

        UserRequestCreate msUserRequestCreate = new UserRequestCreate(
            "test-" + "ms",
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );
        UserResponseCreate msUserResponseCreate = Utilities.createUser(
            restTestClient,
            msUserRequestCreate
        );

        String msAccessToken = Utilities.obtainAccessToken(
            restTestClient,
            msUserRequestCreate.email(),
            msUserRequestCreate.password()
        );

        TransactionRequest jdTransactionRequest1 = new TransactionRequest(
            2006,
            new BigDecimal("-600.00"),
            "EUR",
            "groceries"
        );
        TransactionResponse jdTransactionResponse1 = Utilities.createTransaction(
            restTestClient,
            jdAccessToken,
            jdTransactionRequest1
        );

        TransactionRequest msTransactionRequest1 = new TransactionRequest(
            2007,
            new BigDecimal("-700.00"),
            "EUR",
            "groceries"
        );
        TransactionResponse msTransactionResponse1 = Utilities.createTransaction(
            restTestClient,
            msAccessToken,
            msTransactionRequest1
        );

        TransactionRequest msTransactionRequest2 = new TransactionRequest(
            2009,
            new BigDecimal("-50.00"),
            "USD",
            "gym membership"
        );
        TransactionResponse msTransactionResponse2 = Utilities.createTransaction(
            restTestClient,
            msAccessToken,
            msTransactionRequest2
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(msAccessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                resources: [
                    {
                        "id": %d,
                        "year": 2007,
                        "amount": -700.00,
                        "currency": "EUR",
                        "description": "groceries"
                    },
                    {
                        "id": %d,
                        "year": 2009,
                        "amount": -50.00,
                        "currency": "USD",
                        "description": "gym membership"
                    }
                ],
                paginationInfo: {
                    total: 2,
                    pageNumber: 0,
                    pageSize: 10
                }
            }
            """.formatted(
                msTransactionResponse1.id(),
                msTransactionResponse2.id()
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
