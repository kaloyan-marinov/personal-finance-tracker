package com.kaloyan_marinov.personal_finance_tracker.transaction;

import java.math.BigDecimal;

/**
 * This is intended to represent/model
 * the sum of all (of a given year's) amounts
 * which are in a given {@code currency}
 * and have the same {@code sign}
 * (-1 for expenses, +1 for incomes).
 */
public record CurrencyTotal(
    String currency,
    Integer sign,
    BigDecimal total
) {
}
