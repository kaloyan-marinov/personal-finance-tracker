package com.kaloyan_marinov.personal_finance_tracker.exchange_rate;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.kaloyan_marinov.personal_finance_tracker.user.User;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// The constraints below are also present in the Liquibase changeset that creates `exchange_rates`,
// because the tests build their schema from the Liquibase changelog, not from the entities.
@Entity
@Table(
    name = "exchange_rates",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_exchange_rates_user_id_year_currencies",
        columnNames = { "user_id", "year_", "source_currency", "target_currency" }
    ),
    check = {
        @CheckConstraint(
            name = "chk_exchange_rates_factor_positive",
            constraint = "source_to_target_factor > 0"
        ),
        @CheckConstraint(
            name = "chk_exchange_rates_distinct_currencies",
            constraint = "source_currency != target_currency"
        )
    }
)
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MySQL - more specifically, its storage engine InnoDB - creates an index for the foreign key on its own,
    // with a name you don't control.
    @ManyToOne(
        // By default, a `@ManyToOne` loads eagerly,
        // which would cause every load of an `ExchangeRate` to also load its `User`;
        // that is avoided by the following setting.
        fetch = FetchType.LAZY,
        // The following is a hint to Hibernate.
        optional = false
    )
    @JoinColumn(
        name = "user_id",
        // The following produces a `NOT NULL` in the database schema.
        nullable = false,
        // Without the following setting,
        // Hibernate would generate a name like `FKqwe8...`, which `liquibase:diff` would go on to copy.
        foreignKey = @ForeignKey(
            name = "fk_exchange_rates_user_id"
        )
    )
    private User user;

    // According to https://www.h2database.com/html/advanced.html ,
    // `YEAR` is a reserved SQL keyword in H2.
    @Column(
        name = "year_",
        nullable = false
    )
    private Integer year;

    // `@NotNull` and `@Pattern` are also placed on the entity (not only on `ExchangeRateRequestCreate`),
    // so that Hibernate validates them right before every insert and update,
    // which guards against writes that bypass the endpoints.
    @NotNull
    @Pattern(regexp = "^[A-Z]{3}$")
    // Without `@JdbcTypeCode`, Hibernate would map a `String` to `VARCHAR` instead of `CHAR`.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "source_currency",
        length = 3,
        nullable = false
    )
    private String sourceCurrency;

    @NotNull
    @Pattern(regexp = "^[A-Z]{3}$")
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "target_currency",
        length = 3,
        nullable = false
    )
    private String targetCurrency;

    // 1 unit of `sourceCurrency` = this many units of `targetCurrency`.
    @Column(
        name = "source_to_target_factor",
        precision = 18,
        scale = 8,
        nullable = false
    )
    private BigDecimal sourceToTargetFactor;

    // JPA requires a no-argument constructor.
    protected ExchangeRate() {
    }

    // There are no setters for the key fields,
    // because changing the key of an existing rate is out of scope.
    public ExchangeRate(
        Integer year,
        String sourceCurrency,
        String targetCurrency,
        BigDecimal sourceToTargetFactor
    ) {
        this.year = year;
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.sourceToTargetFactor = sourceToTargetFactor;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getYear() {
        return year;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public BigDecimal getSourceToTargetFactor() {
        return sourceToTargetFactor;
    }

    public void setSourceToTargetFactor(BigDecimal sourceToTargetFactor) {
        this.sourceToTargetFactor = sourceToTargetFactor;
    }
}
