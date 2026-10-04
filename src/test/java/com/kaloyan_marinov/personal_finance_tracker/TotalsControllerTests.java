package com.kaloyan_marinov.personal_finance_tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
	properties = {
		"spring.datasource.url=jdbc:h2:mem:test",
		"spring.jpa.hibernate.ddl-auto=none",
		"spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.mysql.sql"
	}
)
@AutoConfigureRestTestClient
@Transactional
class TotalsControllerTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	void computeTotals_1() {
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
			.get().uri("/totals")
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	@Test
	void computeTotals_2() {
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
			.get().uri("/totals?year=" + nonIntegerYear )
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	@Test
	void computeTotals_3() {
		// Arrange.
		int year = 2030;

		createTransaction(year - 1, "-10.00", "EUR");

		// Act.
		TotalsResponse observed = restTestClient
			.get().uri("/totals?year=" + year)
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
	void computeTotals_4() {
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
			.get().uri("/totals?year=" + year)
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
	void computeTotals_5() {
		// Arrange.
		int year = 2026;

		createTransaction(year, "100.00", "EUR");
		createTransaction(year, "-20.00", "EUR");

		createTransaction(year, "0.00", "JPY");

		createTransaction(year + 1, "-70000.00", "EUR");

		// Act.
		TotalsResponse observed = restTestClient
			.get().uri("/totals?year=" + year)
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

	private void createTransaction(int year, String amount, String currency) {
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			new BigDecimal(amount),
			currency,
			"description"
		);

		restTestClient
			.post().uri("/transactions")
			.body(transactionRequest)
			.exchange()
			.expectStatus().isCreated();
	}
}
