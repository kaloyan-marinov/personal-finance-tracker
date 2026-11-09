package com.kaloyan_marinov.personal_finance_tracker.common;

import java.util.List;

public record PaginatedResponse<T>(
    List<T> resources,
    PaginationInfo paginationInfo
) {
}
