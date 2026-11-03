package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import java.math.BigDecimal;

public record ExchangeRateResponse(
    Long id,
    Integer year,
    String sourceCurrency,
    String targetCurrency,
    BigDecimal sourceToTargetFactor
) {
}
