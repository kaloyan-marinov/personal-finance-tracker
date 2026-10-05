package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// This represents the body
// of an HTTP request for updating an existing `ExchangeRate`.
// It deliberately has no key fields (`year`, `sourceCurrency`, `targetCurrency`),
// because the key is already in the URL,
// and allowing it in the body as well would raise the question of which one wins.
public record ExchangeRateRequestUpdate(
    @NotNull
    @Positive
    @Digits(integer = 10, fraction = 8)
    BigDecimal sourceToTargetFactor
) {
}
