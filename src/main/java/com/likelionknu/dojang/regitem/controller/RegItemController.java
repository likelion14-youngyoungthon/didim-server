package com.likelionknu.dojang.regitem.controller;

import com.likelionknu.dojang.common.response.GlobalResponse;
import com.likelionknu.dojang.regitem.data.dto.response.RegItemResponseDto;
import com.likelionknu.dojang.regitem.service.RegItemService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reg-items")
@RequiredArgsConstructor
public class RegItemController {
    private final RegItemService regItemService;

    @GetMapping
    @Operation(summary = "장애인 등록 신청서 항목 목록")
    public GlobalResponse<List<RegItemResponseDto>> getRegItems() {
        return GlobalResponse.ok(regItemService.getRegItems());
    }
}
