package com.kaloyan_marinov.personal_finance_tracker.common;

public final class ConstantsForPagination {

    private ConstantsForPagination() {
        throw new AssertionError("Not allowed to instantiate this class");
    }

    public static final int PAGE_NUMBER_MIN = 0;
    public static final int PAGE_NUMBER_DEFAULT = 0;

    public static final int PAGE_SIZE_MIN = 1;
    public static final int PAGE_SIZE_MAX = 100;
    public static final int PAGE_SIZE_DEFAULT = 10;
}
