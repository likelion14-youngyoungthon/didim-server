package com.likelionknu.dojang.income.data.dto.response;

import java.time.LocalDate;
import java.util.List;

public record IncomeBandsResponseDto(int year, int household, List<IncomeBandResponseDto> bands, String source, LocalDate checkedAt) {
}
