package com.kaloyan_marinov.personal_finance_tracker.totals;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

import com.kaloyan_marinov.personal_finance_tracker.exchange_rate.ExchangeRateRequestCreate;
import com.kaloyan_marinov.personal_finance_tracker.transaction.Transaction;
import com.kaloyan_marinov.personal_finance_tracker.transaction.TransactionRequest;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:test",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.mysql.sql",
        "pft.security.user.name=application-user-for-test-suite-u",
        "pft.security.user.password=application-user-for-test-suite-p"
    }
)
@AutoConfigureRestTestClient
@Transactional
class TotalsControllerTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void computeTotals_01() {
		// Act + Assert.
		String expectedBodyJSON = """
			{
				"error": "Validation failed",
				"fieldErrors": {
					"year": "must be provided"
				}
			}
		""";

		restTestClient
			.get().uri("/api/v1/totals")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	@Test
	void computeTotals_02() {
		// Arrange.
		String nonIntegerYear = "abc";

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
			.get().uri("/api/v1/totals?year=" + nonIntegerYear )
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	@Test
	void computeTotals_03() {
		// Arrange.
		int year = 2030;

		createTransaction(year - 1, "-10.00", "EUR");

		// Act.
		TotalsResponse observed = restTestClient
			.get().uri("/api/v1/totals?year=" + year)
			.exchange()
			.expectStatus().isOk()
			.expectBody(TotalsResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert.
		Map<String, BigDecimal> expectedExpenses = Map.of();
		Map<String, BigDecimal> expectedIncomes = Map.of();
		TotalsResponse expected = new TotalsResponse(
			year,
			expectedExpenses,
			expectedIncomes
		);
		assertEquals(expected, observed);
	}

	/**
	 * Covers a currency with expenses and incomes (EUR),
	 * a currency with expenses only (USD),
	 * and a currency with incomes only (CHF).
	 */
	@Test
	void computeTotals_04() {
		// Arrange.
		int year = 2026;

		createTransaction(year, "-10.00", "EUR");
		createTransaction(year, "-20.00", "EUR");
		createTransaction(year, "300.00", "EUR");

		createTransaction(year, "-400.00", "USD");

		createTransaction(year, "5000.00", "CHF");
		createTransaction(year, "6000.00", "CHF");

		// Act.
		TotalsResponse observed = restTestClient
			.get().uri("/api/v1/totals?year=" + year)
			.exchange()
			.expectStatus().isOk()
			.expectBody(TotalsResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert.
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
		assertEquals(expected, observed);
	}

	/**
	 * Covers a currency with only a zero amount (JPY, which must appear in neither map)
	 * and a {@link Transaction} in another year (which must be ignored),
	 * alongside a currency with expenses and incomes (EUR) (so that each map will end up being non-empty).
	 */
	@Test
	void computeTotals_05() {
		// Arrange.
		int year = 2026;

		createTransaction(year, "100.00", "EUR");
		createTransaction(year, "-20.00", "EUR");

		createTransaction(year, "0.00", "JPY");

		createTransaction(year + 1, "-70000.00", "EUR");

		// Act.
		TotalsResponse observed = restTestClient
			.get().uri("/api/v1/totals?year=" + year)
			.exchange()
			.expectStatus().isOk()
			.expectBody(TotalsResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert.
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
		assertEquals(expected, observed);
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
		int year = 2030;
		String currencyToReportTotalsIn = "EUR";

		createTransaction(year - 1, "-10.00", "USD");

		// Act.
		TotalsResponse observed = restTestClient
			.get().uri("/api/v1/totals?year=" + year + "&currency=" + currencyToReportTotalsIn)
			.exchange()
			.expectStatus().isOk()
			.expectBody(TotalsResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert.
		TotalsResponse expected = new TotalsResponse(
			year,
			Map.of(),
			Map.of()
		);
		assertEquals(expected, observed);
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
			.get().uri("/api/v1/totals?year=2026&currency=")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
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
		String currencyToReportTotalsIn = "eur";

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
			.get().uri("/api/v1/totals?year=2026&currency=" + currencyToReportTotalsIn)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
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
		int year = 2026;
		String currencyToReportTotalsIn = "EUR";

		createTransaction(year, "-10.00", currencyToReportTotalsIn);
		createTransaction(year, "-20.00", "USD");
		createTransaction(year, "30.00", "CHF");

		createExchangeRate(
			year,
			currencyToReportTotalsIn,
			"USD",
			"1.1"
		);

		createExchangeRate(
			year - 1,
			"CHF",
			currencyToReportTotalsIn,
			"1.05"
		);

		createExchangeRate(
			year,
			"USD",
			"GBP",
			"0.75"
		);
		createExchangeRate(
			year,
			"GBP",
			currencyToReportTotalsIn,
			"1.15"
		);

		// Act + Assert.
		String expectedBodyJSON = """
			{
				"error": "Missing exchange rates",
				"missingExchangeRates": [
					{ "year": 2026, "sourceCurrency": "CHF", "targetCurrency": "EUR" },
					{ "year": 2026, "sourceCurrency": "USD", "targetCurrency": "EUR" }
				]
			}
		""";

		restTestClient
			.get().uri("/api/v1/totals?year=" + year + "&currency=" + currencyToReportTotalsIn)
			.exchange()
			.expectStatus().isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT)
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
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
		int year = 2026;
		String currencyToReportTotalsIn = "EUR";

		createTransaction(year, "100.00", currencyToReportTotalsIn);
		createTransaction(year, "200.50", currencyToReportTotalsIn);

		// Act.
		TotalsResponse observed = restTestClient
			.get().uri("/api/v1/totals?year=" + year + "&currency=" + currencyToReportTotalsIn)
			.exchange()
			.expectStatus().isOk()
			.expectBody(TotalsResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert.
		TotalsResponse expected = new TotalsResponse(
			year,
			Map.of(),
			Map.of(currencyToReportTotalsIn, new BigDecimal("300.50"))
		);
		assertEquals(expected, observed);
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
		int year = 2026;
		String currencyToReportTotalsIn = "GBP";

		createTransaction(year, "-40.00", "EUR");
		createTransaction(year, "80.00", "USD");

		createExchangeRate(year, "EUR", currencyToReportTotalsIn, "0.85");
		createExchangeRate(year, "USD", currencyToReportTotalsIn, "0.75");

		// Act.
		TotalsResponse observed = restTestClient
			.get().uri("/api/v1/totals?year=" + year + "&currency=" + currencyToReportTotalsIn)
			.exchange()
			.expectStatus().isOk()
			.expectBody(TotalsResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert.
		TotalsResponse expected = new TotalsResponse(
			year,
			Map.of(currencyToReportTotalsIn, new BigDecimal("-34.00")),
			Map.of(currencyToReportTotalsIn, new BigDecimal("60.00"))
		);
		assertEquals(expected, observed);
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
		int year = 2026;
		String currencyToReportTotalsIn = "EUR";

		createTransaction(year, "-10.00", currencyToReportTotalsIn);
		createTransaction(year, "-100.01", "USD");
		createTransaction(year, "-100.01", "CHF");

		createTransaction(year, "100.00", currencyToReportTotalsIn);
		createTransaction(year, "100.01", "USD");

		createExchangeRate(year, "USD", currencyToReportTotalsIn, "0.5");
		createExchangeRate(year, "CHF", currencyToReportTotalsIn, "0.5");

		// Act.
		TotalsResponse observed = restTestClient
			.get().uri("/api/v1/totals?year=" + year + "&currency=" + currencyToReportTotalsIn)
			.exchange()
			.expectStatus().isOk()
			.expectBody(TotalsResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert.
		// expenses: -10.00 + (-50.005) + (-50.005) = -110.01
		// (rounding before summing would give -110.02)
		// incomes: 100.00 + 50.005 = 150.005, which `HALF_UP` rounds to 150.01
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
		assertEquals(expected, observed);
	}

	private void createTransaction(int year, String amount, String currency) {
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			new BigDecimal(amount),
			currency,
			"description"
		);

		restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest)
			.exchange()
			.expectStatus().isCreated();
	}

	private void createExchangeRate(
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
			.body(exchangeRateRequest)
			.exchange()
			.expectStatus().isCreated();
	}
}
