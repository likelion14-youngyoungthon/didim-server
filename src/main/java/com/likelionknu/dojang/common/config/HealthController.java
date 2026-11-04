package com.likelionknu.dojang.common.config;

import com.likelionknu.dojang.common.response.GlobalResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 배포 후 서버가 살아 있는지 확인용. */
@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public GlobalResponse<String> health() {
        return GlobalResponse.ok("ok");
    }
}
