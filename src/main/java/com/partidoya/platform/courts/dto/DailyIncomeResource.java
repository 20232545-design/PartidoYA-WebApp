package com.partidoya.platform.courts.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyIncomeResource(LocalDate date, BigDecimal income, long reservations) {
}
