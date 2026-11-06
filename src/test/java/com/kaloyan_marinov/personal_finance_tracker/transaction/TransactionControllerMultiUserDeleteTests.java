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
class TransactionControllerMultiUserDeleteTests {

    @Autowired
    private RestTestClient restTestClient;

    /**
     * Given two distinct users (User 1 and User 2),
     * User 2 should not be able to delete
     * any transaction that refers to (= is owned by) User 1.
     */
    @Test
    void delete_1() {
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

        TransactionRequest transactionRequest1 = new TransactionRequest(
            2006,
            new BigDecimal("-600.00"),
            "EUR",
            "groceries"
        );
        TransactionResponse transactionResponse1 = Utilities.createTransaction(
            restTestClient,
            jdAccessToken,
            transactionRequest1
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/transactions/" + transactionResponse1.id())
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

        String expectedBodyJSONFromGet =
            """
            {
                "id": %d,
                "year": 2006,
                "amount": -600.00,
                "currency": "EUR",
                "description": "groceries"
            }
            """.formatted(
                transactionResponse1.id()
            )
        ;
        restTestClient
            .get().uri("/api/v1/transactions/" + transactionResponse1.id())
            .headers(
                h -> h.setBearerAuth(jdAccessToken)
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBodyJSONFromGet,
                JsonCompareMode.STRICT
            )
        ;
    }
}
