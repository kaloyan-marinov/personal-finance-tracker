package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.kaloyan_marinov.personal_finance_tracker.user.User;
import com.kaloyan_marinov.personal_finance_tracker.user.UserRepository;

import jakarta.validation.ConstraintViolationException;

// These tests bypass the endpoints (and thereby the validation on the request records),
// so as to check the guarantees given by the entity and by the database.
@SpringBootTest(
    properties = {
        "spring.datasource.url=jdbc:h2:mem:test",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.mysql.sql",
        "pft.security.secret.key=secret-value-that-must-be-of-at-least-256-bits-aka-32-bytes"
    }
)
@Transactional
class ExchangeRateRepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    /**
     * Attempts to save a rate whose {@code (user, year, sourceCurrency, targetCurrency)} already exists.
     * Expects {@code uq_exchange_rates_user_id_year_currencies} to reject it.
     */
    @Test
    void saveAndFlush_1() {
        // Arrange.
        User user = new User(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );
        userRepository.save(user);

        Integer year = 2001;
        String sourceCurrency = "EUR";
        String targetCurrency = "USD";
        ExchangeRate er = new ExchangeRate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.085")
        );
        er.setUser(user);

        exchangeRateRepository.saveAndFlush(er);
        
        // Act + Assert.
        ExchangeRate duplicate = new ExchangeRate(
            year,
            sourceCurrency,
            targetCurrency,
            new BigDecimal("1.1")
        );
        duplicate.setUser(user);

        DataIntegrityViolationException exc = assertThrows(
            DataIntegrityViolationException.class,
            () -> exchangeRateRepository.saveAndFlush(duplicate)
        );

        assertTrue(
            exc
            .getMessage().toUpperCase()
            .contains("UQ_EXCHANGE_RATES_USER_ID_YEAR_CURRENCIES")
        );
    }

    /**
     * Attempts to save a rate whose {@code sourceToTargetFactor} is zero.
     * Expects {@code chk_exchange_rates_factor_positive} to reject it.
     */
    @Test
    void saveAndFlush_2() {
        // Arrange.
        User user = new User(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );
        userRepository.save(user);

        ExchangeRate exchangeRate = new ExchangeRate(
            2002,
            "EUR",
            "USD",
            new BigDecimal("0")
        );
        exchangeRate.setUser(user);

        // Act + Assert.
        DataIntegrityViolationException exc = assertThrows(
            DataIntegrityViolationException.class,
            () -> exchangeRateRepository.saveAndFlush(exchangeRate)
        );

        assertTrue(
            exc
            .getMessage().toUpperCase()
            .contains("CHK_EXCHANGE_RATES_FACTOR_POSITIVE")
        );
    }

    /**
     * Attempts to save a rate whose {@code sourceCurrency} equals its {@code targetCurrency}.
     * Expects {@code chk_exchange_rates_distinct_currencies} to reject it.
     */
    @Test
    void saveAndFlush_3() {
        // Arrange.
        User user = new User(
            "test-" + "jd",
            "test-" + "john.doe@protonmail.com",
            "test-" + "123"
        );
        userRepository.save(user);

        ExchangeRate exchangeRate = new ExchangeRate(
            2003,
            "EUR",
            "EUR",
            new BigDecimal("1")
        );
        exchangeRate.setUser(user);

        // Act + Assert.
        DataIntegrityViolationException exc = assertThrows(
            DataIntegrityViolationException.class,
            () -> exchangeRateRepository.saveAndFlush(exchangeRate)
        );

        assertTrue(
            exc
            .getMessage().toUpperCase()
            .contains("CHK_EXCHANGE_RATES_DISTINCT_CURRENCIES")
        );
    }

    /*
     * This pertains to each of the following test cases.
     *
     * The missing `user` does not cause the test case to fail,
     * because the `INSERT` is never sent to the database:
     * Hibernate's `BeanValidationEventListener` runs before the `INSERT`
     * and throws on the invalid `sourceCurrency` (`@NotNull` or `@Pattern`),
     * so H2 never checks the `NOT NULL` on `user_id`.
     *
     * The null `user` isn't reported as a violation either,
     * because it has no `@NotNull`
     * (`@ManyToOne(optional = false)` and `@JoinColumn(nullable = false)`
     * are JPA mapping settings, not validation constraints),
     * and Hibernate's own null check (`hibernate.check_nullability`)
     * is off by default when «Bean Validation» is on the classpath.
     */

    /**
     * Attempts to save an exchange rate
     * whose {@code sourceCurrency} violates {@code @NotNull} or {@code @Pattern} on the entity.
     * Expects Hibernate to reject the attempt before the insert.
     */
    @Test
    void saveAndFlush_4_1() {
        // Arrange.
        String badCurrency = null;
        ExchangeRate exchangeRate = new ExchangeRate(
            2004,
            badCurrency,
            "USD",
            new BigDecimal("1.085")
        );

        // Act + Assert.
        ConstraintViolationException exc = assertThrows(
            ConstraintViolationException.class,
            () -> exchangeRateRepository.saveAndFlush(exchangeRate)
        );

        assertTrue(
            exc
            .getMessage()//.toUpperCase()
            .contains("interpolatedMessage='must not be null', propertyPath=sourceCurrency")
        );
    }

    /**
     * Attempts to save an exchange rate
     * whose {@code sourceCurrency} violates {@code @NotNull} or {@code @Pattern} on the entity.
     * Expects Hibernate to reject the attempt before the insert.
     */
    @Test
    void saveAndFlush_4_2() {
        // Arrange.
        String badCurrency = "eur";
        ExchangeRate exchangeRate = new ExchangeRate(
            2004,
            badCurrency,
            "USD",
            new BigDecimal("1.085")
        );

        // Act + Assert.
        ConstraintViolationException exc = assertThrows(
            ConstraintViolationException.class,
            () -> exchangeRateRepository.saveAndFlush(exchangeRate)
        );

        assertTrue(
            exc
            .getMessage()//.toUpperCase()
            .contains("interpolatedMessage='must match \"^[A-Z]{3}$\"', propertyPath=sourceCurrency")
        );
    }

    /**
     * Attempts to save an exchange rate
     * whose {@code sourceCurrency} violates {@code @NotNull} or {@code @Pattern} on the entity.
     * Expects Hibernate to reject the attempt before the insert.
     */
    @Test
    void saveAndFlush_4_3() {
        // Arrange.
        String badCurrency = "EURO";
        ExchangeRate exchangeRate = new ExchangeRate(
            2004,
            badCurrency,
            "USD",
            new BigDecimal("1.085")
        );

        // Act + Assert.
        ConstraintViolationException exc = assertThrows(
            ConstraintViolationException.class,
            () -> exchangeRateRepository.saveAndFlush(exchangeRate)
        );

        assertTrue(
            exc
            .getMessage()//.toUpperCase()
            .contains("interpolatedMessage='must match \"^[A-Z]{3}$\"', propertyPath=sourceCurrency")
        );
    }
}
