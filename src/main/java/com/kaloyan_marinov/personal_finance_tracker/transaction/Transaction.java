package com.kaloyan_marinov.personal_finance_tracker.transaction;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
    name = "transactions",
    indexes = @Index(
        name = "idx_transactions_year_currency",
        columnList = "year_, currency"
    )
)
public class Transaction {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

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
