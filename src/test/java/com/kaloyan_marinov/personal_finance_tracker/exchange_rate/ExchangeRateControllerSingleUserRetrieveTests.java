package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;

import com.kaloyan_marinov.personal_finance_tracker.Utilities;
import com.kaloyan_marinov.personal_finance_tracker.common.ConstantsForPagination;
import com.kaloyan_marinov.personal_finance_tracker.common.PaginatedResponse;
import com.kaloyan_marinov.personal_finance_tracker.common.PaginationInfo;
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
class ExchangeRateControllerSingleUserRetrieveTests {

    @Autowired
    private RestTestClient restTestClient;

    /**
     * GETs a rate which does not exist.
     * Expects a 404 response with an empty body.
     */
    @Test
    void retrieveOne_1() {
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
            .get().uri("/api/v1/exchange-rates/2001/EUR/USD")
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
     * GETs a rate with a {@code year} which is not an integer.
     * Expects a 400 response, which reports the type mismatch.
     */
    @Test
    void retrieveOne_2() {
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

        String inadmissibleYear = "abc";

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates/" + inadmissibleYear + "/EUR/USD")
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
                    "year": "must be of type Integer"
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
    }

    /**
     * Creates a rate
     * and then attempts to GET it with a lowercase {@code sourceCurrency}.
     * Expects a 400 response, which reports the violated pattern.
     */
    @Test
    void retrieveOne_3() {
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
            .get().uri("/api/v1/exchange-rates/2003/" + inadmissibleSourceCurrency + "/USD")
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
    }

    /**
     * This tests the happy path.
     */
    @Test
    void retrieveOne_4() {
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
            .get().uri("/api/v1/exchange-rates/2003/EUR/USD")
            .headers(
                h -> h.setBearerAuth(accessToken)
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
            exchangeRateResponse.id(),
            exchangeRateRequestCreate.year(),
            exchangeRateRequestCreate.sourceCurrency(),
            exchangeRateRequestCreate.targetCurrency(),
            exchangeRateRequestCreate.sourceToTargetFactor()
        );
        Assertions.assertEquals(expected, observed);
    }

    /**
     * GETs all rates when none exist.
     * Expects a 200 response with an empty list of resources.
     */
    @Test
    void retrieveAll_01() {
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
            .get().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                resources: [],
                paginationInfo: {
                    total: 0,
                    pageNumber: 0,
                    pageSize: 10
                }
            }
            """
        ;
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Creates two rates and then GETs all rates.
     * Expects a 200 response with both rates, in the order in which they were created.
     */
    @Test
    void retrieveAll_02() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate1 = new ExchangeRateRequestCreate(
            2001,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse1 = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate1
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate2 = new ExchangeRateRequestCreate(
            2002,
            "CHF",
            "EUR",
            new BigDecimal("1.06")
        );
        ExchangeRateResponse exchangeRateResponse2 = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate2
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                resources: [
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
                ],
                paginationInfo: {
                    total: 2,
                    pageNumber: 0,
                    pageSize: 10
                }
            }
            """.formatted(
                exchangeRateResponse1.id(),
                exchangeRateResponse2.id()
            )
        ;

        responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Create 11 or more rows, send no params, expect 10 rows with `size: 10`
     */
    @Test
    void retrieveAll_03() {
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

        int expectedTotal = ConstantsForPagination.PAGE_SIZE_DEFAULT + 1;
        for (int i = 0; i < expectedTotal; i++) {
            ExchangeRateRequestCreate erReq = new ExchangeRateRequestCreate(
                2000 + i,
                "ABC",
                "XYZ",
                new BigDecimal("1.500")
            );

            ExchangeRateResponse erResp = Utilities.createExchangeRate(
                restTestClient,
                accessToken,
                erReq
            );
        }

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        PaginatedResponse<ExchangeRateResponse> paginatedResponse = responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(
                new TypeReferenceForPaginatedResponseWithExchangeRates()
            )
            .returnResult()
            .getResponseBody()
        ;

        int sizeExpected = ConstantsForPagination.PAGE_SIZE_DEFAULT;
        int sizeObserved = paginatedResponse.resources().size();
        Assertions.assertEquals(
            sizeExpected,
            sizeObserved
        );

        PaginationInfo paginationInfoExpected = new PaginationInfo(
            expectedTotal,
            ConstantsForPagination.PAGE_NUMBER_DEFAULT,
            ConstantsForPagination.PAGE_SIZE_DEFAULT
        );
        PaginationInfo paginationInfoObserved = paginatedResponse.paginationInfo();
        Assertions.assertEquals(
            paginationInfoExpected,
            paginationInfoObserved
        );
    }

    /*
    Java doesn't let you put type arguments on a class literal,
    preventing
    ```
    .expectBody(PaginatedResponse<ExchangeRateResponse>.class)
    ```
    from compiling.

    Generics are erased at compile time,
    causing type arguments to be missing at runtime;
    in this concrete case:
    at runtime, there is only one `PaginatedResponse.class`
    (which implies that
    a `PaginatedResponse<ExchangeRateResponse>.class` doesn't exist at runtime).
    If the preceding line were replaced with
    ```
    .expectBody(PaginatedResponse.class)
    ```
    then the returned value would be a `PaginatedResponse` (= a raw type, without a type argument)
    and each item in `resources` would come back as a `LinkedHashMap`
    instead of a `ExchangeRateResponse` (b/c Jackson wouldn't know what `T` is).

    A common way of mitigating that deficiency consists in
    ```
    .expectBody(new X())
    ```
    where `X` is an empty-body (= identical-implementation) subclass
    of `ParameterizedTypeReference<GenericType<A, B, ...>>`.
    Though seemingly complex,
    that causes the subclass's `extends ParameterizedTypeReference<GenericType<A, B, ...>>` clause to be kept in the bytecode,
    which in turn enables Spring to read `GenericType<A, B, ...>` back at runtime.
    */
    private static class TypeReferenceForPaginatedResponseWithExchangeRates extends ParameterizedTypeReference<PaginatedResponse<ExchangeRateResponse>> {
    }

    /**
     * A `size` that is not an integer
     * should be rejected with a 400.
     */
    @Test
    void retrieveAll_04() {
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

        String invalidPageSize = "abc";
        
        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates?pageSize=" + invalidPageSize)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedJSONBody =
            """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "pageSize": "must be of type int"
                }
            }
            """
        ;
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody().json(
                expectedJSONBody,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * A `size` less than the min admissible value
     * should be rejected with a 400.
     */
    @Test
    void retrieveAll_05() {
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

        Integer invalidPageSize = ConstantsForPagination.PAGE_SIZE_MIN - 1;
        
        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates?pageSize=" + invalidPageSize)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedJSONBody =
            """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "pageSize": "must be greater than or equal to 1"
                }
            }
            """
        ;
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody().json(
                expectedJSONBody,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * A `size` greater than the max admissible value
     * should be rejected with a 400.
     */
    @Test
    void retrieveAll_06() {
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

        Integer invalidPageSize = ConstantsForPagination.PAGE_SIZE_MAX + 1;
        
        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates?pageSize=" + invalidPageSize)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedJSONBody =
            """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "pageSize": "must be less than or equal to 100"
                }
            }
            """
        ;
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody().json(
                expectedJSONBody,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * A page size of `ConstantsForPagination.PAGE_SIZE_MAX` should be accepted.
     */
    @Test
    void retrieveAll_07() {
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

        ExchangeRateRequestCreate exchangeRateRequestCreate1 = new ExchangeRateRequestCreate(
            2001,
            "EUR",
            "USD",
            new BigDecimal("1.085")
        );
        ExchangeRateResponse exchangeRateResponse1 = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate1
        );

        ExchangeRateRequestCreate exchangeRateRequestCreate2 = new ExchangeRateRequestCreate(
            2002,
            "CHF",
            "EUR",
            new BigDecimal("1.06")
        );
        ExchangeRateResponse exchangeRateResponse2 = Utilities.createExchangeRate(
            restTestClient,
            accessToken,
            exchangeRateRequestCreate2
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates?pageSize=" + ConstantsForPagination.PAGE_SIZE_MAX)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedBodyJSON =
            """
            {
                resources: [
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
                ],
                paginationInfo: {
                    total: 2,
                    pageNumber: 0,
                    pageSize: 100
                }
            }
            """.formatted(
                exchangeRateResponse1.id(),
                exchangeRateResponse2.id()
            )
        ;

        responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedBodyJSON,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * A `page` less than the min admissible value
     * should be rejected with a 400.
     */
    @Test
    void retrieveAll_08() {
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

        Integer invalidPageNumber = ConstantsForPagination.PAGE_NUMBER_MIN - 1;
        
        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates?pageNumber=" + invalidPageNumber)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedJSONBody =
            """
            {
                "error": "Validation failed",
                "fieldErrors": {
                    "pageNumber": "must be greater than or equal to 0"
                }
            }
            """
        ;
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody().json(
                expectedJSONBody,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Requesting a page beyond the last one
     * should return `resources: []` and the correct `total`.
     */
    @Test
    void retrieveAll_09() {
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

        int expectedTotal = ConstantsForPagination.PAGE_SIZE_DEFAULT + 1;
        for (int i = 0; i < expectedTotal; i++) {
            ExchangeRateRequestCreate erReq = new ExchangeRateRequestCreate(
                2000 + i,
                "ABC",
                "XYZ",
                new BigDecimal("1.500")
            );

            ExchangeRateResponse erResp = Utilities.createExchangeRate(
                restTestClient,
                accessToken,
                erReq
            );
        }

        int lastPageNumber = (expectedTotal - 1) / ConstantsForPagination.PAGE_SIZE_DEFAULT;
        int pageNumber = lastPageNumber + 1;

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates?pageNumber=" + pageNumber)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        String expectedJSONBody = 
            """
            {
                resources: [],
                paginationInfo: {
                    total: %d,
                    pageNumber: %d,
                    pageSize: %d
                }
            }
            """.formatted(
                expectedTotal,
                pageNumber,
                ConstantsForPagination.PAGE_SIZE_DEFAULT
            )
        ;

        responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody().json(
                expectedJSONBody,
                JsonCompareMode.STRICT
            )
        ;
    }

    /**
     * Given a `User` who has 11 `ExchangeRate`s,
     * requesting the "middle" page characterized by `pageSize=3&pageNumber=1`
     * should return the relevant list of that `User`'s `ExchangeRate`s (in `id` order).
     */
    @Test
    void retrieveAll_10() {
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

        int expectedTotal = ConstantsForPagination.PAGE_SIZE_DEFAULT + 1;
        List<ExchangeRateResponse> exchangeRateResponses = new ArrayList<>();
        for (int i = 0; i < expectedTotal; i++) {
            ExchangeRateRequestCreate erReq = new ExchangeRateRequestCreate(
                2000 + i,
                "ABC",
                "XYZ",
                new BigDecimal("1.500")
            );

            ExchangeRateResponse erResp = Utilities.createExchangeRate(
                restTestClient,
                accessToken,
                erReq
            );
            exchangeRateResponses.add(erResp);
        }

        int pageSize = 3;
        int pageNumber = 1;

        // Act.
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .get().uri("/api/v1/exchange-rates?pageSize=" + pageSize + "&pageNumber=" + pageNumber)
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;

        // Assert.
        PaginatedResponse<ExchangeRateResponse> observed = responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(
                new TypeReferenceForPaginatedResponseWithExchangeRates()
            )
            .returnResult()
            .getResponseBody()
        ;

        int fromIndex = pageNumber * pageSize;
        int toIndex = fromIndex + pageSize;
        List<ExchangeRateResponse> expectedResources = exchangeRateResponses.subList(
            fromIndex,
            toIndex
        );
        List<ExchangeRateResponse> observedResources = observed.resources();
        Assertions.assertEquals(
            expectedResources,
            observedResources
        );

        PaginationInfo expectedPaginationInfo = new PaginationInfo(
            expectedTotal,
            pageNumber,
            pageSize
        );
        PaginationInfo observedPaginationInfo = observed.paginationInfo();
        Assertions.assertEquals(
            expectedPaginationInfo,
            observedPaginationInfo
        );
    }
}
