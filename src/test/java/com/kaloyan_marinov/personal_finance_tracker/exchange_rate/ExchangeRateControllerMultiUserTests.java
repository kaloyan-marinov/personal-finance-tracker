package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import com.kaloyan_marinov.personal_finance_tracker.token.TokenResponse;
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
class ExchangeRateControllerMultiUserTests {

    @Autowired
    private RestTestClient restTestClient;

    private UserResponseCreate createUser(
        String username,
        String email,
        String password
    ) {
        UserRequestCreate userRequestCreate = new UserRequestCreate(
            username,
            email,
            password
        );
        UserResponseCreate userResponseCreate = restTestClient
            .post().uri("/api/v1/users")
            .body(userRequestCreate)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(UserResponseCreate.class)
            .returnResult()
            .getResponseBody()
        ;

        return userResponseCreate;
    }

    private String obtainAccessToken(
        String email,
        String password
    ) {
        TokenResponse tokenResponse = restTestClient
            .post().uri("/api/v1/tokens")
            .headers(
                h -> h.setBasicAuth(
                    email,
                    password
                )
            )
            .exchange()
            .expectStatus().isOk()
            .expectBody(TokenResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        return tokenResponse.accessToken();
    }

    private ExchangeRateResponse createExchangeRate(
        String accessToken,
        ExchangeRateRequestCreate exchangeRateRequestCreate
    ) {
        return restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestCreate)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody();
    }

