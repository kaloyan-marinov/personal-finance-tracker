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
class ExchangeRateControllerMultiUserDeleteTests {

    @Autowired
    private RestTestClient restTestClient;

    /**
     * Given two distinct users (User 1 and User 2),
     * User 2 should not be able to delete
     * any exchange rate that refers to (= is owned by) User 1.
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

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2003,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = Utilities.createExchangeRate(
            restTestClient,
            jdAccessToken,
            exchangeRateRequestCreate
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/exchange-rates/" + exchangeRateResponse.year() + "/" + exchangeRateResponse.sourceCurrency() + "/" + exchangeRateResponse.targetCurrency())
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
                "year": 2003,
                "sourceCurrency": "EUR",
                "targetCurrency": "USD",
                "sourceToTargetFactor": 1.085
            }
            """.formatted(
                exchangeRateResponse.id()
            )
        ;
        restTestClient
            .get().uri("/api/v1/exchange-rates/" + exchangeRateResponse.year() + "/" + exchangeRateResponse.sourceCurrency() + "/" + exchangeRateResponse.targetCurrency())
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

    /**
     * Given two distinct users (User 1 and User 2),
     * each of whom owns an exchange rate
     * for the same year and currency pair (but with different factors).
     *
     * User 1 should be able to delete their own exchange rate
     * for that year and currency pair,
     * even though User 2 owns an exchange rate for the same year and currency pair.
     */
    @Test
    void delete_2() {
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

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/exchange-rates/" + year + "/" + sourceCurrency + "/" + targetCurrency)
            .headers(
                h -> h.setBearerAuth(jdAccessToken)
            )
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.NO_CONTENT)
            .expectBody().isEmpty()
        ;

        String expectedBodyJSON = 
            """
            {
                "id": %d,
                "year": 2003,
                "sourceCurrency": "EUR",
                "targetCurrency": "USD",
                "sourceToTargetFactor": 1.200
            }    
            """.formatted(
                erResponse2.id()
            )
        ;

        restTestClient
            .get().uri("/api/v1/exchange-rates/" + year + "/" + sourceCurrency + "/" + targetCurrency)
            .headers(
                h -> h.setBearerAuth(msAccessToken)
            )
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }
}
