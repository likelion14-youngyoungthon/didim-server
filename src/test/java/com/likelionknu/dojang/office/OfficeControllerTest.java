package com.likelionknu.dojang.office;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OfficeControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void 동_행정복지센터가_맨_앞에_온다() throws Exception {
        mvc.perform(get("/api/v1/offices").param("dong", "백암면"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value("dong_center"))
                .andExpect(jsonPath("$.data[0].name").value("백암면 행정복지센터"))
                .andExpect(jsonPath("$.data[0].kind").value("접수처"))
                .andExpect(jsonPath("$.data[0].address").value("경기도 용인시 처인구 백암면 백암로 189"))
                .andExpect(jsonPath("$.data[0].phone").value("031-6193-5792"))
                .andExpect(jsonPath("$.data[0].lat").value(37.16396))
                .andExpect(jsonPath("$.data[0].checked_at").value("2026-11-07"));
    }

    @Test
    void 서비스별_접수처() throws Exception {
        mvc.perform(get("/api/v1/offices").param("dong", "백암면").param("service", "dev_rehab_voucher"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("백암면 행정복지센터"));
        mvc.perform(get("/api/v1/offices").param("dong", "백암면").param("service", "employment_package"))
                .andExpect(jsonPath("$.data[0].id").value("kead"))
                .andExpect(jsonPath("$.data[0].phone").value("1588-1519"));
        mvc.perform(get("/api/v1/offices").param("dong", "백암면").param("service", "car_excise_tax"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void 기흥_수지구도_좌표가_있다() throws Exception {
        mvc.perform(get("/api/v1/offices").param("dong", "성복동").param("service", "dev_rehab_voucher"))
                .andExpect(jsonPath("$.data[0].lat").isNumber())
                .andExpect(jsonPath("$.data[0].lng").isNumber());
    }

    @Test
    void 이용기관은_아직_빈_목록() throws Exception {
        mvc.perform(get("/api/v1/offices").param("dong", "성복동").param("kind", "이용기관"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void 잘못된_요청() throws Exception {
        mvc.perform(get("/api/v1/offices").param("dong", "강남동"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));
        mvc.perform(get("/api/v1/offices").param("dong", "백암면").param("kind", "병원"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/offices").param("dong", "백암면").param("service", "nothing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E404"));
        mvc.perform(get("/api/v1/offices"))
                .andExpect(status().isBadRequest());
    }
}
