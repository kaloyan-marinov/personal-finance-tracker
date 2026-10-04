package com.kaloyan_marinov.personal_finance_tracker;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// This represents the body
// of an HTTP request for creating a new `Transaction`
// or of one for updating an existing `Transaction`.
// It deliberately has no `id`, so a client cannot choose (and thereby overwrite) an existing row;
// an `id` sent in the JSON body is ignored.
public record TransactionRequest(
    @NotNull
    Integer year,

    @NotNull
    // The following annotation is consistent with the `precision` and `scale` of `Transaction.amount`;
    // rejects values like 4.789 - which the database would, if left to its own devices, round or reject! -
    // and returns a 400 for them.
    @Digits(integer = 16, fraction = 2)
    BigDecimal amount,

    @NotNull
    @Pattern(regexp = "^[A-Z]{3}$")
    String currency,

    @NotBlank
    @Size(max = 255)
    String description
) {
}
