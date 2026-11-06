package com.kaloyan_marinov.personal_finance_tracker.transaction;

import org.junit.jupiter.api.Assertions;

import java.math.BigDecimal;
import java.util.Map;

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
class TransactionControllerSingleUserCreateTests {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void create_1() {
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

        BigDecimal inadmissibleAmount = null;
        TransactionRequest transactionRequest = new TransactionRequest(
            2001,
            inadmissibleAmount,
            "EUR",
            "groceries"
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "amount": "must not be null"
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

    @Test
    void create_2() {
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

        BigDecimal inadmissibleAmount = new BigDecimal("-4.789");
        TransactionRequest transactionRequest = new TransactionRequest(
            2002,
            inadmissibleAmount,
            "EUR",
            "groceries"
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "amount": "numeric value out of bounds (<16 digits>.<2 digits> expected)"
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
     * Loops over {@code null}, {@code ""} and {@code " "}.
     * For each one, POSTs an otherwise valid transaction with that description.
     * Expects a 400 response with {@code "description": "must not be blank"},
     * using the same {@code STRICT} JSON check as elsewhere in the encompassing test-suite class.
     */
    @Test
    void create_3() {
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

        for (
            String inadmissibleDescription : new String[] {
                null,
                "",
                " "
            }
        ) {
            TransactionRequest transactionRequest = new TransactionRequest(
                2003,
                new BigDecimal("-300.00"),
                "EUR",
                inadmissibleDescription
            );

            // Act.
            RestTestClient.ResponseSpec responseSpec = restTestClient
                .post().uri("/api/v1/transactions")
                .headers(
                    h -> h.setBearerAuth(accessToken)
                )
                .body(transactionRequest)
                .exchange()
            ;

            // Assert.
            String expectedBodyJSON =
                """
                {
                    "error": "Validation failed",
                    "fieldErrors": {
                        "description": "must not be blank"
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
    }

    @Test
    void create_4() {
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

        String inadmissibleCurrency = null;
        TransactionRequest transactionRequest = new TransactionRequest(
            2004,
            new BigDecimal("-400.00"),
            inadmissibleCurrency,
            "groceries"
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "currency": "must not be null"
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
     * Loops over currency codes which are not exactly 3 uppercase Latin letters.
     * For each one, POSTs an otherwise valid transaction with that currency.
     * Expects a 400 response, which reports the violated pattern.
     */
    @Test
    void create_5() {
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

        for (
            String inadmissibleCurrency : new String[] {
                "",
                "eur",
                "Eur",
                "EU",
                "EURO",
                "E1R"
            }
        ) {
            TransactionRequest transactionRequest = new TransactionRequest(
                2005,
                new BigDecimal("-500.00"),
                inadmissibleCurrency,
                "groceries"
            );

            // Act.
            RestTestClient.ResponseSpec responseSpec = restTestClient
                .post().uri("/api/v1/transactions")
                .headers(
                    h -> h.setBearerAuth(accessToken)
                )
                .body(transactionRequest)
                .exchange()
            ;

            // Assert.
            String expectedBodyJSON =
                """
                {
                    "error": "Validation failed",
                    "fieldErrors": {
                        "currency": "must match \\"^[A-Z]{3}$\\""
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
    }

    @Test
    void create_6() {
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

        String inadmissibleDescription = "x".repeat(256);
        TransactionRequest transactionRequest = new TransactionRequest(
            2006,
            new BigDecimal("-600.00"),
            "EUR",
            inadmissibleDescription
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "description": "size must be between 0 and 255"
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
     * POSTs a transaction whose description has the maximum allowed length (255)
     * and reads it back,
     * so as to check that the database columns (`CHAR(3)` and `VARCHAR(255)`)
     * store the currency and the description without truncating or padding them.
     */
    @Test
    void create_7() {
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

        String description = "x".repeat(255);
        TransactionRequest transactionRequest = new TransactionRequest(
            2007,
            new BigDecimal("-700.00"),
            "CHF",
            description
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
        ;

        // Assert.
        TransactionResponse transactionResponse = responseSpec
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        Assertions.assertEquals(
            transactionRequest.year(),
            transactionResponse.year()
        );
        Assertions.assertEquals(
            transactionRequest.amount(),
            transactionResponse.amount()
        );
        Assertions.assertEquals(
            transactionRequest.currency(),
            transactionResponse.currency()
        );
        Assertions.assertEquals(
            transactionRequest.description(),
            transactionResponse.description()
        );

        long transactionId = transactionResponse.id();
        String expectedBodyJSON =
            """
            {
                id: %d,
                year: 2007,
                amount: -700.00,
                currency: "CHF",
                description: "%s"
            }
            """.formatted(
                transactionId,
                description
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

    /**
     * POSTs a transaction whose body carries the {@code id} of an existing transaction.
     * Expects the {@code id} to be ignored and a new transaction to be created,
     * i.e. a POST must never overwrite an existing transaction.
     */
    @Test
    void create_8() {
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
            2008,
            new BigDecimal("-800.00"),
            "EUR",
            "groceries"
        );

        TransactionResponse transactionResponse = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody()
        ;
        long transactionId = transactionResponse.id();

        // Since `TransactionRequest` has no `id`,
        // the body of the new request will be built as a `Map`.
        Map<String, Object> newTransactionRequestWithId = Map.of(
            "id", transactionId,
            "year", 2028,
            "amount", new BigDecimal("-8000.19"),
            "currency", "CHF",
            "description", "hotel stay"
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(newTransactionRequestWithId)
            .exchange()
        ;

        // Assert.
        TransactionResponse newTransactionResponse = responseSpec
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        long newTransactionId = newTransactionResponse.id();
        Assertions.assertNotEquals(transactionId, newTransactionId);

        String expectedBodyJSON =
            """
            {
                id: %d,
                year: %d,
                amount: %s,
                currency: "%s",
                description: "%s"
            }
            """.formatted(
                transactionId,
                transactionRequest.year(),
                transactionRequest.amount(),
                transactionRequest.currency(),
                transactionRequest.description()
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

    /**
     * Creates a user, obtains an access token for that user, and then deletes the user.
     * POSTs a transaction with the (still unexpired) token.
     * Expects a 401 response.
     */
    @Test
    void create_9() {
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

        restTestClient
            .delete().uri("/api/v1/users/" + userResponseCreate.id())
            .headers(
                h -> h.setBasicAuth(
                    userRequestCreate.email(),
                    userRequestCreate.password()
                )
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NO_CONTENT)
            .expectBody().isEmpty()
        ;

        TransactionRequest transactionRequest = new TransactionRequest(
            2008,
            new BigDecimal("-800.00"),
            "EUR",
            "groceries"
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                "error": "unauthorized to create a transaction"
            }
            """
        ;
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }
}
