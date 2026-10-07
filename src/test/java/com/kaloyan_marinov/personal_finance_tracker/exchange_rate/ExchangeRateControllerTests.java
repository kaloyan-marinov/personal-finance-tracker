package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import java.math.BigDecimal;

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
class ExchangeRateControllerTests {

	@Autowired
	private RestTestClient restTestClient;

	private ExchangeRate createExchangeRate(
		ExchangeRateRequestCreate exchangeRateRequestCreate
	) {
		return restTestClient
			.post().uri("/api/v1/exchange-rates")
			.body(exchangeRateRequestCreate)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(ExchangeRate.class)
			.returnResult()
			.getResponseBody();
	}

	/**
	 * POSTs a valid exchange rate whose factor uses all 8 decimal places
	 * and reads it back by its natural key.
	 */
	@Test
	void create_1() {
		// Arrange.
		ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
			2001,
			"JPY",
			"EUR",
			new BigDecimal("0.00571234")
		);

		// Act.
		ExchangeRate exchangeRate = createExchangeRate(exchangeRateRequestCreate);

		// Assert.
		String expectedBodyJSON = """
			{
				id: %d,
				year: 2001,
				sourceCurrency: "JPY",
				targetCurrency: "EUR",
				sourceToTargetFactor: 0.00571234
			}
		""".formatted(exchangeRate.getId());

		restTestClient
			.get().uri("/api/v1/exchange-rates/2001/JPY/EUR")
			.exchange()
			.expectStatus().isOk()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	/**
	 * POSTs a rate and then a second rate with the same natural key but a different factor.
	 * Expects a 409 response for the second one, which mirrors {@code uq_exchange_rates_year_currencies}.
	 */
	@Test
	void create_2() {
		// Arrange.
		Integer year = 2002;
		String sourceCurrency = "EUR";
		String targetCurrency = "USD";

		ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
			year,
			sourceCurrency,
			targetCurrency,
			new BigDecimal("1.085")
		);
		createExchangeRate(exchangeRateRequestCreate);

		
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
	void create_3() {
		// Arrange.
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
		createExchangeRate(exchangeRateRequestCreate1);
		createExchangeRate(exchangeRateRequestCreate2);
	}

	/**
	 * POSTs an otherwise valid rate without a {@code year}.
	 * Expects a 400 response, which reports the missing field.
	 */
	@Test
	void create_4() {
		// Arrange.
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
	void create_5() {
		String trgtCurrency = "USD";

		for (String srcCurrency : new String[] {"", "eur", "Eur", "EU", "EURO", "E1R"}) {
			// Arrange.
			ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
				2005,
				srcCurrency,
				trgtCurrency,
				new BigDecimal("1.085")
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
	void create_6() {
		String srcCurrency = "USD";

		for (String trgtCurrency : new String[] {"", "eur", "Eur", "EU", "EURO", "E1R"}) {
			// Arrange.
			ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
				2006,
				srcCurrency,
				trgtCurrency,
				new BigDecimal("0.92")
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
	void create_7() {
		// Arrange.
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
	void create_8() {
		for (String srcToTrgtFactor : new String[] {"0", "-1.085"}) {
			// Arrange.
			ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
				2008,
				"EUR",
				"USD",
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
	void create_9() {
		for (String srcToTrgtFactor : new String[] {"0.000000001", "12345678901"}) {
			// Arrange.
			ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
				2009,
				"EUR",
				"USD",
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
	 * GETs a rate which does not exist.
	 * Expects a 404 response with an empty body.
	 */
	@Test
	void retrieveOne_1() {
		// Act + Assert.
		restTestClient
			.get().uri("/api/v1/exchange-rates/2001/EUR/USD")
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
		ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
			2003, 
			"EUR",
			"USD",
			new BigDecimal("1.085")
		);
		createExchangeRate(exchangeRateRequestCreate);

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
		// Act + Assert.
		restTestClient
			.get().uri("/api/v1/exchange-rates")
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
		ExchangeRateRequestCreate exchangeRateRequestCreate1 = new ExchangeRateRequestCreate(
			2001,
			"EUR",
			"USD",
			new BigDecimal("1.085")
		);
		ExchangeRate exchangeRate1 = createExchangeRate(exchangeRateRequestCreate1);

		ExchangeRateRequestCreate exchangeRateRequestCreate2 = new ExchangeRateRequestCreate(
			2002,
			"CHF",
			"EUR",
			new BigDecimal("1.06")
		);
		ExchangeRate exchangeRate2 = createExchangeRate(exchangeRateRequestCreate2);

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
			exchangeRate1.getId(),
			exchangeRate2.getId()
		);

		restTestClient
			.get().uri("/api/v1/exchange-rates")
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
		ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
			2001,
			"EUR",
			"USD",
			new BigDecimal("1.085")
		);
		ExchangeRate exchangeRate = createExchangeRate(exchangeRateRequestCreate);

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
		""".formatted(exchangeRate.getId());

		restTestClient
			.put().uri("/api/v1/exchange-rates/2001/EUR/USD")
			.body(exchangeRateRequestUpdate)
			.exchange()
			.expectStatus().isOk()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);

		restTestClient
			.get().uri("/api/v1/exchange-rates/2001/EUR/USD")
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
		ExchangeRateRequestUpdate exchangeRateRequestUpdate = new ExchangeRateRequestUpdate(
			new BigDecimal("1.1")
		);

		// Act + Assert.
		restTestClient
			.put().uri("/api/v1/exchange-rates/2002/EUR/USD")
			.body(exchangeRateRequestUpdate)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().isEmpty();

		restTestClient
			.get().uri("/api/v1/exchange-rates/2002/EUR/USD")
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
		ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
			2003,
			"EUR",
			"USD",
			new BigDecimal("1.085")
		);
		ExchangeRate exchangeRate = createExchangeRate(exchangeRateRequestCreate);

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
		""".formatted(exchangeRate.getId());

		restTestClient
			.get().uri("/api/v1/exchange-rates/2003/EUR/USD")
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
		ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
			2004,
			"EUR",
			"USD",
			new BigDecimal("1.085")
		);
		ExchangeRate exchangeRate = createExchangeRate(exchangeRateRequestCreate);

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
		""".formatted(exchangeRate.getId());

		restTestClient
			.get().uri("/api/v1/exchange-rates/2004/EUR/USD")
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
		// Act + Assert.
		restTestClient
			.delete().uri("/api/v1/exchange-rates/2001/EUR/USD")
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
		ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
			2002,
			"EUR",
			"USD",
			new BigDecimal("1.085")
		);
		createExchangeRate(exchangeRateRequestCreate);

		// Act + Assert.
		restTestClient
			.delete().uri("/api/v1/exchange-rates/2002/EUR/USD")
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();

		restTestClient
			.get().uri("/api/v1/exchange-rates/2002/EUR/USD")
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
		ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
			2003,
			"EUR",
			"USD",
			new BigDecimal("1.085")
		);
		createExchangeRate(exchangeRateRequestCreate);

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
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);

		restTestClient
			.get().uri("/api/v1/exchange-rates/2003/EUR/USD")
			.exchange()
			.expectStatus().isOk();
	}
}
