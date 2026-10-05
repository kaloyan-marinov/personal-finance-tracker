package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    boolean existsByYearAndSourceCurrencyAndTargetCurrency(
        Integer year,
        String sourceCurrency,
        String targetCurrency
    );

    Optional<ExchangeRate> findByYearAndSourceCurrencyAndTargetCurrency(
        Integer year,
        String sourceCurrency,
        String targetCurrency
    );
}
