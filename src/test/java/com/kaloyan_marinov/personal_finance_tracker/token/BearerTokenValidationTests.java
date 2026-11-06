package com.kaloyan_marinov.personal_finance_tracker.token;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

/*
Notice that
```java
/// src/main/java/com/kaloyan_marinov/personal_finance_tracker/token/TokenController.java

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

// ...
```
differ from the following imports.

The two are layers of the same stack:
«Spring Security»'s types are a library-agnostic abstraction,
and `NimbusJwtEncoder` translates them into Nimbus' types
(which is why Nimbus is always on the classpath via `spring-security-oauth2-jose`).

- `TokenController` issues tokens through the application's `JwtEncoder` bean,
  so it must speak Spring's API (= must use «Spring Security»'s types).

- This file's class builds (deliberately bad) tokens that the application should never issue,
  and uses Nimbus directly for the following reasons:

  1. An unsigned JWT (`"alg": "none"`) cannot be produced via «Spring Security»'s `JwtEncoder`,
     which only produces signed JWTs;
     Nimbus' `PlainJWT` makes that straightforward.

  2. A JWT signed with another algorithm or another secret
     could be produced via a second `NimbusJwtEncoder`
     (built over an `ImmutableSecret` and given a different `MacAlgorithm`),
     but calling `MACSigner` directly is shorter
     and does not rely on the application's encoder setup/configuration.

  3. If this file's test cases created/minted/generated/built tokens
     with the same Spring setup/configuration that the application uses itself,
     a bug/defect in shared setup/configuration could hide itself.

     Building tokens with a separate tool (= independently of the production code)
     means that
     the application's validation is checked against a separate implementation
     (= something that the application doesn't share code with).

Since (1) requires Nimbus anyway,
using it for all test cases keeps this file's class consistent.
*/
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;

@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:test",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.mysql.sql",
        "pft.security.secret.key=" + BearerTokenValidationTests.SECRET
    }
)
@AutoConfigureRestTestClient
/*
The following annotation is required,
because it creates a `MockMvc` bean that has the security filter chain,
and then the `RestTestClient` binds to the created `MockMvc` bean.
*/
@AutoConfigureMockMvc
class BearerTokenValidationTests {

    static final String SECRET = "secret-value-that-must-be-of-at-least-256-bits-aka-32-bytes";

    @Autowired
    private RestTestClient restTestClient;

    private static JWTClaimsSet buildJWTClaimsSet(Instant instantOfExpiration) {
        return new JWTClaimsSet.Builder()
            .subject("application-user-for-test-suite-u")
            .claim("userId", 1)
            .expirationTime(
                Date.from(instantOfExpiration)
            )
            .build()
        ;
    }

    private static String createAccessToken(
        JWSHeader jwsHeader,
        JWTClaimsSet jwtClaimsSet,
        String secret
    ) throws JOSEException {
        SignedJWT signedJWT = new SignedJWT(
            jwsHeader,
            jwtClaimsSet
        );

        MACSigner messageAuthenticationCodeSigner = new MACSigner(
            secret.getBytes(StandardCharsets.UTF_8)
        );
        signedJWT.sign(messageAuthenticationCodeSigner);

        String accessToken = signedJWT.serialize();
        return accessToken;
    }

    private RestTestClient.ResponseSpec retrieveAllTransactions(String accessToken) {
        return restTestClient
            .get().uri("/api/v1/transactions")
            .headers(
                h -> h.setBearerAuth(accessToken)
            )
            .exchange()
        ;
    }

    /**
     * This test case serves as «a positive control».
     *
     * It shows that a token built by this class's helper methods is accepted,
     * so the remaining test cases fail for the reason they name
     * (and not because of a mistake in the helper methods).
     */
    @Test
    void retrieveAll_0() throws JOSEException {
        // Arrange.
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet jwtClaimsSet = buildJWTClaimsSet(
            Instant.now().plus(Duration.ofHours(1))
        );

        String accessToken = createAccessToken(
            jwsHeader,
            jwtClaimsSet,
            SECRET
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = retrieveAllTransactions(accessToken);

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.OK)
        ;
    }

    /**
     * An unsigned JWT (`"alg": "none"`) should be rejected.
     */
    @Test
    void retrieveAll_1() {
        // Arrange.
        JWTClaimsSet jwtClaimsSet = buildJWTClaimsSet(
            Instant.now().plus(Duration.ofHours(1))
        );

        PlainJWT plainJWT = new PlainJWT(jwtClaimsSet);
        String unsignedJWT = plainJWT.serialize();

        // Act.
        RestTestClient.ResponseSpec responseSpec = retrieveAllTransactions(unsignedJWT);

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
        ;
    }

    /**
     * A JWT signed with the correct secret but with an algorithm other than HS256
     * should be rejected.
     */
    @Test
    void retrieveAll_2() throws JOSEException {
        // Arrange.
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS384);

        JWTClaimsSet jwtClaimsSet = buildJWTClaimsSet(
            Instant.now().plus(Duration.ofHours(1))
        );

        String accessToken = createAccessToken(
            jwsHeader,
            jwtClaimsSet,
            SECRET
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = retrieveAllTransactions(accessToken);

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
        ;
    }

    /**
     * A JWT signed with HS256 but with a different secret
     * (i.e. a JWT not issued by this application) should be rejected.
     */
    @Test
    void retrieveAll_3() throws JOSEException {
        // Arrange.
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet jwtClaimsSet = buildJWTClaimsSet(
            Instant.now().plus(Duration.ofHours(1))
        );

        String accessToken = createAccessToken(
            jwsHeader,
            jwtClaimsSet,
            "a-different-secret-that-is-also-at-least-32-bytes-long"
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = retrieveAllTransactions(accessToken);

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
        ;
    }

    /**
     * An expired JWT should be rejected.
     *
     * «Spring Security» tolerates a clock skew of 60 seconds by default,
     * so the expiration lies well beyond that.
     */
    @Test
    void retrieveAll_4() throws JOSEException {
        // Arrange.
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet jwtClaimsSet = buildJWTClaimsSet(
            Instant.now().minus(Duration.ofHours(1))
        );

        String accessToken = createAccessToken(
            jwsHeader,
            jwtClaimsSet,
            SECRET
        );

        // Act.
        RestTestClient.ResponseSpec responseSpec = retrieveAllTransactions(accessToken);

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
        ;
    }

    /**
     * A JWT whose payload was changed after signing should be rejected.
     */
    @Test
    void retrieveAll_5() throws JOSEException {
        // Arrange.
        JWSHeader jwsHeader_1 = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet jwtClaimsSet_1 = buildJWTClaimsSet(
            Instant.now().plus(Duration.ofHours(1))
        );
        String accessToken_1 = createAccessToken(
            jwsHeader_1,
            jwtClaimsSet_1,
            SECRET
        );
        String[] parts_1 = accessToken_1.split("\\.");
        String header_1 = parts_1[0];
        String signature_1 = parts_1[2];

        JWTClaimsSet jwtClaimsSet_365 = buildJWTClaimsSet(
            Instant.now().plus(Duration.ofDays(365))
        );
        String payload_365 = Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                jwtClaimsSet_365
                    .toString()
                    .getBytes(StandardCharsets.UTF_8)
            );

        String accessToken = header_1 + "." + payload_365 + "." + signature_1;

        // Act.
        RestTestClient.ResponseSpec responseSpec = retrieveAllTransactions(accessToken);

        // Assert.
        responseSpec
            .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED)
        ;
    }

}
