package com.kaloyan_marinov.personal_finance_tracker;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    // According to https://www.h2database.com/html/advanced.html ,
    // `YEAR` is a reserved SQL keyword in H2.
    @Column(
        name = "year_"
    )
    private Integer year;

    @Column(
        name = "amount",
        precision = 18,
        scale = 2,
        nullable = false
    )
    private BigDecimal amount;

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
}
