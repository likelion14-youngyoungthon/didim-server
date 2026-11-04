package com.likelionknu.dojang.common.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** API 문서: /swagger-ui.html */
@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("도장 API")
                .description("장애 등록 후 남은 복지 신청 안내 서비스")
                .version("v1.0.0"));
    }
}
