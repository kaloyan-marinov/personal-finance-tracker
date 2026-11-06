package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import org.junit.jupiter.api.Assertions;

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
class ExchangeRateControllerMultiUserUpdateTests {

    @Autowired
    private RestTestClient restTestClient;

    /**
     * Given two distinct users (User 1 and User 2),
     * User 2 should not be able to update
     * any exchange rate that refers to (= is owned by) User 1.
     */
    @Test
    void update_1() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2004,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            jdAccessToken,
            exchangeRateRequestCreate
        );

        ExchangeRateRequestUpdate updateRequest = new ExchangeRateRequestUpdate(
            new BigDecimal("1.1")
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .put().uri("/api/v1/exchange-rates/" + exchangeRateResponse.year() + "/" + exchangeRateResponse.sourceCurrency() + "/" + exchangeRateResponse.targetCurrency())
            .headers(
                h -> h.setBearerAuth(msAccessToken)
            )
            .body(updateRequest)
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.NOT_FOUND)
            .expectBody().isEmpty()
        ;

        String expectedBodyJSONAfterGet =
            """
            {
                id: %d,
                year: 2004,
                sourceCurrency: "EUR",
                targetCurrency: "USD",
                sourceToTargetFactor: 1.085
            }
            """.formatted(
                exchangeRateResponse.id()
            )
        ;
        restTestClient
            .get().uri("/api/v1/exchange-rates/" + exchangeRateResponse.year() + "/" + exchangeRateResponse.sourceCurrency() + "/" + exchangeRateResponse.targetCurrency())
            .header(
                "Authorization",
                "Bearer " + jdAccessToken
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBodyJSONAfterGet,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Given two distinct users (User 1 and User 2),
     * each of whom owns an exchange rate
     * for the same year and currency pair (but with different factors).
     *
     * When User 1 updates the exchange rate for that year and currency pair,
     * the update should be applied to User 1's exchange rate (and not to User 2's).
     */
    @Test
    void update_2() {
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

        Integer year = 2003;
        String sourceCurrency = "EUR";
        String targetCurrency = "USD";

        ExchangeRateRequestCreate erRequest1 = new ExchangeRateRequestCreate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.100")
        );
        ExchangeRateResponse erResponse1 = Utilities.createExchangeRate(
            restTestClient,
            jdAccessToken,
            erRequest1
        );

        ExchangeRateRequestCreate erRequest2 = new ExchangeRateRequestCreate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.200")
        );
        ExchangeRateResponse erResponse2 = Utilities.createExchangeRate(
            restTestClient,
            msAccessToken,
            erRequest2
        );

        ExchangeRateRequestUpdate erRequestUpdate = new ExchangeRateRequestUpdate(
            new BigDecimal("2.100")
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .put().uri("/api/v1/exchange-rates/" + year + "/" + sourceCurrency + "/" + targetCurrency)
            .headers(
                h -> h.setBearerAuth(jdAccessToken)
            )
            .body(erRequestUpdate)
            .exchange()
        ;

        // Assert.
        ExchangeRateResponse jdObserved = responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        ExchangeRateResponse jdExpected = new ExchangeRateResponse(
            erResponse1.id(),
            year,
            sourceCurrency,
            targetCurrency,
            erRequestUpdate.sourceToTargetFactor()
        );
        Assertions.assertEquals(jdExpected, jdObserved);

        ExchangeRateResponse msObserved = restTestClient
            .get().uri("/api/v1/exchange-rates/" + year + "/" + sourceCurrency + "/" + targetCurrency)
            .headers(
                h -> h.setBearerAuth(msAccessToken)
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        ExchangeRateResponse msExpected = new ExchangeRateResponse(
            erResponse2.id(),
            year,
            sourceCurrency,
            targetCurrency,
            erRequest2.sourceToTargetFactor()
        );
        Assertions.assertEquals(msExpected, msObserved);
    }
}
