package com.kaloyan_marinov.personal_finance_tracker.totals;

/**
 * This identifies an {@code ExchangeRate}
 * which is required for converting the totals into the chosen currency,
 * but which is absent from the database.
 */
public record MissingExchangeRate(
    Integer year,
    String sourceCurrency,
    String targetCurrency
) {
}
