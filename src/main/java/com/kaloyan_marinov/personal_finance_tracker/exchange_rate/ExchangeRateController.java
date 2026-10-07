package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    public ExchangeRateController(ExchangeRateRepository exchangeRateRepository) {
        this.exchangeRateRepository = exchangeRateRepository;
    }

    // The return type is `ResponseEntity<?>`,
    // because the body is either the created `ExchangeRate` or an error.
    @PostMapping("")
    public ResponseEntity<?> create(
        @Valid @RequestBody ExchangeRateRequestCreate exchangeRateRequestCreate
    ) {
        // `uq_exchange_rates_year_currencies` remains the guarantee
        // (e.g. for writes that bypass the endpoints);
        // this check merely gives the client a 409 rather than a database error.
        boolean exists = exchangeRateRepository.existsByYearAndSourceCurrencyAndTargetCurrency(
            exchangeRateRequestCreate.year(),
            exchangeRateRequestCreate.sourceCurrency(),
            exchangeRateRequestCreate.targetCurrency()
        );

        if (exists) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Exchange rate already exists"))
            ;
        }

        ExchangeRate er = new ExchangeRate(
            exchangeRateRequestCreate.year(),
            exchangeRateRequestCreate.sourceCurrency(),
            exchangeRateRequestCreate.targetCurrency(),
            exchangeRateRequestCreate.sourceToTargetFactor()
        );
        ExchangeRate createdExchangeRate = exchangeRateRepository.save(er);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(createdExchangeRate)
        ;
    }

    @GetMapping("/{year}/{sourceCurrency}/{targetCurrency}")
    public ResponseEntity<ExchangeRate> retrieveOne(
        @PathVariable Integer year,
        @PathVariable @Pattern(regexp = "^[A-Z]{3}$") String sourceCurrency,
        @PathVariable @Pattern(regexp = "^[A-Z]{3}$") String targetCurrency
    ) {
        Optional<ExchangeRate> optionalExchangeRate = exchangeRateRepository
            .findByYearAndSourceCurrencyAndTargetCurrency(year, sourceCurrency, targetCurrency);

        if (optionalExchangeRate.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(optionalExchangeRate.get())
        ;
    }

    @GetMapping("")
    public ResponseEntity<List<ExchangeRate>> retrieveAll() {
        List<ExchangeRate> exchangeRates = exchangeRateRepository.findAll();

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(exchangeRates)
        ;
    }

    // Only `sourceToTargetFactor` changes;
    // a missing rate is not created (404 instead).
    @PutMapping("/{year}/{sourceCurrency}/{targetCurrency}")
    public ResponseEntity<ExchangeRate> update(
        @PathVariable Integer year,
        @PathVariable @Pattern(regexp = "^[A-Z]{3}$") String sourceCurrency,
        @PathVariable @Pattern(regexp = "^[A-Z]{3}$") String targetCurrency,
        @Valid @RequestBody ExchangeRateRequestUpdate exchangeRateRequestUpdate
    ) {
        Optional<ExchangeRate> optionalExchangeRate = exchangeRateRepository
            .findByYearAndSourceCurrencyAndTargetCurrency(year, sourceCurrency, targetCurrency);

        if (optionalExchangeRate.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        ExchangeRate er = optionalExchangeRate.get();
        er.setSourceToTargetFactor(exchangeRateRequestUpdate.sourceToTargetFactor());
        exchangeRateRepository.save(er);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(er)
        ;
    }

    @DeleteMapping("/{year}/{sourceCurrency}/{targetCurrency}")
    public ResponseEntity<Void> delete(
        @PathVariable Integer year,
        @PathVariable @Pattern(regexp = "^[A-Z]{3}$") String sourceCurrency,
        @PathVariable @Pattern(regexp = "^[A-Z]{3}$") String targetCurrency
    ) {
        Optional<ExchangeRate> optionalExchangeRate = exchangeRateRepository
            .findByYearAndSourceCurrencyAndTargetCurrency(year, sourceCurrency, targetCurrency);

        if (optionalExchangeRate.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        exchangeRateRepository.delete(optionalExchangeRate.get());

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
