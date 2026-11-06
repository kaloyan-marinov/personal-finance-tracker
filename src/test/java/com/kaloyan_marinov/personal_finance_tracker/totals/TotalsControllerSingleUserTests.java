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
import com.kaloyan_marinov.personal_finance_tracker.transaction.Transaction;
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
class TotalsControllerSingleUserTests {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void computeTotals_01() {
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
            .get().uri("/api/v1/totals")
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
                    "year": "must be provided"
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
    void computeTotals_02() {
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
            .get().uri("/api/v1/totals?year=" + inadmissibleYear)
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

    @Test
    void computeTotals_03() {
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

        int year = 2030;

        TransactionRequest transactionRequest = new TransactionRequest(
            year - 1,
            new BigDecimal("-10.00"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequest
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=" + year)
            .headers(
                h -> h.setBearerAuth(accessToken)
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

        Map<String, BigDecimal> expectedExpenses = Map.of();
        Map<String, BigDecimal> expectedIncomes = Map.of();
        TotalsResponse expected = new TotalsResponse(
            year,
            expectedExpenses,
            expectedIncomes
        );
        Assertions.assertEquals(expected, observed);
    }

    /**
     * Covers a currency with expenses and incomes (EUR),
     * a currency with expenses only (USD),
     * and a currency with incomes only (CHF).
     */
    @Test
    void computeTotals_04() {
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

        int year = 2026;

        // Create transaction(s) in EUR.
        TransactionRequest transactionRequestEUR1 = new TransactionRequest(
            year,
            new BigDecimal("-10.00"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestEUR1
        );

        TransactionRequest transactionRequestEUR2 = new TransactionRequest(
            year,
            new BigDecimal("-20.00"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestEUR2
        );

        TransactionRequest transactionRequestEUR3 = new TransactionRequest(
            year,
            new BigDecimal("300.00"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestEUR3
        );

        // Create transaction(s) in USD.
        TransactionRequest transactionRequestUSD1 = new TransactionRequest(
            year,
            new BigDecimal("-400.00"),
            "USD",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestUSD1
        );

        // Create transaction(s) in CHF.
        TransactionRequest transactionRequestCHF1 = new TransactionRequest(
            year,
            new BigDecimal("5000.00"),
            "CHF",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestCHF1
        );

        TransactionRequest transactionRequestCHF2 = new TransactionRequest(
            year,
            new BigDecimal("6000.00"),
            "CHF",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestCHF2
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=" + year)
            .headers(
                h -> h.setBearerAuth(accessToken)
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

        // `BigDecimal.equals` compares the scale too,
        // so this also checks for 2 decimal places.
        Map<String, BigDecimal> expectedExpenses = Map.of(
            "EUR", new BigDecimal("-30.00"),
            "USD", new BigDecimal("-400.00")
        );
        Map<String, BigDecimal> expectedIncomes = Map.of(
            "EUR", new BigDecimal("300.00"),
            "CHF", new BigDecimal("11000.00")
        );
        TotalsResponse expected = new TotalsResponse(
            year,
            expectedExpenses,
            expectedIncomes
        );
        Assertions.assertEquals(expected, observed);
    }

    /**
     * Covers a currency with only a zero amount (JPY, which must appear in neither map)
     * and a {@link Transaction} in another year (which must be ignored),
     * alongside a currency with expenses and incomes (EUR) (so that each map will end up being non-empty).
     */
    @Test
    void computeTotals_05() {
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

        int year = 2026;

        // Create transaction(s) in EUR.
        TransactionRequest transactionRequestEUR1 = new TransactionRequest(
            year,
            new BigDecimal("100.00"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestEUR1
        );

        TransactionRequest transactionRequestEUR2 = new TransactionRequest(
            year,
            new BigDecimal("-20.00"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestEUR2
        );

        // Create transaction(s) in JPY.
        TransactionRequest transactionRequestJPY1 = new TransactionRequest(
            year,
            new BigDecimal("0.00"),
            "JPY",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestJPY1
        );

        // Create transaction(s) in EUR.
        TransactionRequest transactionRequestEUR3 = new TransactionRequest(
            year + 1,
            new BigDecimal("-70000.00"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestEUR3
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=" + year)
            .headers(
                h -> h.setBearerAuth(accessToken)
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
            "EUR", new BigDecimal("-20.00")
        );
        Map<String, BigDecimal> expectedIncomes = Map.of(
            "EUR", new BigDecimal("100.00")
        );
        TotalsResponse expected = new TotalsResponse(
            year,
            expectedExpenses,
            expectedIncomes
        );
        Assertions.assertEquals(expected, observed);
    }

    /**
     * This exercises a "happy but atypical" success path.
     *
     * Covers a year without any {@link Transaction}s,
     * which requires no {@code ExchangeRate}s at all.
     */
    @Test
    void computeTotals_06() {
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

        int year = 2030;
        String currencyToReportTotalsIn = "EUR";

        // Create transaction(s) in USD.
        TransactionRequest transactionRequestUSD1 = new TransactionRequest(
            year - 1,
            new BigDecimal("-10.00"),
            "USD",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestUSD1
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=" + year + "&currency=" + currencyToReportTotalsIn)
            .headers(
                h -> h.setBearerAuth(accessToken)
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

        TotalsResponse expected = new TotalsResponse(
            year,
            Map.of(),
            Map.of()
        );
        Assertions.assertEquals(expected, observed);
    }

    /**
     * This exercises an unhappy path.
     *
     * Covers an empty {@code currency} ({@code ?currency=}),
     * which Spring passes as {@code ""} rather than {@code null}.
     *
     * Expects a 400 (like {@code computeTotals_08}),
     * rather than the per-currency totals reported when {@code currency} is omitted.
     */
    @Test
    void computeTotals_07() {
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

        String inadmissibleCurrency = "";

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=2026&currency=" + inadmissibleCurrency)
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

    /**
     * This exercises an unhappy path.
     *
     * Covers a {@code currency} which does not match the expected {@code @Pattern}.
     *
     * Expects a 400 with the same body shape as the existing validation errors.
     */
    @Test
    void computeTotals_08() {
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

        String inadmissibleCurrency = "eur";

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=2026&currency=" + inadmissibleCurrency)
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

    /**
     * This exercises an unhappy path.
     *
     * Covers 2 missing {@code ExchangeRate}s (CHF -> EUR, USD -> EUR),
     * while the database holds rates which must not be used instead:
     * an inverse rate (EUR -> USD),
     * a rate for another year (CHF -> EUR),
     * and rates which would allow chaining (USD -> GBP -> EUR).
     *
     * Expects every missing rate to be reported,
     * ordered by {@code sourceCurrency}.
     */
    @Test
    void computeTotals_09() {
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

        int year = 2026;
        String currencyToReportTotalsIn = "EUR";

        // Create transaction(s) in EUR.
        TransactionRequest transactionRequestEUR1 = new TransactionRequest(
            year,
            new BigDecimal("-10.00"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestEUR1
        );

        // Create transaction(s) in USD.
        TransactionRequest transactionRequestUSD1 = new TransactionRequest(
            year,
            new BigDecimal("-20.00"),
            "USD",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestUSD1
        );

        // Create transaction(s) in CHF.
        TransactionRequest transactionRequestCHF1 = new TransactionRequest(
            year,
            new BigDecimal("30.00"),
            "CHF",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestCHF1
        );

        // Create exchange rates.
        ExchangeRateRequestCreate exchangeRateRequestCreate1 = new ExchangeRateRequestCreate(
            year,
            currencyToReportTotalsIn,
            "USD",
            new BigDecimal("1.1")
        );
        Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate1
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate2 = new ExchangeRateRequestCreate(
            year - 1,
            "CHF",
            currencyToReportTotalsIn,
            new BigDecimal("1.05")
        );
        Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate2
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate3 = new ExchangeRateRequestCreate(
            year,
            "USD",
            "GBP",
            new BigDecimal("0.75")
        );
        Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate3
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate4 = new ExchangeRateRequestCreate(
            year,
            "GBP",
            currencyToReportTotalsIn,
            new BigDecimal("1.15")
        );
        Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate4
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=" + year + "&currency=" + currencyToReportTotalsIn)
            .headers(
                h -> h.setBearerAuth(accessToken)
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
                        "year": 2026,
                        "sourceCurrency": "CHF",
                        "targetCurrency": "EUR"
                    },
                    {
                        "year": 2026,
                        "sourceCurrency": "USD",
                        "targetCurrency": "EUR"
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

    /**
     * This exercises a happy path.
     *
     * Covers totals which are all in the chosen currency (EUR) and are all incomes,
     * which requires no {@code ExchangeRate}s and yields {@code {}} for the expenses.
     */
    @Test
    void computeTotals_10() {
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

        int year = 2026;
        String currencyToReportTotalsIn = "EUR";

        // Create transaction(s) in EUR.
        TransactionRequest transactionRequestEUR1 = new TransactionRequest(
            year,
            new BigDecimal("100.00"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestEUR1
        );

        TransactionRequest transactionRequestEUR2 = new TransactionRequest(
            year,
            new BigDecimal("200.50"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestEUR2
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=" + year + "&currency=" + currencyToReportTotalsIn)
            .headers(
                h -> h.setBearerAuth(accessToken)
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

        TotalsResponse expected = new TotalsResponse(
            year,
            Map.of(),
            Map.of(
                currencyToReportTotalsIn, new BigDecimal("300.50")
            )
        );
        Assertions.assertEquals(expected, observed);
    }

    /**
     * This exercises a happy path.
     *
     * Covers a chosen currency (GBP) which none of the year's {@link Transaction}s are in.
     * Expects every total to be converted into it.
     */
    @Test
    void computeTotals_11() {
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

        int year = 2026;
        String currencyToReportTotalsIn = "GBP";

        // Create transaction(s) in EUR.
        TransactionRequest transactionRequestEUR1 = new TransactionRequest(
            year,
            new BigDecimal("-40.00"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestEUR1
        );

        // Create transaction(s) in USD.
        TransactionRequest transactionRequestUSD1 = new TransactionRequest(
            year,
            new BigDecimal("80.00"),
            "USD",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestUSD1
        );

        // Create exchange rates.
        ExchangeRateRequestCreate exchangeRateRequestCreate1 = new ExchangeRateRequestCreate(
            year,
            "EUR",
            currencyToReportTotalsIn,
            new BigDecimal("0.85")
        );
        Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate1
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate2 = new ExchangeRateRequestCreate(
            year,
            "USD",
            currencyToReportTotalsIn,
            new BigDecimal("0.75")
        );
        Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate2
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=" + year + "&currency=" + currencyToReportTotalsIn)
            .headers(
                h -> h.setBearerAuth(accessToken)
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

        TotalsResponse expected = new TotalsResponse(
            year,
            Map.of(
                currencyToReportTotalsIn, new BigDecimal("-34.00")
            ),
            Map.of(
                currencyToReportTotalsIn, new BigDecimal("60.00")
            )
        );

        Assertions.assertEquals(expected, observed);
    }

    /**
     * This exercises a happy path.
     *
     * Covers converting totals from several currencies (USD, CHF) into the chosen one (EUR),
     * alongside a total already in the chosen currency (EUR).
     * The amounts are picked so that
     * rounding each converted total before summing would give a different result,
     * and so that {@code HALF_UP} differs from {@code HALF_EVEN}.
     */
    @Test
    void computeTotals_12() {
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

        int year = 2026;
        String currencyToReportTotalsIn = "EUR";

        // Create transaction(s) in EUR.
        TransactionRequest transactionRequestEUR1 = new TransactionRequest(
            year,
            new BigDecimal("-10.00"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestEUR1
        );

        TransactionRequest transactionRequestEUR2 = new TransactionRequest(
            year,
            new BigDecimal("100.00"),
            "EUR",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestEUR2
        );

        // Create transaction(s) in USD.
        TransactionRequest transactionRequestUSD1 = new TransactionRequest(
            year,
            new BigDecimal("-100.01"),
            "USD",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestUSD1
        );

        TransactionRequest transactionRequestUSD2 = new TransactionRequest(
            year,
            new BigDecimal("100.01"),
            "USD",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestUSD2
        );

        // Create transaction(s) in CHF.
        TransactionRequest transactionRequestCHF1 = new TransactionRequest(
            year,
            new BigDecimal("-100.01"),
            "CHF",
            "description"
        );
        Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequestCHF1
        );

        // Create exchange rates.
        ExchangeRateRequestCreate exchangeRateRequestCreate1 = new ExchangeRateRequestCreate(
            year,
            "USD",
            currencyToReportTotalsIn,
            new BigDecimal("0.5")
        );
        Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate1
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate2 = new ExchangeRateRequestCreate(
            year,
            "CHF",
            currencyToReportTotalsIn,
            new BigDecimal("0.5")
        );
        Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate2
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/totals?year=" + year + "&currency=" + currencyToReportTotalsIn)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        // expenses: -10.00 + (-50.005) + (-50.005) = -110.01
        // (rounding before summing would give -110.02)
        // incomes: 100.00 + 50.005 = 150.005, which `HALF_UP` rounds to 150.01
        TotalsResponse observed = responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(TotalsResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        Map<String, BigDecimal> expectedExpenses = Map.of(
            currencyToReportTotalsIn, new BigDecimal("-110.01")
        );
        Map<String, BigDecimal> expectedIncomes = Map.of(
            currencyToReportTotalsIn, new BigDecimal("150.01")
        );
        TotalsResponse expected = new TotalsResponse(
            year,
            expectedExpenses,
            expectedIncomes
        );

        Assertions.assertEquals(expected, observed);
    }
}
