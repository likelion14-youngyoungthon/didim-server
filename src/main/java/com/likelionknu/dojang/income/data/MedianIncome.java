package com.likelionknu.dojang.income.data;

import java.time.LocalDate;
import java.util.Map;

/** increment_won: 표에 있는 가장 큰 가구보다 1인 늘 때마다 더하는 금액 */
public record MedianIncome(int year, String source, LocalDate checkedAt, Map<Integer, Long> monthlyWon, long incrementWon) {
}
