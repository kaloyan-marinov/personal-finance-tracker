package com.kaloyan_marinov.personal_finance_tracker.transaction;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.kaloyan_marinov.personal_finance_tracker.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "transactions",
    indexes = @Index(
        name = "idx_transactions_user_id_year_currency",
        columnList = "user_id, year_, currency"
    )
)
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MySQL - more specifically, its storage engine InnoDB - creates an index for the foreign key on its own,
    // with a name you don't control.
    @ManyToOne(
        // By default, a `@ManyToOne` loads eagerly,
        // which would cause every load of a `Transaction` to also load its `User`;
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
            name = "fk_transactions_user_id"
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

    @Column(
        name = "amount",
        precision = 18,
        scale = 2,
        nullable = false
    )
    private BigDecimal amount;

    // Without `@JdbcTypeCode`, Hibernate would map a `String` to `VARCHAR` instead of `CHAR`.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
        name = "currency",
        length = 3,
        nullable = false
    )
    private String currency;

    @Column(
        name = "description",
        length = 255,
        nullable = false
    )
    private String description;

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

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
