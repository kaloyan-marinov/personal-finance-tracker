package com.kaloyan_marinov.personal_finance_tracker.user;

public record UserResponseCreate(
    Long id,
    String username,
    String email
) {
}
