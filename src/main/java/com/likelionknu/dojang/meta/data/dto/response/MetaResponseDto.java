package com.likelionknu.dojang.meta.data.dto.response;

import java.time.LocalDate;

public record MetaResponseDto(LocalDate rulesUpdated, LocalDate officesUpdated, LocalDate welfareApiFetched) {
}
