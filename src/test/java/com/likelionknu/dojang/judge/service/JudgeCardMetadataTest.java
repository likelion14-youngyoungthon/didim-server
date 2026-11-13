package com.likelionknu.dojang.judge.service;

import com.likelionknu.dojang.judge.data.dto.request.JudgeRequestDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JudgeCardMetadataTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    private JudgeRequestDto request(String status, String band, String type) {
        return new JudgeRequestDto(4, type, "severe", "백암면", 3, status, band, List.of(), false, List.of("daycare"));
    }

    @Test void 혜택은_배열순서보다_수급_소득_기본값_우선순위를_따른다() {
        var rule = mapper.readTree("""
                {"benefit":[{"when":"any","amount":"기본"},
                  {"when":"le180","amount":"소득"},{"when":"basic","amount":"수급"},
                  {"when":"basic","amount":"뒤의 중복"}]}
                """);
        assertEquals("수급", JudgeCardMetadata.benefit(request("basic", "le180", "autism"), rule).amount());
        assertEquals("소득", JudgeCardMetadata.benefit(request("neither", "le180", "autism"), rule).amount());
        assertEquals("기본", JudgeCardMetadata.benefit(request(null, null, "autism"), rule).amount());
    }

    @Test void 일치하는_혜택이_없으면_금액을_추정하지_않는다() {
        var rule = mapper.readTree("""
                {"benefit":[{"when":"le180","amount":"월 18만원"}]}
                """);
        assertNull(JudgeCardMetadata.benefit(request("neither", "le120", "autism"), rule));
        assertNull(JudgeCardMetadata.benefit(request("unknown", "unknown", "autism"), rule));
        assertNull(JudgeCardMetadata.benefit(request(null, null, "autism"), mapper.readTree("{}")));
    }

    @Test void 조건은_표시문장을_파싱하지_않고_규칙과_미확인값을_사용한다() {
        var rule = mapper.readTree("""
                {"id":"sample","age_max":17,"disability_types":["autism"],
                 "disability_types_conditional":["hearing"],"income":{"type":"median_pct","pct":180},
                 "conditions":[{"key":"age","rule":"화면에 쓸 문장"},
                  {"key":"disability_type"},{"key":"income_band"}]}
                """);
        var known = JudgeCardMetadata.conditions(request("neither", "le180", "autism"), rule);
        assertEquals("만 4세", known.get(0).value());
        assertTrue(known.get(0).met());
        assertEquals("자폐성", known.get(1).value());
        assertTrue(known.get(1).met());
        assertTrue(known.get(2).met());
        var unknown = JudgeCardMetadata.conditions(request(null, "unknown", "unknown"), rule);
        assertNull(unknown.get(1).met());
        assertNull(unknown.get(2).met());
        assertEquals("잘 모르겠어요", unknown.get(1).value());
        assertNull(JudgeCardMetadata.conditions(request(null, null, "hearing"), rule).get(1).met());
        assertFalse(JudgeCardMetadata.conditions(request(null, "over180", "physical"), rule).get(1).met());
        assertFalse(JudgeCardMetadata.conditions(request(null, "over180", "physical"), rule).get(2).met());
    }

    @Test void 미확정_조건은_null이고_빈조건목록과_누락은_구별한다() {
        var q = request(null, null, null);
        assertNull(JudgeCardMetadata.conditions(q, mapper.readTree("{}")));
        assertEquals(List.of(), JudgeCardMetadata.conditions(q, mapper.readTree("{\"conditions\":[]}")));
        var rule = mapper.readTree("{\"conditions\":[{\"key\":\"unconfirmed\",\"rule\":\"확인 필요\"}]}");
        assertNull(JudgeCardMetadata.conditions(q, rule).getFirst().met());
    }
}
