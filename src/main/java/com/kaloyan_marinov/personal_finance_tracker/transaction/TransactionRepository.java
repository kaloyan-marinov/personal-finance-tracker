package com.kaloyan_marinov.personal_finance_tracker.transaction;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/*
Upon application startup (= At the very beginning of runtime),
the «Spring IoC container» will create an implementation of the following `interface`
and
a «bean definition» based on that implementation will be added to the «Spring IoC container»'s registry of «bean definitions».
That «bean definition» has almost the same name as the class
(the only difference will be a change in the case).
In this case, the name will of the «bean definition» be `transactionRepository`.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByUserId(Long userId);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    Page<Transaction> findAllByUserId(
        Long userId,
        Pageable pageable
    );

    /**
     * The summing happens in the database,
     * so the {@link Transaction}s of the year are never loaded into memory.
     * A {@link Transaction} whose {@code amount} is exactly 0 is neither an expense nor an income,
     * so it is filtered out.
     * 
     * <p>
     * JPQL
     * <ul>
     *  <li>Java Persistence Query Language
     *      (now officially "Jakarta Persistence Query Language" since the move to Jakarta EE).</li>
     *  <li>It's the object-oriented query language
     *      used with JPA
     *      to query entities rather than database tables.</li>
     * </ul>
     *
     * <p>
     * The query is written in JPQL (not SQL) - it refers to the entity and its fields,
     * and Hibernate translates it into SQL for the underlying database.
     * 
     * (
     * A derived query method (such as {@code findByYear}) cannot be used here,
     * because derived query methods do not support aggregation ({@code SUM}, {@code GROUP BY})
     * or constructor projections ({@code new CurrencyTotal(...)}).
     * )
     *
     * <p>
     * Alternatives that avoid JPQL exist,
     * but each has a drawback:
     * <ul>
     *   <li>loading the {@link Transaction}s and aggregating them via Java streams
     *       loads all {@link Transaction}s of the year into memory;</li>
     *   <li>building the same query via the JPA Criteria API (or Querydsl / jOOQ)
     *       is much more verbose and requires a custom repository implementation.</li>
     * </ul>
     */
    @Query("""
        SELECT
            new com.kaloyan_marinov.personal_finance_tracker.transaction.CurrencyTotal(
                t.currency,
                SIGN(t.amount),
                SUM(t.amount)
            )
        FROM
            Transaction t
        WHERE
            t.user.id = :userId
            AND
            t.year = :year
            AND
            t.amount != 0
        GROUP BY
            t.currency, SIGN(t.amount)
        ORDER BY
            t.currency
    """)
    List<CurrencyTotal> sumAmountsByCurrencyAndSign(
        /*
        Each `@Param` binds its parameter to the `:name` of the same name in the query.

        Without `@Param`, «Spring Data» would read the names from the compiled class,
        which only keeps them if the compiler runs with the `-parameters` flag.
        «Spring Boot»'s parent POM turns that flag on,
        but a build without it (e.g. some IDE builds) would make the application fail at startup.

        So every parameter gets an explicit `@Param`.
        */
        @Param("userId") Long userId,
        @Param("year") Integer year
    );
}
