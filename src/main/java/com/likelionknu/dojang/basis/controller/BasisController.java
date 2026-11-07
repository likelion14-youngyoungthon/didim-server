package com.likelionknu.dojang.basis.controller;

import com.likelionknu.dojang.basis.data.dto.response.BasisResponseDto;
import com.likelionknu.dojang.basis.service.BasisService;
import com.likelionknu.dojang.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class BasisController {
    // 값은 /judge 허용값(영문 코드)만 받는다. AI에 그대로 넘기므로 문장은 막는다
    private static final String CODE = "[a-z0-9_]+";
    private static final String CODE_MESSAGE = "허용값이 아닙니다.";

    private final BasisService basisService;

    @GetMapping("/{id}/basis")
    @Operation(summary = "카드 상세 근거 (RAG)", description = "사용자 값은 /judge 요청과 같은 값. AI 호출에 실패하면 E502")
    public GlobalResponse<BasisResponseDto> getBasis(
            @Parameter(example = "dev_rehab_voucher") @PathVariable String id,
            @Parameter(example = "4") @RequestParam(required = false) @Min(0) @Max(120) Integer age,
            @Parameter(example = "autism") @RequestParam(name = "disability_type", required = false) @Pattern(regexp = CODE, message = CODE_MESSAGE) String disabilityType,
            @RequestParam(required = false) @Pattern(regexp = CODE, message = CODE_MESSAGE) String severity,
            @RequestParam(required = false) @Pattern(regexp = CODE, message = CODE_MESSAGE) String status,
            @RequestParam(name = "income_band", required = false) @Pattern(regexp = CODE, message = CODE_MESSAGE) String incomeBand,
            @RequestParam(required = false) List<@Pattern(regexp = CODE, message = CODE_MESSAGE) String> tags) {
        Map<String, Object> facts = new LinkedHashMap<>();
        putIfPresent(facts, "age", age);
        putIfPresent(facts, "disability_type", disabilityType);
        putIfPresent(facts, "severity", severity);
        putIfPresent(facts, "status", status);
        putIfPresent(facts, "income_band", incomeBand);
        putIfPresent(facts, "tags", tags);
        return GlobalResponse.ok(basisService.basis(id, facts));
    }

    private static void putIfPresent(Map<String, Object> facts, String key, Object value) {
        if (value != null) {
            facts.put(key, value);
        }
    }
}
