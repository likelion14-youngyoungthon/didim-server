package com.likelionknu.dojang.update.data.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** 변경안 목록. 바로 반영하지 않고 화면에서 사용자 확인을 받는다. 찾은 게 없으면 빈 목록 */
public record UpdateResponseDto(List<ChangeDto> changes) {

    /**
     * type별로 쓰는 필드만 채운다.
     * status: service_id, from, to / supplement: service_id, reopen_step / correct_service: from_service, to_service
     * correct_person: service_id, person / fact: key, to (needs_confirm true)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChangeDto(String type, String serviceId, String from, String to, String reopenStep,
                            String fromService, String toService, String person, String key,
                            boolean needsConfirm, String evidence) {
    }
}
