package com.kaloyan_marinov.personal_finance_tracker.transaction;

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
class TransactionControllerSingleUserDeleteTests {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void delete_1() {
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

        long nonexistentId = 17;

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/transactions/" + nonexistentId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody().isEmpty()
        ;
    }

    @Test
    void delete_2() {
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

        TransactionRequest transactionRequest = new TransactionRequest(
            2006,
            new BigDecimal("-600.00"),
            "EUR",
            "groceries"
        );

        TransactionResponse transactionResponse = Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequest
        );
        long transactionId = transactionResponse.id();

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.NO_CONTENT)
            .expectBody().isEmpty()
        ;

        restTestClient
            .get().uri("/api/v1/transactions/" + transactionId)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody().isEmpty()
        ;
    }

    /*
    Q:
    Why does this test case fail with
    `Status expected:<409 CONFLICT> but was:<204 NO_CONTENT>`
    instead of a 500
    (given that `fk_transactions_user_id` should reject the `DELETE`)?

    A:
    Because the `DELETE` statement never reaches the database.

    1. This test class is annotated with `@Transactional`,
       and `RestTestClient` is bound to `MockMvc`,
       which runs the controller on the test's own thread.
       So `UserController.delete` joins the test's transaction
       instead of getting its own.

    2. `userRepository.delete(...)` calls `EntityManager.remove(...)`,
       which only marks the entity as removed;
       Hibernate saves the `DELETE` for the next flush.

       (
       The `INSERT`s, on the other hand, are executed immediately,
       because `GenerationType.IDENTITY` requires Hibernate
       to execute each `INSERT` in order to obtain the generated id.
       )

    3. Nothing flushes after the removal:
       the repository's `@Transactional` merely joins the existing transaction
       (so it doesn't commit),
       and no later query touches `users`
       (so auto-flush never runs).
       The controller returns 204.

    4. When the test method ends, Spring rolls back the transaction,
       which discards the pending `DELETE` without ever executing it.
       So the foreign key is never checked.

    In production, there is no surrounding transaction:
    the repository's own transaction commits,
    the flush executes the `DELETE`,
    MySQL rejects it,
    and (because nothing catches that exception) the client gets a 500.

    Possible fixes:
    - (recommended)
      before deleting, check for owned rows
      (e.g. `transactionRepository.existsByUser(user)`, and similarly for exchange rates)
      and return 409 if any exist;
      this doesn't depend on when Hibernate flushes;
    - call `userRepository.flush()` right after `delete`,
      inside a `try` that catches `DataIntegrityViolationException` and returns 409;
    - remove `@Transactional` from this test class
      (which requires cleaning up data between tests manually).
    */
    /**
     * If a user owns any `Transaction`s,
     * an attempt to delete that user should be refused with a 409.
     */
    @Test
    void delete_user() {
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

        TransactionRequest transactionRequest1 = new TransactionRequest(
            2006,
            new BigDecimal("-600.00"),
            "EUR",
            "groceries"
        );
        TransactionResponse transactionResponse1 = Utilities.createTransaction(
            restTestClient,
            accessToken,
            transactionRequest1
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/users/" + userResponseCreate.id())
            .headers(
                h -> h.setBasicAuth(
                    userRequestCreate.email(),
                    userRequestCreate.password()
                )
            )
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                "error": "unable to delete your user, because you still have transactions"
            }
            """
        ;
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }
}
