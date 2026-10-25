package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

// This represents the body
// of an HTTP request for creating a new `ExchangeRate`.
// It deliberately has no `id`, so a client cannot choose (and thereby overwrite) an existing row;
// an `id` sent in the JSON body is ignored.
public record ExchangeRateRequestCreate(
    @NotNull
    Integer year,

    @NotNull
    @Pattern(regexp = "^[A-Z]{3}$")
    String sourceCurrency,

    @NotNull
    @Pattern(regexp = "^[A-Z]{3}$")
    String targetCurrency,

    // `@Positive` mirrors `chk_exchange_rates_factor_positive`,
    // and `@Digits` mirrors the `precision` and `scale` of `ExchangeRate.sourceToTargetFactor`.
    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 8)
    BigDecimal sourceToTargetFactor
) {

    /**
     * This mirrors `chk_exchange_rates_distinct_currencies`,
     * so that the client gets a 400 with a `fieldErrors` entry
     * rather than a database error.
     * 
     * (
     * A few remarks are in order:
     * 
     * - A missing currency is reported by `@NotNull` instead,
     *   so it is not reported here too.
     * 
     * - `@JsonIgnore` prevents this method
     *   from being serialized as a `distinctCurrencies` property.
     * )
    */
    @JsonIgnore
    @AssertTrue(message = "sourceCurrency and targetCurrency must differ")
    public boolean isDistinctCurrencies() {
        if (sourceCurrency == null || targetCurrency == null) {
            return true;
        }
        return !sourceCurrency.equals(targetCurrency);
    }
}
