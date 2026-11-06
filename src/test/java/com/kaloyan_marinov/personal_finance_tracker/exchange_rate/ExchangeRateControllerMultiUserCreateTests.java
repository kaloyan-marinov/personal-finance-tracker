package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
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
class ExchangeRateControllerMultiUserCreateTests {

    @Autowired
    private RestTestClient restTestClient;

    /**
     * Distinct users should be able to create their own exchange rate
     * for the same year and currency pair.
     */
    @Test
    void create_1() {
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

        ExchangeRateRequestCreate erRequestCreate = new ExchangeRateRequestCreate(
            2003,
            "ABC",
            "XYZ",
            new BigDecimal("1.234")
        );

        Utilities.createExchangeRate(
            restTestClient,
            jdAccessToken,
            erRequestCreate
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(msAccessToken)
            )
            .body(erRequestCreate)
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.CREATED)
        ;
    }
}
