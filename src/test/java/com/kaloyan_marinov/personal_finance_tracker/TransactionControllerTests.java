package com.kaloyan_marinov.personal_finance_tracker;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
class TransactionControllerTests {

	@Autowired 
	private RestTestClient restTestClient;

	@Test
	void create_1() {
		// Arrange.
		Transaction transactionDTO = new Transaction();
		transactionDTO.setYear(2000);

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
			.post().uri("/transactions")
			.body(transactionDTO)
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
		Transaction transactionDTO = new Transaction();
		transactionDTO.setYear(2000);
		transactionDTO.setAmount(new BigDecimal("4.789"));

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
			.post().uri("/transactions")
			.body(transactionDTO)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
	}

	@Test
	void create_3() {
		// Arrange.
		Transaction transactionDTO = new Transaction();
		transactionDTO.setYear(2001);
		transactionDTO.setAmount(new BigDecimal("100.00"));

		// Act.
		Transaction transaction = restTestClient
			.post().uri("/transactions")
			.body(transactionDTO)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
		
		// Assert.
		assertEquals(transactionDTO.getYear(), transaction.getYear());
		assertEquals(transactionDTO.getAmount(), transaction.getAmount());
	}

	@Test
	void retrieveOne_1() {
		// Arrange.
		long nonexistentId = 17;

		// Act + Assert.
		restTestClient
			.get().uri("/transactions/" + nonexistentId)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().isEmpty();
	}

	@Test
	void retrieveOne_2() {
		// Arrange.
		Transaction transactionDTO = new Transaction();
		transactionDTO.setYear(2002);
		transactionDTO.setAmount(new BigDecimal("200.00"));

		Transaction transaction = restTestClient
			.post().uri("/transactions")
			.body(transactionDTO)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
		;
		long transactionId = transaction.getId();

		// Act + Assert.
		String expectedBodyJSON = """
			{id: %d, year: 2002, amount: 200}
		""".formatted(transactionId);

		restTestClient
			.get().uri("/transactions/" + transactionId)
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
			.get().uri("/transactions")
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
		Transaction transactionDTO6 = new Transaction();
		transactionDTO6.setYear(2006);
		transactionDTO6.setAmount(new BigDecimal("600.00"));

		Transaction transactionDTO7 = new Transaction();
		transactionDTO7.setYear(2007);
		transactionDTO7.setAmount(new BigDecimal("700.00"));

		Transaction transaction6 = restTestClient
			.post().uri("/transactions")
			.body(transactionDTO6)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
		
		Transaction transaction7 = restTestClient
			.post().uri("/transactions")
			.body(transactionDTO7)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();

		// Act + Assert.
		String expectedBodyJSON = """
			[
				{id: %d, year: 2006, amount: 600.00},
				{id: %d, year: 2007, amount: 700}
			]
		""".formatted(
			transaction6.getId(),
			transaction7.getId()
		);

		restTestClient
			.get().uri("/transactions")
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
		Transaction transactionDTO = new Transaction();
		transactionDTO.setYear(3000 + 2003);
		transactionDTO.setAmount(
			new BigDecimal(
				String.valueOf(3000 + 300.00)
			)
		);

		Transaction transaction = restTestClient
			.post().uri("/transactions")
			.body(transactionDTO)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
		long transactionId = transaction.getId();

		transactionDTO.setYear(null);
		transactionDTO.setAmount(new BigDecimal("300.00"));

		// Act + Assert.
		String expectedBodyJSON1 = """
			{
				"error": "Validation failed",
				"fieldErrors": {
					"year": "must not be null"
				}
			}
		""".formatted(transactionId);

		restTestClient
			.put().uri("/transactions/" + transactionId)
			.body(transactionDTO)
			.exchange()
			.expectStatus().isBadRequest()
			.expectBody().json(
 				expectedBodyJSON1,
				JsonCompareMode.STRICT
			);
		
		String expectedBodyJSON2 = """
			{id: %d, year: 5003, amount: 3300.00}
		""".formatted(transactionId);

		restTestClient
			.get().uri("/transactions/" + transactionId)
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
		Transaction transactionDTO = new Transaction();
		transactionDTO.setYear(2004);
		transactionDTO.setAmount(new BigDecimal("400.00"));

		long nonexistentId = 17;

		// Arrange + Act.
		restTestClient
			.put().uri("/transactions/" + nonexistentId)
			.body(transactionDTO)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().isEmpty();
	}

	@Test
	void update_3() {
		// Arrange.
		Transaction transactionDTO = new Transaction();
		transactionDTO.setYear(3000 + 2005);
		transactionDTO.setAmount(
			new BigDecimal(
				String.valueOf(3000 + 500.00)
			)
		);

		Transaction transaction = restTestClient
			.post().uri("/transactions")
			.body(transactionDTO)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
		long transactionId = transaction.getId();

		transactionDTO.setYear(2005);
		transactionDTO.setAmount(new BigDecimal("500.00"));

		// Act + Assert.
		String expectedBodyJSON = """
			{id: %d, year: 2005, amount: 500}
		""".formatted(transactionId);

		restTestClient
			.put().uri("/transactions/" + transactionId)
			.body(transactionDTO)
			.exchange()
			.expectStatus().isOk()
			.expectBody().json(
 				expectedBodyJSON,
				JsonCompareMode.STRICT
			);
		
		restTestClient
			.get().uri("/transactions/" + transactionId)
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
			.delete().uri("/transactions/" + nonexistentId)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().isEmpty();
	}

	@Test
	void delete_2() {
		// Arrange.
		Transaction transactionDTO = new Transaction();
		transactionDTO.setYear(2006);
		transactionDTO.setAmount(new BigDecimal("600.00"));

		Transaction transaction = restTestClient
			.post().uri("/transactions")
			.body(transactionDTO)
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Transaction.class)
			.returnResult()
			.getResponseBody();
		long transactionId = transaction.getId();

		// Act + Assert.
		restTestClient
			.delete().uri("/transactions/" + transactionId)
			.exchange()
			.expectStatus().isNoContent()
			.expectBody().isEmpty();
		
		restTestClient
			.get().uri("/transactions/" + transactionId)
			.exchange()
			.expectStatus().isNotFound()
			.expectBody().isEmpty();
	}
}
