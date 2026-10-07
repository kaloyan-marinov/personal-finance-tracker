package com.kaloyan_marinov.personal_finance_tracker.totals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kaloyan_marinov.personal_finance_tracker.exchange_rate.ExchangeRate;
import com.kaloyan_marinov.personal_finance_tracker.exchange_rate.ExchangeRateRepository;
import com.kaloyan_marinov.personal_finance_tracker.transaction.CurrencyTotal;
import com.kaloyan_marinov.personal_finance_tracker.transaction.TransactionRepository;

import jakarta.validation.constraints.Pattern;

@RestController
@RequestMapping("/api/v1/totals")
public class TotalsController {

    private TransactionRepository transactionRepository;

    private ExchangeRateRepository exchangeRateRepository;

    public TotalsController(
        TransactionRepository transactionRepository,
        ExchangeRateRepository exchangeRateRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.exchangeRateRepository = exchangeRateRepository;
    }

    // The return type is `ResponseEntity<?>`,
    // because the body is either a `TotalsResponse` or an error.
    @GetMapping("")
    public ResponseEntity<?> computeTotals(
        @RequestParam Integer year,
        // (
        // Spring validates `@Pattern` on a `@RequestParam` without `@Validated`,
        // and reports a violation as a `HandlerMethodValidationException`.
        // A missing `currency` is `null`, which `@Pattern` accepts.
        // )
        @RequestParam(required = false) @Pattern(regexp = "^[A-Z]{3}$") String currency
    ) {
        List<CurrencyTotal> currencyTotals = transactionRepository.sumAmountsByCurrencyAndSign(year);

        if (currency == null) {
            // Report totals for each currency.
            TotalsResponse totalsResponse = groupBySign(year, currencyTotals);

            return ResponseEntity
                .status(HttpStatus.OK)
                .body(totalsResponse)
            ;
        }

        // Convert all totals to `currency`,
        // and aggregate the converted amounts.
        Set<String> currenciesNeedingToBeConverted = findCurrenciesNeedingToBeConverted(
            currency,
            currencyTotals
        );
        Map<String, BigDecimal> currencyNeedingToBeConverted_2_exchangeRateFactor =
            loadSrcCurrency_2_exchangeRateFactor(
                year,
                currency,
                currenciesNeedingToBeConverted
            );

        List<MissingExchangeRate> missingExchangeRates = detectMissingExchangeRates(
            year,
            currency,
            currenciesNeedingToBeConverted,
            currencyNeedingToBeConverted_2_exchangeRateFactor
        );
        if (!missingExchangeRates.isEmpty()) {
            return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(
                    Map.of(
                        "error", "Missing exchange rates",
                        "missingExchangeRates", missingExchangeRates
                    )
                )
            ;
        }

        TotalsResponse totalsResponse = convertCurrencyTotalsAndAggregate(
            year,
            currency,
            currencyTotals,
            currencyNeedingToBeConverted_2_exchangeRateFactor
        );

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(totalsResponse)
        ;
    }

    private TotalsResponse groupBySign(Integer year, List<CurrencyTotal> currencyTotals) {
        // A `LinkedHashMap` remembers the order in which keys were added,
        // so the keys of `expenses` / `incomes` come out in the query's ORDER BY.
        // (
        // That is cosmetic: JSON objects are unordered (RFC 8259),
        // so clients must not depend on the order of the keys,
        // and `JsonCompareMode.STRICT` ignores it, too.
        // A plain `HashMap` would be correct, but would make the response less predictable.
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

        return new TotalsResponse(year, expenses, incomes);
    }

    private Set<String> findCurrenciesNeedingToBeConverted(
        String targetCurrency,
        List<CurrencyTotal> currencyTotals
    ) {
        Set<String> currenciesNeedingToBeConverted = new HashSet<>();

        for (CurrencyTotal ct : currencyTotals) {
            String srcCurrency = ct.currency();

            if (!srcCurrency.equals(targetCurrency)) {
                currenciesNeedingToBeConverted.add(srcCurrency);
            }
        }

        return currenciesNeedingToBeConverted;
    }

    // This returns each found `sourceToTargetFactor`, keyed by its `sourceCurrency`.
    // Only direct rates (`sourceCurrency` -> `targetCurrency`) are looked up;
    // inverse and chained rates are deliberately not used.
    private Map<String, BigDecimal> loadSrcCurrency_2_exchangeRateFactor(
        Integer year,
        String targetCurrency,
        Set<String> sourceCurrencies
    ) {
        Map<String, BigDecimal> srcCurrency_2_exchangeRateFactor = new HashMap<>();

        // When every total is already in `targetCurrency` (or there are no totals at all),
        // there is nothing that needs to be done here.
        if (sourceCurrencies.isEmpty()) {
            return srcCurrency_2_exchangeRateFactor;
        }

        List<ExchangeRate> exchangeRates = exchangeRateRepository
            .findByYearAndTargetCurrencyAndSourceCurrencyIn(year, targetCurrency, sourceCurrencies);
        for (ExchangeRate er : exchangeRates) {
            srcCurrency_2_exchangeRateFactor.put(
                er.getSourceCurrency(),
                er.getSourceToTargetFactor()
            );
        }
        return srcCurrency_2_exchangeRateFactor;
    }

    // The result is sorted by `sourceCurrency`,
    // so that the response is deterministic.
    private List<MissingExchangeRate> detectMissingExchangeRates(
        Integer year,
        String targetCurrency,
        Set<String> currenciesNeedingToBeConverted,
        Map<String, BigDecimal> srcCurrency_2_exchangeRateFactor
    ) {
        return currenciesNeedingToBeConverted
            .stream()
            .filter(currencyNeedingToBeConverted -> !srcCurrency_2_exchangeRateFactor.containsKey(currencyNeedingToBeConverted))
            .map(currencyNeedingToBeConverted -> new MissingExchangeRate(year, currencyNeedingToBeConverted, targetCurrency))
            .sorted(Comparator.comparing(MissingExchangeRate::sourceCurrency))
            .toList();
    }

    // Expenses and incomes are summed separately,
    // so the expenses total stays negative and the incomes total stays positive.
    // Each sum is rounded only once, at the end, to avoid accumulating rounding errors.
    private TotalsResponse convertCurrencyTotalsAndAggregate(
        Integer year,
        String targetCurrency,
        List<CurrencyTotal> currencyTotals,
        Map<String, BigDecimal> currencyNeedingToBeConverted_2_exchangeRateFactor
    ) {
        BigDecimal expensesSum = null;
        BigDecimal incomesSum = null;

        for (CurrencyTotal ct : currencyTotals) {
            BigDecimal converted = ct.currency().equals(targetCurrency)
                ? ct.total()
                : ct.total().multiply(
                    currencyNeedingToBeConverted_2_exchangeRateFactor.get(ct.currency())
                );

            if (ct.sign() < 0) {
                expensesSum = expensesSum == null ? converted : expensesSum.add(converted);
            } else {
                incomesSum = incomesSum == null ? converted : incomesSum.add(converted);
            }
        }

        return new TotalsResponse(
            year,
            toSingleEntryMap(targetCurrency, expensesSum),
            toSingleEntryMap(targetCurrency, incomesSum)
        );
    }

    // A `null` sum means there was nothing to sum, which is reported as `{}`.
    private Map<String, BigDecimal> toSingleEntryMap(String currency, BigDecimal sum) {
        if (sum == null) {
            return Map.of();
        }

        return Map.of(
            currency,
            sum.setScale(2, RoundingMode.HALF_UP)
        );
    }
}
