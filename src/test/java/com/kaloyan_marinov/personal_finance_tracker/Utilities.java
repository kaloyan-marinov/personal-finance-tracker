package com.kaloyan_marinov.personal_finance_tracker;

import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

import com.kaloyan_marinov.personal_finance_tracker.exchange_rate.ExchangeRateRequestCreate;
import com.kaloyan_marinov.personal_finance_tracker.exchange_rate.ExchangeRateResponse;
import com.kaloyan_marinov.personal_finance_tracker.token.TokenResponse;
import com.kaloyan_marinov.personal_finance_tracker.transaction.TransactionRequest;
import com.kaloyan_marinov.personal_finance_tracker.transaction.TransactionResponse;
import com.kaloyan_marinov.personal_finance_tracker.user.UserRequestCreate;
import com.kaloyan_marinov.personal_finance_tracker.user.UserResponseCreate;

/*
This class follows a common idiom
from the «Effective Java» book by Joshua Bloch.

1. `private` constructor

   If you don't declare any constructor,
   the compiler adds a public no-arg constructor.
   That constructor shows up in Javadoc as part of your public API,
   which can be confusing and/or misleading.

   By explicitly declaring that constructor as `private`,
   one removes a misleading public API
   and prevents meaningless object creation.

2. marking the class as `final`

   (a)
   The preceding point already blocks subclassing from the outside.
   (A subclass constructor must call `super()`
   but the compiler won't allow that,
   due to the constructor in the parent class being `private`.)

   However, it does not block subclassing from the inside.
   In general, every nested class can call the `private` constructor;
   consequently, it is possible for a nested class to extend the encompassing class -
   despite the latter class's constructor being `private`.
   Marking the class as `final` closes that gap.

   (b)
   Extending a utility class makes no sense,
   because static methods aren't polymorphic;
   a subclass can only _hide_ them, not override them,
   and that's a classic source of confusion.

   (c)
   Last but not least,
   `final` states the intent directly,
   thus enhancing clarity.

3. throwing an `AssertionError` from the `private` constructor

   The `private` modifier does not prevent the constructor
   from being called from within this class
   or via reflection.
   Throwing an `AssertionError` guards against both,
   and signals that the call should never happen.
*/
public final class Utilities {

    private Utilities() {
        throw new AssertionError("Not allowed to instantiate this class");
    }

    public static UserResponseCreate createUser(
        RestTestClient restTestClient,
        UserRequestCreate userRequestCreate
    ) {
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/users")
            .body(userRequestCreate)
            .exchange()
        ;

        UserResponseCreate userResponseCreate = responseSpec
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody(UserResponseCreate.class)
            .returnResult()
            .getResponseBody()
        ;

        return userResponseCreate;
    }

    public static String obtainAccessToken(
        RestTestClient restTestClient,
        String email,
        String password
    ) {
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/tokens")
            .headers(
                h -> h.setBasicAuth(
                    email,
                    password
                )
            )
            .exchange()
        ;

        TokenResponse tokenResponse = responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
            .expectBody(TokenResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        return tokenResponse.accessToken();
    }

    public static TransactionResponse createTransaction(
        RestTestClient restTestClient,
        String accessToken,
        TransactionRequest transactionRequest
    ) {
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(transactionRequest)
            .exchange()
        ;

        TransactionResponse transactionResponse = responseSpec
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody(TransactionResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        return transactionResponse;
    }

    public static ExchangeRateResponse createExchangeRate(
        RestTestClient restTestClient,
        String accessToken,
        ExchangeRateRequestCreate exchangeRateRequestCreate
    ) {
        RestTestClient.ResponseSpec responseSpec = restTestClient
            .post().uri("/api/v1/exchange-rates")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .body(exchangeRateRequestCreate)
            .exchange()
        ;

        ExchangeRateResponse exchangeRateResponse = responseSpec
            .expectStatus().isEqualTo(HttpStatus.CREATED)
            .expectBody(ExchangeRateResponse.class)
            .returnResult()
            .getResponseBody()
        ;

        return exchangeRateResponse;
    }

}
