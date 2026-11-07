package com.likelionknu.dojang.analyze.data.dto.response;

import java.util.List;

/** missing: 판정에 필요한데 대화에 없는 값. questions: missing 중 앞의 1~2개를 되묻는 질문 */
public record AnalyzeResponseDto(List<FactDto> facts, List<String> missing, List<QuestionDto> questions) {

    /** evidence: 사용자 글에서 그대로 인용한 문장. 근거가 없으면 null */
    public record FactDto(String key, Object value, String evidence) {
    }

    /** options가 빈 목록이면 직접 입력(나이) */
    public record QuestionDto(String key, String text, List<OptionDto> options) {
    }

    public record OptionDto(String value, String label) {
    }
}
