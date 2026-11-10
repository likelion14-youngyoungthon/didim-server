package com.likelionknu.dojang.update.controller;

import com.likelionknu.dojang.common.response.GlobalResponse;
import com.likelionknu.dojang.update.data.dto.request.UpdateRequestDto;
import com.likelionknu.dojang.update.data.dto.response.UpdateResponseDto;
import com.likelionknu.dojang.update.service.UpdateService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/updates")
@RequiredArgsConstructor
public class UpdateController {
    private final UpdateService updateService;

    @PostMapping
    @Operation(summary = "대화로 상태 변경 추출", description = "변경안만 돌려주고 저장하지 않는다. AI 호출에 실패하면 E502")
    public GlobalResponse<UpdateResponseDto> extract(@Valid @RequestBody UpdateRequestDto request) {
        return GlobalResponse.ok(updateService.extract(request));
    }
}
