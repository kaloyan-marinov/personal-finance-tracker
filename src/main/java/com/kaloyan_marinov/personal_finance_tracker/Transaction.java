package com.kaloyan_marinov.personal_finance_tracker;

public record Transaction(
    long id,
    int year,
    float amount
) {
    
}
