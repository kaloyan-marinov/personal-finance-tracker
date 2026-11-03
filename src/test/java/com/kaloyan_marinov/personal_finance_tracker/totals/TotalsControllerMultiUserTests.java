package com.kaloyan_marinov.personal_finance_tracker.totals;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import com.kaloyan_marinov.personal_finance_tracker.exchange_rate.ExchangeRateRequestCreate;
import com.kaloyan_marinov.personal_finance_tracker.token.TokenResponse;
import com.kaloyan_marinov.personal_finance_tracker.transaction.TransactionRequest;
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
class TotalsControllerMultiUserTests {

    @Autowired
    private RestTestClient restTestClient;

    private String obtainAccessToken(
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

        TokenResponse tokenResponse = restTestClient
            .post().uri("/api/v1/tokens")
            .headers(
                h -> h.setBasicAuth(
                    userRequestCreate.email(),
                    userRequestCreate.password()
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

    /**
     * Given two distinct users (User 1 and User 2),
     * each of whom has/owns nonzero transactions,
     * with all transactions in a single currency and in a single year.
     * 
     * When User 1 issues `GET /api/v1/totals`,
     * the response should be based _only_ on that user's transactions.
     */
    @Test
    void computeTotals_1() {
        // Arrange.
        String accessTokenForUser1 = obtainAccessToken(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessTokenForUser2 = obtainAccessToken(
            "test-" + "ms",
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        Integer year = 2006;
        String currency = "EUR";

        createTransaction(
            accessTokenForUser1,
            year,
            "-600.00",
            currency
        );

        createTransaction(
            accessTokenForUser2,
            year,
            "-700.00",
            currency
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=" + year)
            .headers(
                h -> h.setBearerAuth(accessTokenForUser1)
            )
            .exchange()
        ;

        // Assert.
        TotalsResponse observed = responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(TotalsResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        Map<String, BigDecimal> expectedExpenses = Map.of(
            currency, new BigDecimal("-600.00")
        );
        Map<String, BigDecimal> expectedIncomes = Map.of();
        TotalsResponse expected = new TotalsResponse(
            year,
            expectedExpenses,
            expectedIncomes
        );
        assertEquals(expected, observed);
    }

    /**
     * Given two distinct users (User 1 and User 2),
     * each of whom has/owns nonzero transactions,
     * with all transactions in a single currency C1 and in a single year Y.
     *
     * User 1 has an exchange rate from C1 to C2 for Y.
     * User 2 does not have such an exchange rate.
     * 
     * When User 2 issues `GET /api/v1/totals` for Y in C2,
     * the response should not utilize User 1's exchange rate.
     */
    @Test
    void computeTotals_2() {
        // Arrange.
        String accessTokenForUser1 = obtainAccessToken(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessTokenForUser2 = obtainAccessToken(
            "test-" + "ms",
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        Integer year = 2006;
        String currency = "EUR";

        createTransaction(
            accessTokenForUser1,
            year,
            "-600.00",
            currency
        );

        createTransaction(
            accessTokenForUser2,
            year,
            "-700.00",
            currency
        );

        String currencyToReportTotalsIn = "CHF";
        createExchangeRate(
            accessTokenForUser1,
            year,
            currency,
            currencyToReportTotalsIn,
            "1.234"
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=" + year + "&currency=" + currencyToReportTotalsIn)
            .headers(
                h -> h.setBearerAuth(accessTokenForUser2)
            )
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                "error": "Missing exchange rates",
                "missingExchangeRates": [
                    {
                        "year": 2006,
                        "sourceCurrency": "EUR",
                        "targetCurrency": "CHF"
                    }
                ]
            }
            """
        ;
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }

    private void createTransaction(
        String accessToken,
        int year,
        String amount,
        String currency
        // String description
    ) {
        TransactionRequest transactionRequest = new TransactionRequest(
            year,
            new BigDecimal(amount),
            currency,
            "description"
        );

        restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
            .expectStatus().isCreated();
    }

    private void createExchangeRate(
        String accessToken,
        int year,
        String sourceCurrency,
        String targetCurrency,
        String sourceToTargetFactor
    ) {
        ExchangeRateRequestCreate exchangeRateRequest = new ExchangeRateRequestCreate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal(sourceToTargetFactor)
        );

        restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequest)
            .exchange()
            .expectStatus().isCreated();
    }
}
