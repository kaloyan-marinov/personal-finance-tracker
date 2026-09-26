package com.kaloyan_marinov.personal_finance_tracker;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    // According to https://www.h2database.com/html/advanced.html ,
    // `YEAR` is a reserved SQL keyword in H2.
    @Column(
        name = "year_",
        nullable = false
    )
    private Integer year;

    @NotNull
    // The following annotation is consistent with the values specified for `precision` and `scale`;
    // rejects values like 4.789 - which the database would, if left to its own devices, round or reject! -
    // and returns a 400 for them.
    @Digits(integer = 16, fraction = 2)
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
