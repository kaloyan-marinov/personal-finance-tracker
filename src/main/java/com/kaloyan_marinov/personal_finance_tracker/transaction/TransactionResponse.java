package com.kaloyan_marinov.personal_finance_tracker.transaction;

import java.math.BigDecimal;

public record TransactionResponse(
    Long id,
    Integer year,
    BigDecimal amount,
    String currency,
    String description
) {
}
