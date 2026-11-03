package com.kaloyan_marinov.personal_finance_tracker.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

import com.kaloyan_marinov.personal_finance_tracker.token.TokenResponse;
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
class TransactionControllerTests {

    @Autowired 
    private RestTestClient restTestClient;

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

    private String obtainAccessToken(
        String email,
        String password
    ) {
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

        return tokenResponse.accessToken();
    }

    @Test
    void create_1() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );
        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year = 2001;
        BigDecimal amount = null;
        String currency = "EUR";
        String description = "groceries";
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );

        // Act + Assert.
        String expectedBodyJSON = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "amount": "must not be null"
                }
            }    
        """;

        restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    @Test
    void create_2() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year = 2002;
        BigDecimal amount = new BigDecimal("-4.789");
        String currency = "EUR";
        String description = "groceries";
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );

        // Act + Assert.
        String expectedBodyJSON = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "amount": "numeric value out of bounds (<16 digits>.<2 digits> expected)"
                }
            }
        """;

        restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
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
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );
        int year = 2003;
        BigDecimal amount = new BigDecimal("-300.00");
        String currency = "EUR";

        for (String description : new String[] {null, "", " "}) {
            TransactionRequest transactionRequest = new TransactionRequest(
                year,
                amount,
                currency,
                description
            );

            // Act + Assert.
            String expectedBodyJSON = """
                {
                    "error": "Validation failed",
                    "fieldErrors": {
                        "description": "must not be blank"
                    }
                }
            """;

            restTestClient
                .post().uri("/api/v1/transactions")
                .headers(
                    h -> h.setBearerAuth(accessToken)
                )
                .body(transactionRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(
                    expectedBodyJSON,
                    JsonCompareMode.STRICT
                );
        }
    }

    @Test
    void create_4() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year = 2004;
        BigDecimal amount = new BigDecimal("-400.00");
        String currency = null;
        String description = "groceries";
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );

        // Act + Assert.
        String expectedBodyJSON = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "currency": "must not be null"
                }
            }
        """;

        restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    /**
     * Loops over currency codes which are not exactly 3 uppercase Latin letters.
     * For each one, POSTs an otherwise valid transaction with that currency.
     * Expects a 400 response, which reports the violated pattern.
     */
    @Test
    void create_5() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year = 2005;
        BigDecimal amount = new BigDecimal("-500.00");
        String description = "groceries";
        
        for (String currency : new String[] {"", "eur", "Eur", "EU", "EURO", "E1R"}) {
            TransactionRequest transactionRequest = new TransactionRequest(
                year,
                amount,
                currency,
                description
            );

            // Act + Assert.
            String expectedBodyJSON = """
                {
                    "error": "Validation failed",
                    "fieldErrors": {
                        "currency": "must match \\"^[A-Z]{3}$\\""
                    }
                }
            """;

            restTestClient
                .post().uri("/api/v1/transactions")
                .headers(
                    h -> h.setBearerAuth(accessToken)
                )
                .body(transactionRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(
                    expectedBodyJSON,
                    JsonCompareMode.STRICT
                );
        }
    }

    @Test
    void create_6() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year = 2006;
        BigDecimal amount = new BigDecimal("-600.00");
        String currency = "EUR";
        String description = "x".repeat(256);
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );

        // Act + Assert.
        String expectedBodyJSON = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "description": "size must be between 0 and 255"
                }
            }
        """;

        restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
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
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year = 2007;
        BigDecimal amount = new BigDecimal("-700.00");
        String currency = "CHF";
        String description = "x".repeat(255);
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );

        // Act.
        TransactionResponse transactionResponse = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody();
            
        // Assert.
        assertEquals(
            transactionRequest.year(),
            transactionResponse.year()
        );
        assertEquals(
            transactionRequest.amount(),
            transactionResponse.amount()
        );
        assertEquals(
            transactionRequest.currency(),
            transactionResponse.currency()
        );
        assertEquals(
            transactionRequest.description(),
            transactionResponse.description()
        );
            
        long transactionId = transactionResponse.id();
        String expectedBodyJSON = """
            {
                id: %d,
                year: 2007,
                amount: -700.00,
                currency: "CHF",
                description: "%s"
            }
        """.formatted(transactionId, description);

        restTestClient
            .get().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    /**
     * POSTs a transaction whose body carries the {@code id} of an existing transaction.
     * Expects the {@code id} to be ignored and a new transaction to be created,
     * i.e. a POST must never overwrite an existing transaction.
     */
    @Test
    void create_8() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year = 2008;
        BigDecimal amount = new BigDecimal("-800.00");
        String currency = "EUR";
        String description = "groceries";
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );

        TransactionResponse transactionResponse = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody();
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
        TransactionResponse newTransactionResponse = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(newTransactionRequestWithId)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody();

        // Assert.
        long newTransactionId = newTransactionResponse.id();
        assertNotEquals(transactionId, newTransactionId);

        String expectedBodyJSON = """
            {
                id: %d,
                year: %d,
                amount: %s,
                currency: "%s",
                description: "%s"
            }
        """.formatted(
            transactionId,
            year,
            amount,
            currency,
            description
        );
        restTestClient
            .get().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    /**
     * Creates a user, obtains an access token for that user, and then deletes the user.
     * POSTs a transaction with the (still unexpired) token.
     * Expects a 401 response.
     */
    @Test
    void create_9() {
        // Arrange.
        String username = "test-" + "jd";
        String email = "test-" + "john.doe@protonmail.com";
        String password = "test-" + "123";

        UserResponseCreate userResponseCreate = createUser(
            username,
            email,
            password
        );

        String accessToken = obtainAccessToken(
            email,
            password
        );

        int year = 2008;
        BigDecimal amount = new BigDecimal("-800.00");
        String currency = "EUR";
        String description = "groceries";
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );

        restTestClient
            .delete().uri("/api/v1/users/" + userResponseCreate.id())
            .headers(
                h -> h.setBasicAuth(
                    email,
                    password
                )
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NO_CONTENT)
            .expectBody().isEmpty()
        ;

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

    @Test
    void retrieveOne_1() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        long nonexistentId = 17;

        // Act + Assert.
        restTestClient
            .get().uri("/api/v1/transactions/" + nonexistentId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();
    }

    @Test
    void retrieveOne_2() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year = 2002;
        BigDecimal amount = new BigDecimal("-200.00");
        String currency = "EUR";
        String description = "groceries";
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );

        TransactionResponse transactionResponse = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody();
        long transactionId = transactionResponse.id();

        // Act + Assert.
        String expectedBodyJSON = """
            {
                id: %d,
                year: 2002,
                amount: -200,
                currency: "EUR",
                description: "groceries"
            }
        """.formatted(transactionId);

        restTestClient
            .get().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    @Test
    void retrieveAll_1() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String expectedBodyJSON = """
            []
        """;

        // Act + Assert.
        restTestClient
            .get().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    @Test
    void retrieveAll_2() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year6 = 2006;
        BigDecimal amount6 = new BigDecimal("-600.00");
        String currency6 = "EUR";
        String description6 = "groceries";
        TransactionRequest transactionRequest6 = new TransactionRequest(
            year6,
            amount6,
            currency6,
            description6
        );

        int year7 = 2007;
        BigDecimal amount7 = new BigDecimal("-700.00");
        String currency7 = "EUR";
        String description7 = "groceries";
        TransactionRequest transactionRequest7 = new TransactionRequest(
            year7,
            amount7,
            currency7,
            description7
        );

        TransactionResponse transactionResponse6 = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest6)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody();
        
        TransactionResponse transactionResponse7 = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest7)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody();

        // Act + Assert.
        String expectedBodyJSON = """
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
        );

        restTestClient
            .get().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    @Test
    void update_1() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year = 3000 + 2003;
        BigDecimal amount = new BigDecimal(
            String.valueOf(-(3000 + 300.00))
        );
        String currency = "EUR";
        String description = "groceries";
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );

        TransactionResponse transactionResponse = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody();
        long transactionId = transactionResponse.id();

        Integer updatedYear = null;
        BigDecimal updatedAmount = new BigDecimal("-300.00");
        String updatedCurrency = "EUR";
        String updatedDescription = "groceries";
        TransactionRequest updateRequest = new TransactionRequest(
            updatedYear,
            updatedAmount,
            updatedCurrency,
            updatedDescription
        );

        // Act + Assert.
        String expectedBodyJSON1 = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "year": "must not be null"
                }
            }
        """;

        restTestClient
            .put().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(updateRequest)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON1,
                JsonCompareMode.STRICT
            );
        
        String expectedBodyJSON2 = """
            {
                id: %d,
                year: 5003,
                amount: -3300.00,
                currency: "EUR",
                description: "groceries"
            }
        """.formatted(transactionId);

        restTestClient
            .get().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                expectedBodyJSON2,
                JsonCompareMode.STRICT
            );
    }

    @Test
    void update_2() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year = 2004;
        BigDecimal amount = new BigDecimal("-400.00");
        String currency = "EUR";
        String description = "groceries";
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );

        long nonexistentId = 17;

        // Arrange + Act.
        restTestClient
            .put().uri("/api/v1/transactions/" + nonexistentId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();
    }

    @Test
    void update_3() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year = 3000 + 2005;
        BigDecimal amount = new BigDecimal(
            String.valueOf(-(3000 + 500.00))
        );
        String currency = "EUR";
        String description = "groceries";
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );

        TransactionResponse transactionResponse = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody();
        long transactionId = transactionResponse.id();

        int updatedYear = 2005;
        BigDecimal updatedAmount = new BigDecimal("-500.00");
        String updatedCurrency = "EUR";
        String updatedDescription = "groceries";
        TransactionRequest updateTransactionRequest = new TransactionRequest(
            updatedYear,
            updatedAmount,
            updatedCurrency,
            updatedDescription
        );

        // Act + Assert.
        String expectedBodyJSON = """
            {
                id: %d,
                year: 2005,
                amount: -500,
                currency: "EUR",
                description: "groceries"
            }
        """.formatted(transactionId);

        restTestClient
            .put().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(updateTransactionRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
        
        restTestClient
            .get().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    @Test
    void delete_1() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        long nonexistentId = 17;

        // Act + Assert.
        restTestClient
            .delete().uri("/api/v1/transactions/" + nonexistentId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();
    }

    @Test
    void delete_2() {
        // Arrange.
        UserResponseCreate userResponseCreate = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessToken = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        int year = 2006;
        BigDecimal amount = new BigDecimal("-600.00");
        String currency = "EUR";
        String description = "groceries";
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            amount,
            currency,
            description
        );

        TransactionResponse transactionResponse = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody();
        long transactionId = transactionResponse.id();

        // Act + Assert.
        restTestClient
            .delete().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().isEmpty();
        
        restTestClient
            .get().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();
    }

    /*
    Q:
    Why does this test case fail with
    `Status expected:<409 CONFLICT> but was:<204 NO_CONTENT>`
    instead of a 500
    (given that `fk_transactions_user_id` should reject the `DELETE`)?

    A:
    Because the `DELETE` statement never reaches the database.

    1. This test class is annotated with `@Transactional`,
       and `RestTestClient` is bound to `MockMvc`,
       which runs the controller on the test's own thread.
       So `UserController.delete` joins the test's transaction
       instead of getting its own.

    2. `userRepository.delete(...)` calls `EntityManager.remove(...)`,
       which only marks the entity as removed;
       Hibernate saves the `DELETE` for the next flush.

       (
       The `INSERT`s, on the other hand, are executed immediately,
       because `GenerationType.IDENTITY` requires Hibernate
       to execute each `INSERT` in order to obtain the generated id.
       )

    3. Nothing flushes after the removal:
       the repository's `@Transactional` merely joins the existing transaction
       (so it doesn't commit),
       and no later query touches `users`
       (so auto-flush never runs).
       The controller returns 204.

    4. When the test method ends, Spring rolls back the transaction,
       which discards the pending `DELETE` without ever executing it.
       So the foreign key is never checked.

    In production, there is no surrounding transaction:
    the repository's own transaction commits,
    the flush executes the `DELETE`,
    MySQL rejects it,
    and (because nothing catches that exception) the client gets a 500.

    Possible fixes:
    - (recommended)
      before deleting, check for owned rows
      (e.g. `transactionRepository.existsByUser(user)`, and similarly for exchange rates)
      and return 409 if any exist;
      this doesn't depend on when Hibernate flushes;
    - call `userRepository.flush()` right after `delete`,
      inside a `try` that catches `DataIntegrityViolationException` and returns 409;
    - remove `@Transactional` from this test class
      (which requires cleaning up data between tests manually).
    */
    /**
     * If a user owns any `Transaction`s,
     * an attempt to delete that user should be refused with a 409.
     */
    @Test
    void delete_user() {
        // Arrange.
        String username = "jd";
        String email = "john.doe@protonmail.com";
        String password = "123";

        UserResponseCreate userResponseCreate = createUser(
            username,
            email,
            password
        );

        String accessToken = obtainAccessToken(
            email,
            password
        );

        TransactionRequest transactionRequest1 = new TransactionRequest(
            2006,
            new BigDecimal("-600.00"),
            "EUR",
            "groceries"
        );
        TransactionResponse transactionResponse1 = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest1)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody(TransactionResponse.class)
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
        String expectedBodyJSON = 
            """
            {
                "error": "unable to delete your user, because you still have transactions"
            }
            """
        ;
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }
}
