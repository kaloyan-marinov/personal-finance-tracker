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
class ExchangeRateControllerSingleUserRetrieveTests {

    @Autowired
    private RestTestClient restTestClient;

    /**
     * GETs a rate which does not exist.
     * Expects a 404 response with an empty body.
     */
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

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates/2001/EUR/USD")
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

    /**
     * GETs a rate with a {@code year} which is not an integer.
     * Expects a 400 response, which reports the type mismatch.
     */
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

        String inadmissibleYear = "abc";

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates/" + inadmissibleYear + "/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "year": "must be of type Integer"
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
     * Creates a rate
     * and then attempts to GET it with a lowercase {@code sourceCurrency}.
     * Expects a 400 response, which reports the violated pattern.
     */
    @Test
    void retrieveOne_3() {
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
            2003,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate
        );

        String inadmissibleSourceCurrency = "eur";

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates/2003/" + inadmissibleSourceCurrency + "/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
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

    /**
     * This tests the happy path.
     */
    @Test
    void retrieveOne_4() {
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
            2003,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates/2003/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        ExchangeRateResponse observed = responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        ExchangeRateResponse expected = new ExchangeRateResponse(
            exchangeRateResponse.id(),
            exchangeRateRequestCreate.year(),
            exchangeRateRequestCreate.sourceCurrency(),
            exchangeRateRequestCreate.targetCurrency(),
            exchangeRateRequestCreate.sourceToTargetFactor()
        );
        Assertions.assertEquals(expected, observed);
    }

    /**
     * GETs all rates when none exist.
     * Expects a 200 response with an empty array.
     */
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
            .get().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                "[]",
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Creates two rates and then GETs all rates.
     * Expects a 200 response with both rates, in the order in which they were created.
     */
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

        ExchangeRateRequestCreate exchangeRateRequestCreate1 = new ExchangeRateRequestCreate(
            2001,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse1 = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate1
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate2 = new ExchangeRateRequestCreate(
            2002,
            "CHF",
            "EUR",
            new BigDecimal("1.06")
        );
        ExchangeRateResponse exchangeRateResponse2 = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate2
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates")
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
