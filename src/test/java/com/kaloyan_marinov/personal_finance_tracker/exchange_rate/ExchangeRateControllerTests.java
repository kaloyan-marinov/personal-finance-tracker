package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

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
class ExchangeRateControllerTests {

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

    private ExchangeRateResponse createExchangeRate(
        String accessToken,
        ExchangeRateRequestCreate exchangeRateRequestCreate
    ) {
        return restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestCreate)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody();
    }

    /**
     * POSTs a valid exchange rate whose factor uses all 8 decimal places
     * and reads it back by its natural key.
     */
    @Test
    void create_01() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2001,
            "JPY",
            "EUR",
            new BigDecimal("0.00571234")
        );

        // Act.
        ExchangeRateResponse exchangeRateResponse = createExchangeRate(
            accessToken,
            exchangeRateRequestCreate
        );

        // Assert.
        String expectedBodyJSON = """
            {
                id: %d,
                year: 2001,
                sourceCurrency: "JPY",
                targetCurrency: "EUR",
                sourceToTargetFactor: 0.00571234
            }
        """.formatted(exchangeRateResponse.id());

        restTestClient
            .get().uri("/api/v1/exchange-rates/2001/JPY/EUR")
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
     * POSTs a rate and then a second rate with the same natural key but a different factor.
     * Expects a 409 response for the second one, which mirrors {@code uq_exchange_rates_user_id_year_currencies}.
     */
    @Test
    void create_02() {
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

        Integer year = 2002;
        String sourceCurrency = "EUR";
        String targetCurrency = "USD";

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.085")
        );
        createExchangeRate(accessToken, exchangeRateRequestCreate);

        
        // Act + Assert.
        ExchangeRateRequestCreate duplicateExchRateReqCreate = new ExchangeRateRequestCreate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.1")
        );

        String expectedBodyJSON = """
            {
                "error": "Exchange rate already exists"
            }
        """;

        restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(duplicateExchRateReqCreate)
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    /**
     * POSTs a rate and the rate for the reverse direction.
     * Expects both to be created, because the reverse direction is a separate row.
     */
    @Test
    void create_03() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate1 = new ExchangeRateRequestCreate(
            2003,
            "ABC",
            "XYZ",
            new BigDecimal("4")
        );
        ExchangeRateRequestCreate exchangeRateRequestCreate2 = new ExchangeRateRequestCreate(
            2003,
            "XYZ",
            "ABC",
            new BigDecimal("0.25")
        );

        // Act + Assert.
        createExchangeRate(accessToken, exchangeRateRequestCreate1);
        createExchangeRate(accessToken, exchangeRateRequestCreate2);
    }

    /**
     * POSTs an otherwise valid rate without a {@code year}.
     * Expects a 400 response, which reports the missing field.
     */
    @Test
    void create_04() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            null,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );

        // Act + Assert.
        String expectedBodyJSON = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "year": "must not be null"
                }
            }
        """;

        restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestCreate)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    /**
     * Loops over currency codes which are not exactly 3 uppercase Latin letters.
     * For each one, POSTs an otherwise valid rate with that code as the {@code sourceCurrency}.
     * Expects a 400 response, which reports the violated pattern.
     */
    @Test
    void create_05() {
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

        Integer year = 2005;
        String trgtCurrency = "USD";
        BigDecimal amount = new BigDecimal("1.085");

        for (String srcCurrency : new String[] {"", "eur", "Eur", "EU", "EURO", "E1R"}) {
            ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
                year,
                srcCurrency,
                trgtCurrency,
                amount
            );

            // Act + Assert.
            String expectedBodyJSON = """
                {
                    "error": "Validation failed",
                    "fieldErrors": {
                        "sourceCurrency": "must match \\"^[A-Z]{3}$\\""
                    }
                }
            """;

            restTestClient
                .post().uri("/api/v1/exchange-rates")
                .headers(
                    h -> h.setBearerAuth(accessToken)
                )
                .body(exchangeRateRequestCreate)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(
                    expectedBodyJSON,
                    JsonCompareMode.STRICT
                );
        }
    }

    /**
     * Loops over currency codes which are not exactly 3 uppercase Latin letters.
     * For each one, POSTs an otherwise valid rate with that code as the {@code targetCurrency}.
     * Expects a 400 response, which reports the violated pattern.
     */
    @Test
    void create_06() {
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

        Integer year = 2006;
        String srcCurrency = "USD";
        BigDecimal amount = new BigDecimal("0.92");

        for (String trgtCurrency : new String[] {"", "eur", "Eur", "EU", "EURO", "E1R"}) {
            // Arrange.
            ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
                year,
                srcCurrency,
                trgtCurrency,
                amount
            );

            // Act + Assert.
            String expectedBodyJSON = """
                {
                    "error": "Validation failed",
                    "fieldErrors": {
                        "targetCurrency": "must match \\"^[A-Z]{3}$\\""
                    }
                }
            """;

            restTestClient
                .post().uri("/api/v1/exchange-rates")
                .headers(
                    h -> h.setBearerAuth(accessToken)
                )
                .body(exchangeRateRequestCreate)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(
                    expectedBodyJSON,
                    JsonCompareMode.STRICT
                );
        }
    }

    /**
     * POSTs an otherwise valid rate
     * whose {@code sourceCurrency} and {@code targetCurrency} are identical.
     * Expects a 400 response, which mirrors {@code chk_exchange_rates_distinct_currencies}.
     */
    @Test
    void create_07() {
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

        String sameCurrency = "EUR";

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2007,
            sameCurrency,
            sameCurrency,
            new BigDecimal("1")
        );

        // Act + Assert.
        String expectedBodyJSON = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "distinctCurrencies": "sourceCurrency and targetCurrency must differ"
                }
            }
        """;

        restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestCreate)
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    /**
     * Attempts to create an exchange rate with
     * an admissible data type
     * but inadmissible values for {@code sourceToTargetFactor}.
     * Expects a 400 response, which mirrors {@code chk_exchange_rates_factor_positive}.
     */
    @Test
    void create_08() {
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

        Integer year = 2008;
        String srcCurrency = "EUR";
        String tgtCurrency = "USD";

        for (String srcToTrgtFactor : new String[] {"0", "-1.085"}) {
            ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
                year,
                srcCurrency,
                tgtCurrency,
                new BigDecimal(srcToTrgtFactor)
            );

            // Act + Assert.
            String expectedBodyJSON = """
                {
                    "error": "Validation failed",
                    "fieldErrors": {
                        "sourceToTargetFactor": "must be greater than 0"
                    }
                }
            """;

            restTestClient
                .post().uri("/api/v1/exchange-rates")
                .headers(
                    h -> h.setBearerAuth(accessToken)
                )
                .body(exchangeRateRequestCreate)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(
                    expectedBodyJSON,
                    JsonCompareMode.STRICT
                );
        }
    }

    /**
     * Attempt to create an exchange rate with
     * an admissible value for {@code sourceToTargetFactor}
     * but an inadmissible data type:
     * one with 9 decimal places
     * and one with 11 integer digits.
     */
    @Test
    void create_09() {
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
        
        Integer year = 2009;
        String srcCurrency = "EUR";
        String tgtCurrency = "USD";

        for (String srcToTrgtFactor : new String[] {"0.000000001", "12345678901"}) {
            ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
                year,
                srcCurrency,
                tgtCurrency,
                new BigDecimal(srcToTrgtFactor)
            );

            // Act + Assert.
            String expectedBodyJSON = """
                {
                    "error": "Validation failed",
                    "fieldErrors": {
                        "sourceToTargetFactor": "numeric value out of bounds (<10 digits>.<8 digits> expected)"
                    }
                }
            """;

            restTestClient
                .post().uri("/api/v1/exchange-rates")
                .headers(
                    h -> h.setBearerAuth(accessToken)
                )
                .body(exchangeRateRequestCreate)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody().json(
                    expectedBodyJSON,
                    JsonCompareMode.STRICT
                );
        }
    }

    /**
     * Creates a user, obtains an access token for that user, and then deletes the user.
     * POSTs an exchange rate with the (still unexpired) token.
     * Expects a 401 response.
     */
    @Test
    void create_10() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2001,
            "JPY",
            "EUR",
            new BigDecimal("0.00571234")
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestCreate)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                "error": "unauthorized to create an exchange rate"
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

    /**
     * GETs a rate which does not exist.
     * Expects a 404 response with an empty body.
     */
    @Test
    void retrieveOne_1() {
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

        // Act + Assert.
        restTestClient
            .get().uri("/api/v1/exchange-rates/2001/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();
    }

    /**
     * GETs a rate with a {@code year} which is not an integer.
     * Expects a 400 response, which reports the type mismatch.
     */
    @Test
    void retrieveOne_2() {
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

        // Act + Assert.
        String expectedBodyJSON = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "year": "must be of type Integer"
                }
            }
        """;

        restTestClient
            .get().uri("/api/v1/exchange-rates/abc/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    /**
     * Creates a rate
     * and then attempts to GET it with a lowercase {@code sourceCurrency}.
     * Expects a 400 response, which reports the violated pattern.
     */
    @Test
    void retrieveOne_3() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2003,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        createExchangeRate(accessToken, exchangeRateRequestCreate);

        // Act + Assert.
        String expectedBodyJSON = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "sourceCurrency": "must match \\"^[A-Z]{3}$\\""
                }
            }
        """;

        restTestClient
            .get().uri("/api/v1/exchange-rates/2003/eur/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );
    }

    /**
     * GETs all rates when none exist.
     * Expects a 200 response with an empty array.
     */
    @Test
    void retrieveAll_1() {
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

        // Act + Assert.
        restTestClient
            .get().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                "[]",
                JsonCompareMode.STRICT
            );
    }

    /**
     * Creates two rates and then GETs all rates.
     * Expects a 200 response with both rates, in the order in which they were created.
     */
    @Test
    void retrieveAll_2() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate1 = new ExchangeRateRequestCreate(
            2001,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse1 = createExchangeRate(
            accessToken,
            exchangeRateRequestCreate1
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate2 = new ExchangeRateRequestCreate(
            2002,
            "CHF",
            "EUR",
            new BigDecimal("1.06")
        );
        ExchangeRateResponse exchangeRateResponse2 = createExchangeRate(
            accessToken,
            exchangeRateRequestCreate2
        );

        // Act + Assert.
        String expectedBodyJSON = """
            [
                {
                    id: %d,
                    year: 2001,
                    sourceCurrency: "EUR",
                    targetCurrency: "USD",
                    sourceToTargetFactor: 1.085
                },
                {
                    id: %d,
                    year: 2002,
                    sourceCurrency: "CHF",
                    targetCurrency: "EUR",
                    sourceToTargetFactor: 1.06
                }
            ]
        """.formatted(
            exchangeRateResponse1.id(),
            exchangeRateResponse2.id()
        );

        restTestClient
            .get().uri("/api/v1/exchange-rates")
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
     * PUTs a new factor to an existing rate.
     * Expects a 200 response with the updated rate, and that a later GET returns the updated rate.
     */
    @Test
    void update_1() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2001,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = createExchangeRate(
            accessToken,
            exchangeRateRequestCreate
        );

        // Act + Assert.
        ExchangeRateRequestUpdate exchangeRateRequestUpdate = new ExchangeRateRequestUpdate(
            new BigDecimal("1.1")
        );

        String expectedBodyJSON = """
            {
                id: %d,
                year: 2001,
                sourceCurrency: "EUR",
                targetCurrency: "USD",
                sourceToTargetFactor: 1.1
            }
        """.formatted(exchangeRateResponse.id());

        restTestClient
            .put().uri("/api/v1/exchange-rates/2001/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestUpdate)
            .exchange()
            .expectStatus().isOk()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );

        restTestClient
            .get().uri("/api/v1/exchange-rates/2001/EUR/USD")
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
     * PUTs to a rate which does not exist.
     * Expects a 404, and that the rate has not been created.
     */
    @Test
    void update_2() {
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

        ExchangeRateRequestUpdate exchangeRateRequestUpdate = new ExchangeRateRequestUpdate(
            new BigDecimal("1.1")
        );

        // Act + Assert.
        restTestClient
            .put().uri("/api/v1/exchange-rates/2002/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestUpdate)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();

        restTestClient
            .get().uri("/api/v1/exchange-rates/2002/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();
    }

    /**
     * PUTs an invalid factor to an existing rate.
     * Expects a 400, and that the rate is unchanged.
     */
    @Test
    void update_3() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2003,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = createExchangeRate(
            accessToken,
            exchangeRateRequestCreate
        );

        ExchangeRateRequestUpdate updateRequest = new ExchangeRateRequestUpdate(
            new BigDecimal("0")
        );

        // Act + Assert.
        String expectedBodyJSON1 = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "sourceToTargetFactor": "must be greater than 0"
                }
            }
        """;

        restTestClient
            .put().uri("/api/v1/exchange-rates/2003/EUR/USD")
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
                year: 2003,
                sourceCurrency: "EUR",
                targetCurrency: "USD",
                sourceToTargetFactor: 1.085
            }
        """.formatted(exchangeRateResponse.id());

        restTestClient
            .get().uri("/api/v1/exchange-rates/2003/EUR/USD")
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

    /**
     * Creates a rate and then PUTs to it with a lowercase {@code targetCurrency}.
     * Expects a 400 response, which reports the violated pattern, and that the rate is unchanged.
     */
    @Test
    void update_4() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2004,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = createExchangeRate(
            accessToken,
            exchangeRateRequestCreate
        );

        ExchangeRateRequestUpdate updateRequest = new ExchangeRateRequestUpdate(
            new BigDecimal("1.1")
        );

        // Act + Assert.
        String expectedBodyJSON1 = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "targetCurrency": "must match \\"^[A-Z]{3}$\\""
                }
            }
        """;

        restTestClient
            .put().uri("/api/v1/exchange-rates/2004/EUR/usd")
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
                year: 2004,
                sourceCurrency: "EUR",
                targetCurrency: "USD",
                sourceToTargetFactor: 1.085
            }
        """.formatted(exchangeRateResponse.id());

        restTestClient
            .get().uri("/api/v1/exchange-rates/2004/EUR/USD")
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

    /**
     * DELETEs a rate which does not exist.
     * Expects a 404 response with an empty body.
     */
    @Test
    void delete_1() {
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

        // Act + Assert.
        restTestClient
            .delete().uri("/api/v1/exchange-rates/2001/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();
    }

    /**
     * DELETEs an existing rate.
     * Expects a 204 response, and that a later GET returns a 404.
     */
    @Test
    void delete_2() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2002,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        createExchangeRate(accessToken, exchangeRateRequestCreate);

        // Act + Assert.
        restTestClient
            .delete().uri("/api/v1/exchange-rates/2002/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isNoContent()
            .expectBody().isEmpty();

        restTestClient
            .get().uri("/api/v1/exchange-rates/2002/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isNotFound()
            .expectBody().isEmpty();
    }

    /**
     * Creates a rate and then DELETEs it with a lowercase {@code sourceCurrency}.
     * Expects a 400 response, which reports the violated pattern, and that the rate still exists.
     */
    @Test
    void delete_3() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2003,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        createExchangeRate(accessToken, exchangeRateRequestCreate);

        // Act + Assert.
        String expectedBodyJSON = """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "sourceCurrency": "must match \\"^[A-Z]{3}$\\""
                }
            }
        """;

        restTestClient
            .delete().uri("/api/v1/exchange-rates/2003/eur/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            );

        restTestClient
            .get().uri("/api/v1/exchange-rates/2003/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isOk();
    }

    /**
     * If a user owns any `ExchangeRate`s,
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2003,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = createExchangeRate(accessToken, exchangeRateRequestCreate);

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
                "error": "unable to delete your user, because you still have exchange rates"
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
