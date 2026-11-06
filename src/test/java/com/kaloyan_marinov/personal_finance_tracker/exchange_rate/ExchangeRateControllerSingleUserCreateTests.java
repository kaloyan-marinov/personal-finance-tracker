package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
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
class ExchangeRateControllerSingleUserCreateTests {

    @Autowired
    private RestTestClient restTestClient;

    /**
     * POSTs a valid exchange rate whose factor uses all 8 decimal places
     * and reads it back by its natural key.
     */
    @Test
    void create_01() {
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
        ExchangeRateResponse exchangeRateResponse = responseSpec
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        String expectedBodyJSON =
            """
            {
                id: %d,
                year: 2001,
                sourceCurrency: "JPY",
                targetCurrency: "EUR",
                sourceToTargetFactor: 0.00571234
            }
            """.formatted(
                exchangeRateResponse.id()
            )
        ;

        restTestClient
            .get().uri("/api/v1/exchange-rates/2001/JPY/EUR")
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
     * POSTs a rate and then a second rate with the same natural key but a different factor.
     * Expects a 409 response for the second one, which mirrors {@code uq_exchange_rates_user_id_year_currencies}.
     */
    @Test
    void create_02() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2002,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestCreate)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        ExchangeRateRequestCreate duplicateExchRateReqCreate = new ExchangeRateRequestCreate(
            exchangeRateRequestCreate.year(),
            exchangeRateRequestCreate.sourceCurrency(),
            exchangeRateRequestCreate.targetCurrency(),
            new BigDecimal("1.1")
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(duplicateExchRateReqCreate)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                "error": "Exchange rate already exists"
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

    /**
     * POSTs a rate and the rate for the reverse direction.
     * Expects both to be created, because the reverse direction is a separate row.
     */
    @Test
    void create_03() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate1 = new ExchangeRateRequestCreate(
            2003,
            "ABC",
            "XYZ",
            new BigDecimal("4")
        );
        ExchangeRateResponse exchangeRateResponse = restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestCreate1)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        ExchangeRateRequestCreate exchangeRateRequestCreate2 = new ExchangeRateRequestCreate(
            exchangeRateRequestCreate1.year(),
            exchangeRateRequestCreate1.targetCurrency(),
            exchangeRateRequestCreate1.sourceCurrency(),
            new BigDecimal("0.25")
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestCreate2)
            .exchange()
        ;

        // Assert.
        ExchangeRateResponse observed = responseSpec
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        ExchangeRateResponse expected = new ExchangeRateResponse(
            observed.id(),
            exchangeRateRequestCreate2.year(),
            exchangeRateRequestCreate2.sourceCurrency(),
            exchangeRateRequestCreate2.targetCurrency(),
            exchangeRateRequestCreate2.sourceToTargetFactor()
        );

        Assertions.assertEquals(expected, observed);
    }

    /**
     * POSTs an otherwise valid rate without a {@code year}.
     * Expects a 400 response, which reports the missing field.
     */
    @Test
    void create_04() {
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

        Integer inadmissibleYear = null;
        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            inadmissibleYear,
            "EUR",
            "USD",
            new BigDecimal("1.085")
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
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Loops over currency codes which are not exactly 3 uppercase Latin letters.
     * For each one, POSTs an otherwise valid rate with that code as the {@code sourceCurrency}.
     * Expects a 400 response, which reports the violated pattern.
     */
    @Test
    void create_05() {
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
            String inadmissibleSourceCurrency : new String[] {
                "",
                "eur",
                "Eur",
                "EU",
                "EURO",
                "E1R"
            }
        ) {
            ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
                2005,
                inadmissibleSourceCurrency,
                "USD",
                new BigDecimal("1.085")
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
                    "error": "Validation failed",
                    "fieldErrors": {
                        "sourceCurrency": "must match \\"^[A-Z]{3}$\\""
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

    /**
     * Loops over currency codes which are not exactly 3 uppercase Latin letters.
     * For each one, POSTs an otherwise valid rate with that code as the {@code targetCurrency}.
     * Expects a 400 response, which reports the violated pattern.
     */
    @Test
    void create_06() {
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
            String inadmissibleTargetCurrency : new String[] {
                "",
                "eur",
                "Eur",
                "EU",
                "EURO",
                "E1R"
            }
        ) {
            ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
                2006,
                "USD",
                inadmissibleTargetCurrency,
                new BigDecimal("0.92")
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
                    "error": "Validation failed",
                    "fieldErrors": {
                        "targetCurrency": "must match \\"^[A-Z]{3}$\\""
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

    /**
     * POSTs an otherwise valid rate
     * whose {@code sourceCurrency} and {@code targetCurrency} are identical.
     * Expects a 400 response, which mirrors {@code chk_exchange_rates_distinct_currencies}.
     */
    @Test
    void create_07() {
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

        String sameCurrency = "EUR";

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2007,
            sameCurrency,
            sameCurrency,
            new BigDecimal("1")
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
                "error": "Validation failed",
                "fieldErrors": {
                    "distinctCurrencies": "sourceCurrency and targetCurrency must differ"
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
     * Attempts to create an exchange rate with
     * an admissible data type
     * but inadmissible values for {@code sourceToTargetFactor}.
     * Expects a 400 response, which mirrors {@code chk_exchange_rates_factor_positive}.
     */
    @Test
    void create_08() {
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
            BigDecimal inadmissibleSourceToTargetFactor : new BigDecimal[] {
                new BigDecimal("0"),
                new BigDecimal("-1.085")
            }
        ) {
            ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
                2008,
                "EUR",
                "USD",
                inadmissibleSourceToTargetFactor
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
                    "error": "Validation failed",
                    "fieldErrors": {
                        "sourceToTargetFactor": "must be greater than 0"
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

    /**
     * Attempt to create an exchange rate with
     * inadmissible values for {@code sourceToTargetFactor}:
     * positive, but violating {@code @Digits(integer = 10, fraction = 8)} -
     * one with 9 decimal places
     * and one with 11 integer digits.
     */
    @Test
    void create_09() {
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
            BigDecimal inadmissibleSourceToTargetFactor : new BigDecimal[] {
                new BigDecimal("0.000000001"),
                new BigDecimal("12345678901")
            }
        ) {
            ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
                2009,
                "EUR",
                "USD",
                inadmissibleSourceToTargetFactor
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
                    "error": "Validation failed",
                    "fieldErrors": {
                        "sourceToTargetFactor": "numeric value out of bounds (<10 digits>.<8 digits> expected)"
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

    /**
     * Creates a user, obtains an access token for that user, and then deletes the user.
     * POSTs an exchange rate with the (still unexpired) token.
     * Expects a 401 response.
     */
    @Test
    void create_10() {
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
}
