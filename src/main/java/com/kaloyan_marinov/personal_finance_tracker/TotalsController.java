package com.kaloyan_marinov.personal_finance_tracker;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TotalsController {

    private TransactionRepository transactionRepository;

    public TotalsController(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/totals")
    public ResponseEntity<TotalsResponse> computeTotals(
        @RequestParam Integer year
    ) {
        List<CurrencyTotal> currencyTotals = transactionRepository.sumAmountsByCurrencyAndSign(year);

        // A `LinkedHashMap` remembers the order in which keys were added.
        // (
        // A plain `HashMap` doesn't guarantee any iteration order.
        // The keys could come out in any order,
        // which would make the ORDER BY pointless,
        // and tests that compare the exact JSON string could fail at random.
        // )
        Map<String, BigDecimal> expenses = new LinkedHashMap<>();
        Map<String, BigDecimal> incomes = new LinkedHashMap<>();

        for (CurrencyTotal ct : currencyTotals) {
            if (ct.sign() < 0) {
                expenses.put(ct.currency(), ct.total());
            } else {
                // `sign` cannot be 0 here, because the query filters out zero amounts.
                incomes.put(ct.currency(), ct.total());
            }
        }

        TotalsResponse totalsResponse = new TotalsResponse(
            year,
            expenses,
            incomes
        );
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(totalsResponse)
        ;
    }
}
