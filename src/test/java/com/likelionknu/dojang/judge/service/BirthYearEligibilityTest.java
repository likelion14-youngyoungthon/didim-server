package com.likelionknu.dojang.judge.service;

import com.likelionknu.dojang.easy.service.EasyTextService;
import com.likelionknu.dojang.judge.data.dto.request.JudgeRequestDto;
import com.likelionknu.dojang.office.service.OfficeService;
import com.likelionknu.dojang.regitem.service.RegItemService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BirthYearEligibilityTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private JudgeService service(String instant) throws Exception {
        var office = mock(OfficeService.class);
        var regItems = mock(RegItemService.class);
        when(regItems.getRegItems()).thenReturn(List.of());
        when(office.getOffices("백암면", null, null)).thenReturn(List.of());
        return new JudgeService(mapper, office, regItems, mock(EasyTextService.class), Clock.fixed(Instant.parse(instant), KST));
    }

    private JudgeRequestDto request(int age) {
        return new JudgeRequestDto(age, "autism", "severe", "백암면", 3, "neither", "le180", List.of(), false, List.of("daycare"));
    }

    @Test void 두_가능한_출생연도로_양끝_포함과_불확실성을_구분한다() {
        var rule = mapper.readTree("{\"birth_year_min\":1958,\"birth_year_max\":2022}");
        int[][] cases = {{2,-1}, {3,0}, {4,1}, {67,1}, {68,0}, {69,-1}, {Integer.MAX_VALUE,-1}};
        for (int[] item : cases) assertEquals(item[1], BirthYearEligibility.match(item[0], rule, 2026), "age=" + item[0]);
    }

    @Test void 네살은_신청후보_경계는_확인필요_범위밖은_제외한다() throws Exception {
        var service = service("2026-11-14T00:00:00Z");
        String[][] cases = {{"2","not_eligible"}, {"3","check_needed"}, {"4","eligible"},
                {"67","eligible"}, {"68","check_needed"}, {"69","not_eligible"}};
        for (String[] item : cases) {
            var sports = service.judge(request(Integer.parseInt(item[0]))).get("sports_voucher");
            assertEquals(item[1], sports.verdict());
            assertNull(sports.eligibleFromAge());
        }
        var sports = service.judge(request(4)).get("sports_voucher");
        assertEquals("apply", sports.status());
        assertTrue(sports.deadline().contains("2026.11.9~11.27 23:59"));
        assertEquals(9, service.listServices(4).size());
        assertTrue(service.listServices(3).stream().anyMatch(s -> s.id().equals("sports_voucher")));
        assertFalse(service.listServices(69).stream().anyMatch(s -> s.id().equals("sports_voucher")));
        assertEquals("eligible", service.judge(request(4)).get("dev_rehab_voucher").verdict());
    }

    @Test void 한국시간_연도전환을_사용하고_사업의_출생연도범위는_그대로_둔다() throws Exception {
        var before = service("2026-12-31T14:59:59Z");
        var after = service("2026-12-31T15:00:00Z");
        assertEquals("eligible", before.judge(request(4)).get("sports_voucher").verdict());
        assertEquals("check_needed", after.judge(request(4)).get("sports_voucher").verdict());
    }

    @Test void 조건카드도_단순나이_하한보다_출생연도_판정을_우선한다() {
        var rule = mapper.readTree("""
                {"age_min":5,"age_max":69,"birth_year_min":1958,"birth_year_max":2022,
                 "conditions":[{"key":"age","label":"출생연도","rule":"1958~2022년생"}]}
                """);
        assertTrue(JudgeCardMetadata.conditions(request(4), rule, 2026).getFirst().met());
        assertNull(JudgeCardMetadata.conditions(request(3), rule, 2026).getFirst().met());
        assertFalse(JudgeCardMetadata.conditions(request(69), rule, 2026).getFirst().met());
    }
}
