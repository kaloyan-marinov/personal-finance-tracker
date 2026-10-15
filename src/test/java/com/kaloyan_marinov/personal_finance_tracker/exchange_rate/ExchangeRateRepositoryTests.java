package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ConstraintViolationException;

// These tests bypass the endpoints (and thereby the validation on the request records),
// so as to check the guarantees given by the entity and by the database.
@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:test",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.mysql.sql",
        "pft.security.user.name=application-user-for-test-suite-u",
        "pft.security.user.password=application-user-for-test-suite-p"
    }
)
@Transactional
class ExchangeRateRepositoryTests {

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    /**
     * Attempts to save a rate whose {@code (year, sourceCurrency, targetCurrency)} already exists.
     * Expects {@code uq_exchange_rates_year_currencies} to reject it.
     */
    @Test
    void saveAndFlush_1() {
        // Arrange.
        Integer year = 2001;
        String sourceCurrency = "EUR";
        String targetCurrency = "USD";
        ExchangeRate er = new ExchangeRate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.085")
        );

        exchangeRateRepository.saveAndFlush(er);
        
        // Act + Assert.
        ExchangeRate duplicate = new ExchangeRate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.1")
        );

        assertThrows(
            DataIntegrityViolationException.class,
            () -> exchangeRateRepository.saveAndFlush(duplicate)
        );
    }

    /**
     * Attempts to save a rate whose {@code sourceToTargetFactor} is zero.
     * Expects {@code chk_exchange_rates_factor_positive} to reject it.
     */
    @Test
    void saveAndFlush_2() {
        // Arrange.
        ExchangeRate exchangeRate = new ExchangeRate(
            2002,
            "EUR",
            "USD",
            new BigDecimal("0")
        );

        // Act + Assert.
        assertThrows(
            DataIntegrityViolationException.class,
            () -> exchangeRateRepository.saveAndFlush(exchangeRate)
        );
    }

    /**
     * Attempts to save a rate whose {@code sourceCurrency} equals its {@code targetCurrency}.
     * Expects {@code chk_exchange_rates_distinct_currencies} to reject it.
     */
    @Test
    void saveAndFlush_3() {
        // Arrange.
        ExchangeRate exchangeRate = new ExchangeRate(
            2003,
            "EUR",
            "EUR",
            new BigDecimal("1")
        );

        // Act + Assert.
        assertThrows(
            DataIntegrityViolationException.class,
            () -> exchangeRateRepository.saveAndFlush(exchangeRate)
        );
    }

    /**
     * Attempts to save rates
     * whose {@code sourceCurrency} violates {@code @NotNull} or {@code @Pattern} on the entity.
     * Expects Hibernate to reject each one before the insert.
     */
    @Test
    void saveAndFlush_4() {
        for (String currency : new String[] {null, "eur", "EURO"}) {
            // Arrange.
            ExchangeRate exchangeRate = new ExchangeRate(
                2004,
                currency,
                "USD",
                new BigDecimal("1.085")
            );

            // Act + Assert.
            assertThrows(
                ConstraintViolationException.class,
                () -> exchangeRateRepository.saveAndFlush(exchangeRate)
            );
        }
    }
}
