package com.partidoya.platform.courts.services;

import java.time.LocalDate;

record DateRange(LocalDate from, LocalDate to) {
    static DateRange resolve(LocalDate from, LocalDate to) {
        var resolvedFrom = from == null ? LocalDate.now().minusMonths(1) : from;
        var resolvedTo = to == null ? LocalDate.now().plusMonths(3) : to;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new IllegalArgumentException("date range is invalid");
        }
        return new DateRange(resolvedFrom, resolvedTo);
    }
}
