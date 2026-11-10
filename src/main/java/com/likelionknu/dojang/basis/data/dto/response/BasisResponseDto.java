package com.likelionknu.dojang.basis.data.dto.response;

import java.time.LocalDate;
import java.util.List;

/** summary: 원문이 없거나 인용할 문장이 없으면 null */
public record BasisResponseDto(List<QuoteDto> quotes, String summary) {

    /** text: 원문 그대로. matches: 관련된 사용자 값의 key(age 등), 없으면 null */
    public record QuoteDto(String text, String matches, String source, LocalDate checkedAt) {
    }
}