    /**
     * Distinct users should be able to create their own exchange rate
     * for the same year and currency pair.
     */
    @Test
    void create_1() {
        // Arrange.
        UserResponseCreate userResponseCreate1 = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessTokenForUser1 = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        UserResponseCreate userResponseCreate2 = createUser(
            "test-" + "ms",
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        String accessTokenForUser2 = obtainAccessToken(
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        int year = 2003;
        String sourceCurrency = "ABC";
        String targetCurrency = "XYZ";

        ExchangeRateRequestCreate erRequestCreate = new ExchangeRateRequestCreate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.234")
        );

        // Act.
        createExchangeRate(
            accessTokenForUser1,
            erRequestCreate
        );

        // Assert.
        createExchangeRate(
            accessTokenForUser2,
            erRequestCreate
        );
    }

    /**
     * Given two distinct users (User 1 and User 2),
     * User 2 should not be able to retrieve
     * any exhchange rate that refers to (= is owned by) User 1.
     */
    @Test
    void retrieveOne_1() {
        // Arrange.
        UserResponseCreate userResponseCreate1 = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessTokenForUser1 = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        UserResponseCreate userResponseCreate2 = createUser(
            "test-" + "ms",
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        String accessTokenForUser2 = obtainAccessToken(
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );
        
        ExchangeRateRequestCreate erRequest = new ExchangeRateRequestCreate(
            2003,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );

        ExchangeRateResponse erResponse = restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessTokenForUser1)
            )
            .body(erRequest)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates/" + erResponse.year() + "/" + erResponse.sourceCurrency() + "/" + erResponse.targetCurrency())
            .headers(
                h -> h.setBearerAuth(accessTokenForUser2)
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
     * 
     */
    @Test
    void retrieveOne_2() {
        // Arrange.
        UserResponseCreate userResponseCreate1 = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessTokenForUser1 = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        UserResponseCreate userResponseCreate2 = createUser(
            "test-" + "ms",
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        String accessTokenForUser2 = obtainAccessToken(
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
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
        ExchangeRateResponse erResponse1 = createExchangeRate(accessTokenForUser1, erRequest1);

        ExchangeRateRequestCreate erRequest2 = new ExchangeRateRequestCreate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.200")
        );
        ExchangeRateResponse erReponse2 = createExchangeRate(accessTokenForUser2, erRequest2);

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates/" + year + "/" + sourceCurrency + "/" + targetCurrency)
            .headers(
                h -> h.setBearerAuth(accessTokenForUser1)
            )
            .exchange()
        ;

        // Assert.
        ExchangeRateResponse observed = responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        ExchangeRateResponse expected = new ExchangeRateResponse(
            erResponse1.id(),
            year,
            sourceCurrency,
            targetCurrency,
            erRequest1.sourceToTargetFactor()
        );
        assertEquals(expected, observed);
    }

    /**
     * Given two distinct users (User 1 and User 2),
     * each of whom has/owns >=1 exchange rate.
     * 
     * When User 2 issues `GET /api/v1/exchange-rates`,
     * the response body should:
     * (a) consist of the exchange rates owned by User 2;
     * (b) not contain any exchange rate owned by User 1.
     */
    @Test
    void retrieveAll_1() {
        // Arrange.
        UserResponseCreate userResponseCreate1 = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonail.com",
            "test-" + "123"
        );

        String accessTokenForUser1 = obtainAccessToken(
            "test-" + "john.doe@protonail.com",
            "test-" + "123"
        );

        UserResponseCreate userResponseCreate2 = createUser(
            "test-" + "ms",
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        String accessTokenForUser2 = obtainAccessToken(
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate1 = new ExchangeRateRequestCreate(
            2001,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse1 = createExchangeRate(
            accessTokenForUser1,
            exchangeRateRequestCreate1
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate2 = new ExchangeRateRequestCreate(
            2002,
            "CHF",
            "EUR",
            new BigDecimal("1.06")
        );
        ExchangeRateResponse exchangeRateResponse2 = createExchangeRate(
            accessTokenForUser2,
            exchangeRateRequestCreate2
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessTokenForUser2)
            )
            .exchange()
        ;

        // Assert.
        String expectedBoyJSON =
            """
            [
                {
                    id: %d,
                    year: 2002,
                    sourceCurrency: "CHF",
                    targetCurrency: "EUR",
                    sourceToTargetFactor: 1.06
                }
            ]            
            """.formatted(exchangeRateResponse2.id())
        ;
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBoyJSON
            )
        ;
    }

    /**
     * Given two distinct users (User 1 and User 2),
     * User 2 should not be able to update
     * any exchange rate that refers to (= is owned by) User 1.
     */
    @Test
    void update_1() {
        // Arrange.
        UserResponseCreate userResponseCreate1 = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessTokenForUser1 = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        UserResponseCreate userResponseCreate2 = createUser(
            "test-" + "ms",
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        String accessTokenForUser2 = obtainAccessToken(
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2004,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = createExchangeRate(
            accessTokenForUser1,
            exchangeRateRequestCreate
        );

        ExchangeRateRequestUpdate updateRequest = new ExchangeRateRequestUpdate(
            new BigDecimal("1.1")
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .put().uri("/api/v1/exchange-rates/" + exchangeRateResponse.year() + "/" + exchangeRateResponse.sourceCurrency() + "/" + exchangeRateResponse.targetCurrency())
            .headers(
                h -> h.setBearerAuth(accessTokenForUser2)
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
            """.formatted(exchangeRateResponse.id())
        ;
        restTestClient
            .get().uri("/api/v1/exchange-rates/" + exchangeRateResponse.year() + "/" + exchangeRateResponse.sourceCurrency() + "/" + exchangeRateResponse.targetCurrency())
            .header(
                "Authorization",
                "Bearer " + accessTokenForUser1
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
     * 
     */
    @Test
    void update_2() {
        // Arrange.
        UserResponseCreate userResponseCreate1 = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessTokenForUser1 = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        UserResponseCreate userResponseCreate2 = createUser(
            "test-" + "ms",
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        String accessTokenForUser2 = obtainAccessToken(
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
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
        ExchangeRateResponse erResponse1 = createExchangeRate(accessTokenForUser1, erRequest1);

        ExchangeRateRequestCreate erRequest2 = new ExchangeRateRequestCreate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.200")
        );
        ExchangeRateResponse erResponse2 = createExchangeRate(accessTokenForUser2, erRequest2);

        ExchangeRateRequestUpdate erRequestUpdate = new ExchangeRateRequestUpdate(
            new BigDecimal("2.100")
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .put().uri("/api/v1/exchange-rates/" + year + "/" + sourceCurrency + "/" + targetCurrency)
            .headers(
                h -> h.setBearerAuth(accessTokenForUser1)
            )
            .body(erRequestUpdate)
            .exchange()
        ;

        // Assert.
        ExchangeRateResponse observed = responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        ExchangeRateResponse expected = new ExchangeRateResponse(
            erResponse1.id(),
            year,
            sourceCurrency,
            targetCurrency,
            erRequestUpdate.sourceToTargetFactor()
        );
        assertEquals(expected, observed);
    }

    /**
     * Given two distinct users (User 1 and User 2),
     * User 2 should not be able to delete
     * any exchange rate that refers to (= is owned by) User 1.
     */
    @Test
    void delete_1() {
        // Arrange.
        UserResponseCreate userResponseCreate1 = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessTokenForUser1 = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        UserResponseCreate userResponseCreate2 = createUser(
            "test-" + "ms",
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        String accessTokenForUser2 = obtainAccessToken(
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate = new ExchangeRateRequestCreate(
            2003,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse = createExchangeRate(accessTokenForUser1, exchangeRateRequestCreate);

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/exchange-rates/" + exchangeRateResponse.year() + "/" + exchangeRateResponse.sourceCurrency() + "/" + exchangeRateResponse.targetCurrency())
            .headers(
                h -> h.setBearerAuth(accessTokenForUser2)
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
            """.formatted(exchangeRateResponse.id())
        ;
        restTestClient
            .get().uri("/api/v1/exchange-rates/" + exchangeRateResponse.year() + "/" + exchangeRateResponse.sourceCurrency() + "/" + exchangeRateResponse.targetCurrency())
            .headers(
                h -> h.setBearerAuth(accessTokenForUser1)
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
     * 
     */
    @Test
    void delete_2() {
        // Arrange.
        UserResponseCreate userResponseCreate1 = createUser(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        String accessTokenForUser1 = obtainAccessToken(
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );

        UserResponseCreate userResponseCreate2 = createUser(
            "test-" + "ms",
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
        );

        String accessTokenForUser2 = obtainAccessToken(
            "test-" + "mary.smith@protonmail.com",
            "test-" + "456"
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
        ExchangeRateResponse erResponse1 = createExchangeRate(accessTokenForUser1, erRequest1);

        ExchangeRateRequestCreate erRequest2 = new ExchangeRateRequestCreate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.200")
        );
        ExchangeRateResponse erResponse2 = createExchangeRate(accessTokenForUser2, erRequest2);

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .delete().uri("/api/v1/exchange-rates/" + year + "/" + sourceCurrency + "/" + targetCurrency)
            .headers(
                h -> h.setBearerAuth(accessTokenForUser1)
            )
            .exchange()
        ;

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.NO_CONTENT)
            .expectBody().isEmpty()
        ;
    }
}