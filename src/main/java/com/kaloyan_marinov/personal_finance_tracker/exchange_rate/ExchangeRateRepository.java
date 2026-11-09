package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    boolean existsByUserId(Long userId);

    Page<ExchangeRate> findAllByUserId(
        Long userId,
        Pageable pageable
    );

    boolean existsByUserIdAndYearAndSourceCurrencyAndTargetCurrency(
        Long userId,
        Integer year,
        String sourceCurrency,
        String targetCurrency
    );

    Optional<ExchangeRate> findByUserIdAndYearAndSourceCurrencyAndTargetCurrency(
        Long userId,
        Integer year,
        String sourceCurrency,
        String targetCurrency
    );

    // This loads all the requested rates with a single query (`... IN (...)`),
    // rather than with 1 query per source currency.
    List<ExchangeRate> findByUserIdAndYearAndTargetCurrencyAndSourceCurrencyIn(
        Long userId,
        Integer year,
        String targetCurrency,
        Collection<String> sourceCurrencies
    );
}
