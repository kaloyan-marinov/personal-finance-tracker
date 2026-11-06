package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

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
class ExchangeRateControllerSingleUserDeleteTests {

    @Autowired
    private RestTestClient restTestClient;

    /**
     * DELETEs a rate which does not exist.
     * Expects a 404 response with an empty body.
     */
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

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/exchange-rates/2001/EUR/USD")
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

    /**
     * DELETEs an existing rate.
     * Expects a 204 response, and that a later GET returns a 404.
     */
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2002,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/exchange-rates/2002/EUR/USD")
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
            .get().uri("/api/v1/exchange-rates/2002/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody().isEmpty()
        ;
    }

    /**
     * Creates a rate and then DELETEs it with a lowercase {@code sourceCurrency}.
     * Expects a 400 response, which reports the violated pattern, and that the rate still exists.
     */
    @Test
    void delete_3() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2003,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate
        );

        String inadmissibleSourceCurrency = "eur";

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/exchange-rates/2003/" + inadmissibleSourceCurrency + "/USD")
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
                    "sourceCurrency": "must match \\"^[A-Z]{3}$\\""
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

        restTestClient
            .get().uri("/api/v1/exchange-rates/2003/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
        ;
    }

    /**
     * If a user owns any `ExchangeRate`s,
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2003,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate
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
                "error": "unable to delete your user, because you still have exchange rates"
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
