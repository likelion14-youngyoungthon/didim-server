package com.likelionknu.dojang.stt.controller;

import com.likelionknu.dojang.common.response.GlobalResponse;
import com.likelionknu.dojang.stt.data.dto.response.SttResponseDto;
import com.likelionknu.dojang.stt.service.SttService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/stt")
@RequiredArgsConstructor
public class SttController {
    private final SttService sttService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "음성을 텍스트로 변환", description = "audio 필드로 WebM 또는 WAV 파일을 보냅니다(최대 10MiB). "
            + "컨테이너 헤더를 확인하며 실제 디코딩은 음성 변환 제공자가 처리합니다. "
            + "텍스트만 반환하며 대화 분석이나 상태 변경을 수행하지 않습니다. AI 호출 실패는 E502입니다.")
    public GlobalResponse<SttResponseDto> transcribe(
            @Parameter(description = "WebM/WAV 음성 파일", schema = @Schema(type = "string", format = "binary"))
            @RequestPart("audio") MultipartFile audio) {
        return GlobalResponse.ok(sttService.transcribe(audio));
    }
}
