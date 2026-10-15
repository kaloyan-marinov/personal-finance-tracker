package com.kaloyan_marinov.personal_finance_tracker.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.mysql.sql",
        "pft.security.user.name=application-user-for-test-suite-u",
        "pft.security.user.password=application-user-for-test-suite-p"
    }
)
@AutoConfigureRestTestClient
@Transactional
class TransactionControllerTests {

	@Autowired 
	private RestTestClient restTestClient;

	@Test
	void create_1() {
		// Arrange.
		int year = 2001;
		BigDecimal amount = null;
		String currency = "EUR";
		String description = "groceries";
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			amount,
			currency,
			description
		);

		// Act + Assert.
		String expectedBodyJSON = """
			{
				"error": "Validation failed",
				"fieldErrors": {
					"amount": "must not be null"
				}
			}	
		""";

		restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	@Test
	void create_2() {
		// Arrange.
		int year = 2002;
		BigDecimal amount = new BigDecimal("-4.789");
		String currency = "EUR";
		String description = "groceries";
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			amount,
			currency,
			description
		);

		// Act + Assert.
		String expectedBodyJSON = """
			{
				"error": "Validation failed",
				"fieldErrors": {
					"amount": "numeric value out of bounds (<16 digits>.<2 digits> expected)"
				}
			}
		""";

		restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	/**
	 * Loops over {@code null}, {@code ""} and {@code " "}.
	 * For each one, POSTs an otherwise valid transaction with that description.
	 * Expects a 400 response with {@code "description": "must not be blank"},
	 * using the same {@code STRICT} JSON check as elsewhere in the encompassing test-suite class.
	 */
	@Test
	void create_3() {
		for (String description : new String[] {null, "", " "}) {
			// Arrange.
			int year = 2003;
			BigDecimal amount = new BigDecimal("-300.00");
			String currency = "EUR";
			TransactionRequest transactionRequest = new TransactionRequest(
				year,
				amount,
				currency,
				description
			);

			// Act + Assert.
			String expectedBodyJSON = """
				{
					"error": "Validation failed",
					"fieldErrors": {
						"description": "must not be blank"
					}
				}
			""";

			restTestClient
				.post().uri("/api/v1/transactions")
				.body(transactionRequest)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().json(
					expectedBodyJSON,
					JsonCompareMode.STRICT
				);
		}
	}

	@Test
	void create_4() {
		// Arrange.
		int year = 2004;
		BigDecimal amount = new BigDecimal("-400.00");
		String currency = null;
		String description = "groceries";
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			amount,
			currency,
			description
		);

		// Act + Assert.
		String expectedBodyJSON = """
			{
				"error": "Validation failed",
				"fieldErrors": {
					"currency": "must not be null"
				}
			}
		""";

		restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	/**
	 * Loops over currency codes which are not exactly 3 uppercase Latin letters.
	 * For each one, POSTs an otherwise valid transaction with that currency.
	 * Expects a 400 response, which reports the violated pattern.
	 */
	@Test
	void create_5() {
		for (String currency : new String[] {"", "eur", "Eur", "EU", "EURO", "E1R"}) {
			// Arrange.
			int year = 2005;
			BigDecimal amount = new BigDecimal("-500.00");
			String description = "groceries";
			TransactionRequest transactionRequest = new TransactionRequest(
				year,
				amount,
				currency,
				description
			);

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
				.post().uri("/api/v1/transactions")
				.body(transactionRequest)
				.exchange()
				.expectStatus().isBadRequest()
				.expectBody().json(
					expectedBodyJSON,
					JsonCompareMode.STRICT
				);
		}
	}

	@Test
	void create_6() {
		// Arrange.
		int year = 2006;
		BigDecimal amount = new BigDecimal("-600.00");
		String currency = "EUR";
		String description = "x".repeat(256);
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			amount,
			currency,
			description
		);

		// Act + Assert.
		String expectedBodyJSON = """
			{
				"error": "Validation failed",
				"fieldErrors": {
					"description": "size must be between 0 and 255"
				}
			}
		""";

		restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	/**
	 * POSTs a transaction whose description has the maximum allowed length (255)
	 * and reads it back,
	 * so as to check that the database columns (`CHAR(3)` and `VARCHAR(255)`)
	 * store the currency and the description without truncating or padding them.
	 */
	@Test
	void create_7() {
		// Arrange.
		int year = 2007;
		BigDecimal amount = new BigDecimal("-700.00");
		String currency = "CHF";
		String description = "x".repeat(255);
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			amount,
			currency,
			description
		);

