package com.kaloyan_marinov.personal_finance_tracker.common;

public record PaginationInfo(
    long total,
    int pageNumber,
    int pageSize
) {
}
