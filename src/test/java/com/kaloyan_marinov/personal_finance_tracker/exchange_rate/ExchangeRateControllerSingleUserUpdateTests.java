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
class ExchangeRateControllerSingleUserUpdateTests {

    @Autowired
    private RestTestClient restTestClient;

    /**
     * PUTs a new factor to an existing rate.
     * Expects a 200 response with the updated rate, and that a later GET returns the updated rate.
     */
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2001,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate
        );

        ExchangeRateRequestUpdate exchangeRateRequestUpdate = new ExchangeRateRequestUpdate(
            new BigDecimal("1.1")
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .put().uri("/api/v1/exchange-rates/2001/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestUpdate)
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                id: %d,
                year: 2001,
                sourceCurrency: "EUR",
                targetCurrency: "USD",
                sourceToTargetFactor: 1.1
            }
            """.formatted(
                exchangeRateResponse.id()
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
            .get().uri("/api/v1/exchange-rates/2001/EUR/USD")
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
     * PUTs to a rate which does not exist.
     * Expects a 404, and that the rate has not been created.
     */
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

        ExchangeRateRequestUpdate exchangeRateRequestUpdate = new ExchangeRateRequestUpdate(
            new BigDecimal("1.1")
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .put().uri("/api/v1/exchange-rates/2002/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestUpdate)
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody().isEmpty()
        ;

        restTestClient
            .get().uri("/api/v1/exchange-rates/2002/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody().isEmpty()
        ;
    }

    /**
     * PUTs an invalid factor to an existing rate.
     * Expects a 400, and that the rate is unchanged.
     */
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

        BigDecimal inadmissibleSourceToTargetFactor = new BigDecimal("0");
        ExchangeRateRequestUpdate updateRequest = new ExchangeRateRequestUpdate(
            inadmissibleSourceToTargetFactor
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .put().uri("/api/v1/exchange-rates/2003/EUR/USD")
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
                    "sourceToTargetFactor": "must be greater than 0"
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
                year: 2003,
                sourceCurrency: "EUR",
                targetCurrency: "USD",
                sourceToTargetFactor: 1.085
            }
            """.formatted(
                exchangeRateResponse.id()
            )
        ;

        restTestClient
            .get().uri("/api/v1/exchange-rates/2003/EUR/USD")
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

    /**
     * Creates a rate and then PUTs to it with a lowercase {@code targetCurrency}.
     * Expects a 400 response, which reports the violated pattern, and that the rate is unchanged.
     */
    @Test
    void update_4() {
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
            2004,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate
        );

        ExchangeRateRequestUpdate updateRequest = new ExchangeRateRequestUpdate(
            new BigDecimal("1.1")
        );

        String inadmissibleTargetCurrency = "usd";

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .put().uri("/api/v1/exchange-rates/2004/EUR/" + inadmissibleTargetCurrency)
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
                    "targetCurrency": "must match \\"^[A-Z]{3}$\\""
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
                year: 2004,
                sourceCurrency: "EUR",
                targetCurrency: "USD",
                sourceToTargetFactor: 1.085
            }
            """.formatted(
                exchangeRateResponse.id()
            )
        ;

        restTestClient
            .get().uri("/api/v1/exchange-rates/2004/EUR/USD")
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
}
