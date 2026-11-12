package com.likelionknu.dojang.member.service;

import com.likelionknu.dojang.judge.service.JudgeService;
import com.likelionknu.dojang.member.data.dto.request.ProgressRequestDto;
import com.likelionknu.dojang.member.exception.MemberException;
import com.likelionknu.dojang.office.service.OfficeService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 저장할 값의 계약만 검증하며 상태 전환이나 판정 결과를 생성하지 않는다. */
@Component
public class MemberProgressValidator {
    private static final Set<String> STATUSES = Set.of(
            "apply", "already", "extra_form", "check", "auto",
            "submitted", "approved", "in_use", "applied");

    private final Set<String> serviceIds;
    private final JudgeService judgeService;
    private final OfficeService officeService;

    public MemberProgressValidator(JsonMapper mapper, JudgeService judgeService, OfficeService officeService) throws IOException {
        this.judgeService = judgeService;
        this.officeService = officeService;
        Set<String> ids = new HashSet<>();
        for (String path : List.of("data/rules_child_v2.json", "data/rules_adult_v2.json")) {
            try (var input = new ClassPathResource(path).getInputStream()) {
                for (JsonNode rule : mapper.readTree(input).path("services")) {
                    ids.add(rule.path("id").asString());
                }
            }
        }
        serviceIds = Set.copyOf(ids);
    }

    public void validate(ProgressRequestDto request) {
        if (request == null || request.services() == null) {
            throw new MemberException("services: 카드 목록이 필요합니다.");
        }
        if (request.profile() != null) {
            judgeService.validateRequest(request.profile());
            officeService.getOffices(request.profile().dong(), null, null);
        }
        Set<String> seen = new HashSet<>();
        for (var service : request.services()) {
            if (service == null) throw new MemberException("services: null 항목은 허용되지 않습니다.");
            if (service.id() == null || !serviceIds.contains(service.id())) {
                throw new MemberException("services.id: 없는 서비스입니다.");
            }
            if (!seen.add(service.id())) throw new MemberException("services.id: 중복된 서비스입니다.");
            if (service.status() != null && !STATUSES.contains(service.status())) {
                throw new MemberException("services.status: 허용되지 않은 상태입니다.");
            }
        }
    }
}