		// Act.
		Transaction transaction = restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
			
		// Assert.
		assertEquals(transactionRequest.year(), transaction.getYear());
		assertEquals(transactionRequest.amount(), transaction.getAmount());
		assertEquals(transactionRequest.currency(), transaction.getCurrency());
		assertEquals(transactionRequest.description(), transaction.getDescription());
			
		long transactionId = transaction.getId();
		String expectedBodyJSON = """
			{
				id: %d,
				year: 2007,
				amount: -700.00,
				currency: "CHF",
				description: "%s"
			}
		""".formatted(transactionId, description);

		restTestClient
			.get().uri("/api/v1/transactions/" + transactionId)
			.exchange()
			.expectStatus().isOk()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	/**
	 * POSTs a transaction whose body carries the {@code id} of an existing transaction.
	 * Expects the {@code id} to be ignored and a new transaction to be created,
	 * i.e. a POST must never overwrite an existing transaction.
	 */
	@Test
	void create_8() {
		// Arrange.
		int year = 2008;
		BigDecimal amount = new BigDecimal("-800.00");
		String currency = "EUR";
		String description = "groceries";
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			amount,
			currency,
			description
		);

		Transaction transaction = restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
		long transactionId = transaction.getId();

		// Since `TransactionRequest` has no `id`,
		// the body of the new request will be built as a `Map`.
		Map<String, Object> newTransactionRequestWithId = Map.of(
			"id", transactionId,
			"year", 2028,
			"amount", new BigDecimal("-8000.19"),
			"currency", "CHF",
			"description", "hotel stay"
		);

		// Act.
		Transaction newTransaction = restTestClient
			.post().uri("/api/v1/transactions")
			.body(newTransactionRequestWithId)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();

		// Assert.
		long newTransactionId = newTransaction.getId();
		assertNotEquals(transactionId, newTransactionId);

		String expectedBodyJSON = """
			{
				id: %d,
				year: %d,
				amount: %s,
				currency: "%s",
				description: "%s"
			}
		""".formatted(
			transactionId,
			year,
			amount,
			currency,
			description
		);
		restTestClient
			.get().uri("/api/v1/transactions/" + transactionId)
			.exchange()
			.expectStatus().isOk()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	@Test
	void retrieveOne_1() {
		// Arrange.
		long nonexistentId = 17;

		// Act + Assert.
		restTestClient
			.get().uri("/api/v1/transactions/" + nonexistentId)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().isEmpty();
	}

	@Test
	void retrieveOne_2() {
		// Arrange.
		int year = 2002;
		BigDecimal amount = new BigDecimal("-200.00");
		String currency = "EUR";
		String description = "groceries";
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			amount,
			currency,
			description
		);

		Transaction transaction = restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
		long transactionId = transaction.getId();

		// Act + Assert.
		String expectedBodyJSON = """
			{
				id: %d,
				year: 2002,
				amount: -200,
				currency: "EUR",
				description: "groceries"
			}
		""".formatted(transactionId);

		restTestClient
			.get().uri("/api/v1/transactions/" + transactionId)
			.exchange()
			.expectStatus().isOk()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	@Test
	void retrieveAll_1() {
		// Arrange.
		String expectedBodyJSON = """
			[]
		""";

		// Act + Assert.
		restTestClient
			.get().uri("/api/v1/transactions")
			.exchange()
			.expectStatus().isOk()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	@Test
	void retrieveAll_2() {
		// Arrange.
		int year6 = 2006;
		BigDecimal amount6 = new BigDecimal("-600.00");
		String currency6 = "EUR";
		String description6 = "groceries";
		TransactionRequest transactionRequest6 = new TransactionRequest(
			year6,
			amount6,
			currency6,
			description6
		);

		int year7 = 2007;
		BigDecimal amount7 = new BigDecimal("-700.00");
		String currency7 = "EUR";
		String description7 = "groceries";
		TransactionRequest transactionRequest7 = new TransactionRequest(
			year7,
			amount7,
			currency7,
			description7
		);

		Transaction transaction6 = restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest6)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
		
		Transaction transaction7 = restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest7)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();

