package com.likelionknu.dojang.income.service;

import com.likelionknu.dojang.income.data.MedianIncome;
import com.likelionknu.dojang.income.data.dto.response.IncomeBandResponseDto;
import com.likelionknu.dojang.income.data.dto.response.IncomeBandsResponseDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class IncomeBandService {
    private final MedianIncome medianIncome;

    public IncomeBandService(JsonMapper jsonMapper) throws IOException {
        this.medianIncome = jsonMapper.readValue(new ClassPathResource("data/median_income_2026.json").getInputStream(), MedianIncome.class);
    }

    public IncomeBandsResponseDto getIncomeBands(int household) {
        long median = medianWon(household);
        // 120%, 180% 금액은 원 미만 버림
        long le120 = median * 120 / 100;
        long le180 = median * 180 / 100;

        List<IncomeBandResponseDto> bands = List.of(
                new IncomeBandResponseDto("le120", "월 %,d원 이하".formatted(le120), le120),
                new IncomeBandResponseDto("le180", "월 %,d원 이하".formatted(le180), le180),
                new IncomeBandResponseDto("over180", "그보다 많음", null),
                new IncomeBandResponseDto("unknown", "잘 모르겠어요", null)
        );
        return new IncomeBandsResponseDto(medianIncome.year(), household, bands, medianIncome.source(), medianIncome.checkedAt());
    }

    private long medianWon(int household) {
        Map<Integer, Long> table = medianIncome.monthlyWon();
        if (table.containsKey(household)) {
            return table.get(household);
        }
        int largest = Collections.max(table.keySet());
        return table.get(largest) + medianIncome.incrementWon() * (household - largest);
    }
}
