package com.kaloyan_marinov.personal_finance_tracker;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Both maps are intended to be keyed by currency,
 * because totals in different currencies must never be added together.
 */
public record TotalsResponse(
    Integer year,
    Map<String, BigDecimal> expenses,
    Map<String, BigDecimal> incomes
) {
}
