package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kaloyan_marinov.personal_finance_tracker.user.UserRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

// An exchange rate is addressed by its natural key, `{year}/{sourceCurrency}/{targetCurrency}`,
// which plays the role that `{id}` plays for transactions.
//
// The `sourceCurrency` and `targetCurrency` path variables
// must match the same `@Pattern` as
// the corresponding fields of the `@RequestBody`-annotated arguments;
// the reason why this extra caution is worthwhile is as follows:
// - `eur` would match no row on an H2 database;
// - `eur` could match `EUR` on a MySQL database
//   (if that database has been created with case-insensitive collation);
// consequently,
// validating the values up-front makes the result independent of the database.
@RestController
@RequestMapping("/api/v1/exchange-rates")
public class ExchangeRateController {

    private ExchangeRateRepository exchangeRateRepository;
    private UserRepository userRepository;

    public ExchangeRateController(
        ExchangeRateRepository exchangeRateRepository,
        UserRepository userRepository
    ) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.userRepository = userRepository;
    }

    // The return type is `ResponseEntity<?>`,
    // because the body is either the created `ExchangeRateResponse` or an error.
    @PostMapping("")
    public ResponseEntity<?> create(
        @Valid @RequestBody ExchangeRateRequestCreate exchangeRateRequestCreate,
        Authentication authentication
    ) {
        /*
        An alternative implementation would be
        to replace the `Authentication authentication` parameter with `@AuthenticationPrincipal Jwt jwt`
        and to replace the following 6 lines with
        `Long userId = jwt.getClaim("userId");`.

        That works because,
        after a successful Bearer authentication,
        «Spring Security» stores a `JwtAuthenticationToken` whose principal is the `Jwt` itself,
        and `@AuthenticationPrincipal` resolves the parameter from that principal.
        */
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new IllegalStateException("expected a JWT-based authentication");
        }
        JwtAuthenticationToken jwtAuthToken = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthToken.getToken();
        Long userId = jwt.getClaim("userId");

        if (!userRepository.existsById(userId)) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                    Map.of(
                        "error", "unauthorized to create an exchange rate"
                    )
                )
            ;
        }

        // `uq_exchange_rates_user_id_year_currencies` remains the guarantee
        // (e.g. for writes that bypass the endpoints);
        // this check merely gives the client a 409 rather than a database error.
        boolean exists = exchangeRateRepository.existsByUserIdAndYearAndSourceCurrencyAndTargetCurrency(
            userId,
            exchangeRateRequestCreate.year(),
            exchangeRateRequestCreate.sourceCurrency(),
            exchangeRateRequestCreate.targetCurrency()
        );

        if (exists) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                    Map.of(
                        "error", "Exchange rate already exists"
                    )
                )
            ;
        }

        ExchangeRate er = new ExchangeRate(
            exchangeRateRequestCreate.year(),
            exchangeRateRequestCreate.sourceCurrency(),
            exchangeRateRequestCreate.targetCurrency(),
            exchangeRateRequestCreate.sourceToTargetFactor()
        );
        er.setUser(
            /*
            (The user's existence is checked separately above, via `existsById`.)

            The following returns a proxy that holds only the ID, without running a SELECT;
            that's all Hibernate needs to write `user_id`.
            */
            userRepository.getReferenceById(userId)
        );
        exchangeRateRepository.save(er);

        ExchangeRateResponse erResponse = new ExchangeRateResponse(
            er.getId(),
            er.getYear(),
            er.getSourceCurrency(),
            er.getTargetCurrency(),
            er.getSourceToTargetFactor()
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(erResponse)
        ;
    }

    @GetMapping("/{year}/{sourceCurrency}/{targetCurrency}")
    public ResponseEntity<ExchangeRateResponse> retrieveOne(
        @PathVariable Integer year,
        @PathVariable @Pattern(regexp = "^[A-Z]{3}$") String sourceCurrency,
        @PathVariable @Pattern(regexp = "^[A-Z]{3}$") String targetCurrency,
        Authentication authentication
    ) {
        /*
        An alternative implementation would be
        to replace the `Authentication authentication` parameter with `@AuthenticationPrincipal Jwt jwt`
        and to replace the following 6 lines with
        `Long userId = jwt.getClaim("userId");`.

        That works because,
        after a successful Bearer authentication,
        «Spring Security» stores a `JwtAuthenticationToken` whose principal is the `Jwt` itself,
        and `@AuthenticationPrincipal` resolves the parameter from that principal.
        */
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new IllegalStateException("expected a JWT-based authentication");
        }
        JwtAuthenticationToken jwtAuthToken = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthToken.getToken();
        Long userId = jwt.getClaim("userId");

        Optional<ExchangeRate> optionalExchangeRate = exchangeRateRepository
            .findByUserIdAndYearAndSourceCurrencyAndTargetCurrency(userId, year, sourceCurrency, targetCurrency);

        if (optionalExchangeRate.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        ExchangeRate er = optionalExchangeRate.get();

        ExchangeRateResponse erResponse = new ExchangeRateResponse(
            er.getId(),
            er.getYear(),
            er.getSourceCurrency(),
            er.getTargetCurrency(),
            er.getSourceToTargetFactor()
        );
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(erResponse)
        ;
    }

    @GetMapping("")
    public ResponseEntity<List<ExchangeRateResponse>> retrieveAll(
        Authentication authentication
    ) {
        /*
        An alternative implementation would be
        to replace the `Authentication authentication` parameter with `@AuthenticationPrincipal Jwt jwt`
        and to replace the following 6 lines with
        `Long userId = jwt.getClaim("userId");`.

        That works because,
        after a successful Bearer authentication,
        «Spring Security» stores a `JwtAuthenticationToken` whose principal is the `Jwt` itself,
        and `@AuthenticationPrincipal` resolves the parameter from that principal.
        */
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new IllegalStateException("expected a JWT-based authentication");
        }
        JwtAuthenticationToken jwtAuthToken = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthToken.getToken();
        Long userId = jwt.getClaim("userId");

        List<ExchangeRate> exchangeRates = exchangeRateRepository.findAllByUserId(userId);

        List<ExchangeRateResponse> erResponses = new ArrayList<>();
        for (ExchangeRate er : exchangeRates) {
            ExchangeRateResponse erR = new ExchangeRateResponse(
                er.getId(),
                er.getYear(),
                er.getSourceCurrency(),
                er.getTargetCurrency(),
                er.getSourceToTargetFactor()
            );
            erResponses.add(erR);
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(erResponses)
        ;
    }

    // Only `sourceToTargetFactor` changes;
    // a missing rate is not created (404 instead).
    @PutMapping("/{year}/{sourceCurrency}/{targetCurrency}")
    public ResponseEntity<ExchangeRateResponse> update(
        @PathVariable Integer year,
        @PathVariable @Pattern(regexp = "^[A-Z]{3}$") String sourceCurrency,
        @PathVariable @Pattern(regexp = "^[A-Z]{3}$") String targetCurrency,
        @Valid @RequestBody ExchangeRateRequestUpdate exchangeRateRequestUpdate,
        Authentication authentication
    ) {
        /*
        An alternative implementation would be
        to replace the `Authentication authentication` parameter with `@AuthenticationPrincipal Jwt jwt`
        and to replace the following 6 lines with
        `Long userId = jwt.getClaim("userId");`.

        That works because,
        after a successful Bearer authentication,
        «Spring Security» stores a `JwtAuthenticationToken` whose principal is the `Jwt` itself,
        and `@AuthenticationPrincipal` resolves the parameter from that principal.
        */
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new IllegalStateException("expected a JWT-based authentication");
        }
        JwtAuthenticationToken jwtAuthToken = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthToken.getToken();
        Long userId = jwt.getClaim("userId");

        Optional<ExchangeRate> optionalExchangeRate = exchangeRateRepository
            .findByUserIdAndYearAndSourceCurrencyAndTargetCurrency(userId, year, sourceCurrency, targetCurrency);

        if (optionalExchangeRate.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ExchangeRate er = optionalExchangeRate.get();

        er.setSourceToTargetFactor(exchangeRateRequestUpdate.sourceToTargetFactor());
        exchangeRateRepository.save(er);

        ExchangeRateResponse erR = new ExchangeRateResponse(
            er.getId(),
            er.getYear(),
            er.getSourceCurrency(),
            er.getTargetCurrency(),
            er.getSourceToTargetFactor()
        );
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(erR)
        ;
    }

    @DeleteMapping("/{year}/{sourceCurrency}/{targetCurrency}")
    public ResponseEntity<Void> delete(
        @PathVariable Integer year,
        @PathVariable @Pattern(regexp = "^[A-Z]{3}$") String sourceCurrency,
        @PathVariable @Pattern(regexp = "^[A-Z]{3}$") String targetCurrency,
        Authentication authentication
    ) {
        /*
        An alternative implementation would be
        to replace the `Authentication authentication` parameter with `@AuthenticationPrincipal Jwt jwt`
        and to replace the following 6 lines with
        `Long userId = jwt.getClaim("userId");`.

        That works because,
        after a successful Bearer authentication,
        «Spring Security» stores a `JwtAuthenticationToken` whose principal is the `Jwt` itself,
        and `@AuthenticationPrincipal` resolves the parameter from that principal.
        */
        if (!(authentication instanceof JwtAuthenticationToken)) {
            throw new IllegalStateException("expected a JWT-based authentication");
        }
        JwtAuthenticationToken jwtAuthToken = (JwtAuthenticationToken) authentication;
        Jwt jwt = jwtAuthToken.getToken();
        Long userId = jwt.getClaim("userId");

        Optional<ExchangeRate> optionalExchangeRate = exchangeRateRepository
            .findByUserIdAndYearAndSourceCurrencyAndTargetCurrency(userId, year, sourceCurrency, targetCurrency);

        if (optionalExchangeRate.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ExchangeRate er = optionalExchangeRate.get();

        exchangeRateRepository.delete(er);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
