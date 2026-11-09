package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import org.junit.jupiter.api.Assertions;

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
class ExchangeRateControllerMultiUserRetrieveTests {

    @Autowired
    private RestTestClient restTestClient;

    /**
     * Given two distinct users (User 1 and User 2),
     * User 2 should not be able to retrieve
     * any exchange rate that refers to (= is owned by) User 1.
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

        ExchangeRateRequestCreate jdExchangeRateRequest = new ExchangeRateRequestCreate(
            2003,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );

        ExchangeRateResponse jdExchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            jdAccessToken,
            jdExchangeRateRequest
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates/" + jdExchangeRateResponse.year() + "/" + jdExchangeRateResponse.sourceCurrency() + "/" + jdExchangeRateResponse.targetCurrency())
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
     * each of whom owns an exchange rate
     * for the same year and currency pair (but with different factors).
     *
     * When User 1 retrieves the exchange rate for that year and currency pair,
     * the response should contain User 1's exchange rate (and not User 2's).
     */
    @Test
    void retrieveOne_2() {
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

        Integer year = 2003;
        String sourceCurrency = "EUR";
        String targetCurrency = "USD";

        ExchangeRateRequestCreate jdExchangeRateRequest = new ExchangeRateRequestCreate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.100")
        );
        ExchangeRateResponse jdExchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            jdAccessToken,
            jdExchangeRateRequest
        );

        ExchangeRateRequestCreate msExchangeRateRequest = new ExchangeRateRequestCreate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.200")
        );
        ExchangeRateResponse msExchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            msAccessToken,
            msExchangeRateRequest
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates/" + year + "/" + sourceCurrency + "/" + targetCurrency)
            .headers(
                h -> h.setBearerAuth(jdAccessToken)
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
            jdExchangeRateResponse.id(),
            year,
            sourceCurrency,
            targetCurrency,
            jdExchangeRateRequest.sourceToTargetFactor()
        );
        Assertions.assertEquals(expected, observed);
    }

    /**
     * Given two distinct users (User 1 and User 2),
     * each of whom has/owns >=1 exchange rate.
     *
     * When User 2 issues `GET /api/v1/exchange-rates`,
     * the response body should:
     * (a) consist of the exchange rates owned by User 2;
     * (b) not contain any exchange rate owned by User 1.
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

        ExchangeRateRequestCreate jdExchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2001,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse jdExchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            jdAccessToken,
            jdExchangeRateRequestCreate
        );

        ExchangeRateRequestCreate msExchangeRateRequestCreate1 = new ExchangeRateRequestCreate(
            2002,
            "CHF",
            "EUR",
            new BigDecimal("1.02")
        );
        ExchangeRateResponse msExchangeRateResponse1 = Utilities.createExchangeRate(
            restTestClient,
            msAccessToken,
            msExchangeRateRequestCreate1
        );

        ExchangeRateRequestCreate msExchangeRateRequestCreate2 = new ExchangeRateRequestCreate(
            2004,
            "CHF",
            "EUR",
            new BigDecimal("1.04")
        );
        ExchangeRateResponse msExchangeRateResponse2 = Utilities.createExchangeRate(
            restTestClient,
            msAccessToken,
            msExchangeRateRequestCreate2
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(msAccessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedBoyJSON =
            """
            {
                resources: [
                    {
                        id: %d,
                        year: 2002,
                        sourceCurrency: "CHF",
                        targetCurrency: "EUR",
                        sourceToTargetFactor: 1.02
                    },
                    {
                        id: %d,
                        year: 2004,
                        sourceCurrency: "CHF",
                        targetCurrency: "EUR",
                        sourceToTargetFactor: 1.04
                    }
                ],
                paginationInfo: {
                    total: 2,
                    pageNumber: 0,
                    pageSize: 10
                }
            }
            """.formatted(
                msExchangeRateResponse1.id(),
                msExchangeRateResponse2.id()
            )
        ;
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBoyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }
}