		// Act + Assert.
		String expectedBodyJSON = """
			[
				{
					id: %d,
					year: 2006,
					amount: -600.00,
					currency: "EUR",
					description: "groceries"
				},
				{
					id: %d,
					year: 2007,
					amount: -700,
					currency: "EUR",
					description: "groceries"
				}
			]
		""".formatted(
			transaction6.getId(),
			transaction7.getId()
		);

		restTestClient
			.get().uri("/api/v1/transactions")
			.exchange()
			.expectStatus().isOk()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	@Test
	void update_1() {
		// Arrange.
		int year = 3000 + 2003;
		BigDecimal amount = new BigDecimal(
			String.valueOf(-(3000 + 300.00))
		);
		String currency = "EUR";
		String description = "groceries";
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			amount,
			currency,
			description
		);

		Transaction transaction = restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
		long transactionId = transaction.getId();

		Integer updatedYear = null;
		BigDecimal updatedAmount = new BigDecimal("-300.00");
		String updatedCurrency = "EUR";
		String updatedDescription = "groceries";
		TransactionRequest updateRequest = new TransactionRequest(
			updatedYear,
			updatedAmount,
			updatedCurrency,
			updatedDescription
		);

		// Act + Assert.
		String expectedBodyJSON1 = """
			{
				"error": "Validation failed",
				"fieldErrors": {
					"year": "must not be null"
				}
			}
		""";

		restTestClient
			.put().uri("/api/v1/transactions/" + transactionId)
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
				year: 5003,
				amount: -3300.00,
				currency: "EUR",
				description: "groceries"
			}
		""".formatted(transactionId);

		restTestClient
			.get().uri("/api/v1/transactions/" + transactionId)
			.exchange()
			.expectStatus().isOk()
			.expectBody().json(
				expectedBodyJSON2,
				JsonCompareMode.STRICT
			);
	}

	@Test
	void update_2() {
		// Arrange.
		int year = 2004;
		BigDecimal amount = new BigDecimal("-400.00");
		String currency = "EUR";
		String description = "groceries";
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			amount,
			currency,
			description
		);

		long nonexistentId = 17;

		// Arrange + Act.
		restTestClient
			.put().uri("/api/v1/transactions/" + nonexistentId)
			.body(transactionRequest)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().isEmpty();
	}

	@Test
	void update_3() {
		// Arrange.
		int year = 3000 + 2005;
		BigDecimal amount = new BigDecimal(
			String.valueOf(-(3000 + 500.00))
		);
		String currency = "EUR";
		String description = "groceries";
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			amount,
			currency,
			description
		);

		Transaction transaction = restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
		long transactionId = transaction.getId();

		int updatedYear = 2005;
		BigDecimal updatedAmount = new BigDecimal("-500.00");
		String updatedCurrency = "EUR";
		String updatedDescription = "groceries";
		TransactionRequest updateTransactionRequest = new TransactionRequest(
			updatedYear,
			updatedAmount,
			updatedCurrency,
			updatedDescription
		);

		// Act + Assert.
		String expectedBodyJSON = """
			{
				id: %d,
				year: 2005,
				amount: -500,
				currency: "EUR",
				description: "groceries"
			}
		""".formatted(transactionId);

		restTestClient
			.put().uri("/api/v1/transactions/" + transactionId)
			.body(updateTransactionRequest)
			.exchange()
			.expectStatus().isOk()
			.expectBody().json(
 				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
		
		restTestClient
			.get().uri("/api/v1/transactions/" + transactionId)
			.exchange()
			.expectStatus().isOk()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	@Test
	void delete_1() {
		// Arrange.
		long nonexistentId = 17;

		// Act + Assert.
		restTestClient
			.delete().uri("/api/v1/transactions/" + nonexistentId)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().isEmpty();
	}

	@Test
	void delete_2() {
		// Arrange.
		int year = 2006;
		BigDecimal amount = new BigDecimal("-600.00");
		String currency = "EUR";
		String description = "groceries";
		TransactionRequest transactionRequest = new TransactionRequest(
			year,
			amount,
			currency,
			description
		);

		Transaction transaction = restTestClient
			.post().uri("/api/v1/transactions")
			.body(transactionRequest)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
		long transactionId = transaction.getId();

		// Act + Assert.
		restTestClient
			.delete().uri("/api/v1/transactions/" + transactionId)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();
		
		restTestClient
			.get().uri("/api/v1/transactions/" + transactionId)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().isEmpty();
	}
}
