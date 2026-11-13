package com.likelionknu.dojang.judge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ServiceListControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;

    private JsonNode services(String age) throws Exception {
        String body = mvc.perform(get("/api/v1/services").param("age", age))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").isEmpty())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).path("data");
    }

    private List<String> ids(JsonNode services) {
        List<String> ids = new ArrayList<>();
        services.forEach(service -> ids.add(service.path("id").asString()));
        return ids;
    }

    @Test void 네살은_따로_신청할_복지_여덟개를_규칙표_순서로_반환한다() throws Exception {
        JsonNode services = services("4");
        assertEquals(List.of("child_allowance", "dev_rehab_voucher", "family_care_support", "dd_parent_counsel",
                "dd_family_rest", "disabled_childcare_fee", "special_education", "special_transport"), ids(services));
        assertEquals("장애아동수당", services.get(0).path("name").asString());
        assertTrue(services.get(0).path("category").isNull());
        assertEquals("therapy", services.get(1).path("category").asString());
        services.forEach(service -> {
            assertEquals(3, service.size());
            assertTrue(service.path("id").isString());
            assertTrue(service.path("name").isString());
            assertTrue(service.path("category").isString() || service.path("category").isNull());
        });
    }

    @Test void 나이_경계와_등록신청서_항목을_규칙표대로_처리한다() throws Exception {
        List<String> age0 = ids(services("0"));
        assertFalse(age0.contains("sports_voucher"));
        assertTrue(ids(services("5")).contains("sports_voucher"));
        assertTrue(ids(services("17")).contains("child_allowance"));
        assertFalse(ids(services("17")).contains("pension_basic"));
        List<String> adult18 = ids(services("18"));
        assertTrue(adult18.contains("pension_basic"));
        assertFalse(adult18.contains("child_allowance"));
        assertFalse(adult18.contains("self_reliance_loan"));
        assertTrue(ids(services("19")).contains("self_reliance_loan"));
        assertTrue(ids(services("69")).contains("sports_voucher"));
        assertFalse(ids(services("70")).contains("sports_voucher"));
        for (String id : List.of("dd_individual_plan", "transport_discounts", "employment_package")) {
            assertFalse(age0.contains(id));
            assertFalse(adult18.contains(id));
        }
    }

    @Test void 잘못된_나이는_명세의_오류를_반환한다() throws Exception {
        mvc.perform(get("/api/v1/services"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"))
                .andExpect(jsonPath("$.message").value("age: 값이 필요합니다."));
        mvc.perform(get("/api/v1/services").param("age", "-1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("age: 0 이상이어야 합니다."));
        for (String age : List.of("abc", "4.5", "2147483648")) {
            mvc.perform(get("/api/v1/services").param("age", age))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("age: 형식이 올바르지 않습니다."));
        }
        mvc.perform(post("/api/v1/services"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("E405"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 요청 방식입니다."));
    }
}
