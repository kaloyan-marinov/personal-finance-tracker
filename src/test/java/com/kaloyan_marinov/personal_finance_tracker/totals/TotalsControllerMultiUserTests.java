package com.kaloyan_marinov.personal_finance_tracker.totals;

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
import com.kaloyan_marinov.personal_finance_tracker.exchange_rate.ExchangeRateRequestCreate;
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

        Integer year = 2006;
        String currency = "EUR";

        // Create transaction(s) in EUR.
        TransactionRequest jdTransactionRequestEUR = new TransactionRequest(
            year,
            new BigDecimal("-600.00"),
            currency,
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            jdAccessToken,
            jdTransactionRequestEUR
        );

        TransactionRequest msTransactionRequestEUR = new TransactionRequest(
            year,
            new BigDecimal("-700.00"),
            currency,
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            msAccessToken,
            msTransactionRequestEUR
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=" + year)
            .headers(
                h -> h.setBearerAuth(jdAccessToken)
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
        Assertions.assertEquals(expected, observed);
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

        Integer year = 2006;
        String currency = "EUR";

        // Create transaction(s) in EUR.
        TransactionRequest jdTransactionRequestEUR = new TransactionRequest(
            year,
            new BigDecimal("-600.00"),
            currency,
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            jdAccessToken,
            jdTransactionRequestEUR
        );

        TransactionRequest msTransactionRequestEUR = new TransactionRequest(
            year,
            new BigDecimal("-700.00"),
            currency,
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            msAccessToken,
            msTransactionRequestEUR
        );

        String currencyToReportTotalsIn = "CHF";

        ExchangeRateRequestCreate jdExchangeRateRequestCreate = new ExchangeRateRequestCreate(
            year,
            currency,
            currencyToReportTotalsIn,
            new BigDecimal("1.234")
        );
        Utilities.createExchangeRate(
            restTestClient,
            jdAccessToken,
            jdExchangeRateRequestCreate
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=" + year + "&currency=" + currencyToReportTotalsIn)
            .headers(
                h -> h.setBearerAuth(msAccessToken)
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
}
