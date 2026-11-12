package com.likelionknu.dojang.member;

import com.jayway.jsonpath.JsonPath;
import com.likelionknu.dojang.auth.client.KakaoClient;
import com.likelionknu.dojang.auth.client.KakaoClient.KakaoProfile;
import com.likelionknu.dojang.auth.service.JwtProvider;
import com.likelionknu.dojang.member.entity.Member;
import com.likelionknu.dojang.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MemberProgressControllerTest {
    private static final AtomicLong KAKAO_IDS = new AtomicLong(34000);
    private static final String PATH = "/api/v1/me/progress";
    private static final String SNAPSHOT = """
            {"profile":{"age":4,"disability_type":"autism","severity":"severe","dong":"백암면",
            "household":4,"status":"neither","income_band":"le120","reg_items":[],"reg_items_unknown":false,"tags":["daycare"]},
            "services":[{"id":"dev_rehab_voucher","status":"submitted"},{"id":"parking_sticker","status":"already"}]}
            """;
    @Autowired MockMvc mvc;
    @Autowired MemberRepository repository;
    @Autowired JwtProvider jwtProvider;
    @MockitoBean KakaoClient kakaoClient;
    private Member member;
    private String auth;

    @BeforeEach void member() {
        member = repository.saveAndFlush(Member.join(KAKAO_IDS.incrementAndGet()));
        auth = "Bearer " + jwtProvider.issue(member.getId());
    }

    private ResultActions save(String token, String json) throws Exception {
        return mvc.perform(put(PATH).header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(json));
    }
    private ResultActions read(String token) throws Exception {
        return mvc.perform(get(PATH).header("Authorization", token));
    }

    @Test void 실제_DB에_저장하고_동일회원_재로그인후_복원한다() throws Exception {
        save(auth, SNAPSHOT).andExpect(status().isOk());
        Member stored = repository.findById(member.getId()).orElseThrow();
        assertThat(stored.getProfile()).contains("백암면");
        assertThat(stored.getServices()).contains("submitted");
        assertThat(stored.getProgressUpdatedAt()).isNotNull();

        given(kakaoClient.fetchProfile(eq("progress-relogin"), anyString())).willReturn(new KakaoProfile(member.getKakaoId(), null));
        String login = mvc.perform(post("/api/v1/auth/kakao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"progress-relogin\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.is_new").value(false))
                .andReturn().getResponse().getContentAsString();
        String token = "Bearer " + JsonPath.read(login, "$.data.access_token");
        read(token).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.dong").value("백암면"))
                .andExpect(jsonPath("$.data.services[0].status").value("submitted"));
    }

    @Test void 다른_회원_저장과_조회가_섞이지_않는다() throws Exception {
        Member other = repository.saveAndFlush(Member.join(KAKAO_IDS.incrementAndGet()));
        String otherAuth = "Bearer " + jwtProvider.issue(other.getId());
        save(auth, SNAPSHOT).andExpect(status().isOk());
        read(otherAuth).andExpect(jsonPath("$.data.services.length()").value(0));
        save(otherAuth, SNAPSHOT.replace("백암면", "포곡읍").replace("submitted", "approved"))
                .andExpect(status().isOk());
        read(auth).andExpect(jsonPath("$.data.profile.dong").value("백암면"))
                .andExpect(jsonPath("$.data.services[0].status").value("submitted"));
        read(otherAuth).andExpect(jsonPath("$.data.profile.dong").value("포곡읍"))
                .andExpect(jsonPath("$.data.services[0].status").value("approved"));
    }

    @Test void 전체_덮어쓰기와_빈목록_초기화_계약을_유지한다() throws Exception {
        save(auth, SNAPSHOT).andExpect(status().isOk());
        save(auth, "{\"services\":[{\"id\":\"activity_support\",\"status\":\"in_use\"}]}")
                .andExpect(status().isOk());
        read(auth).andExpect(jsonPath("$.data.profile").isEmpty())
                .andExpect(jsonPath("$.data.services.length()").value(1))
                .andExpect(jsonPath("$.data.services[0].id").value("activity_support"));
        save(auth, "{\"profile\":null,\"services\":[]}").andExpect(status().isOk());
        read(auth).andExpect(jsonPath("$.data.services.length()").value(0))
                .andExpect(jsonPath("$.data.updated_at").isString());
    }

    @Test void 기존_상태와_진행상태를_변환하지_않고_보관한다() throws Exception {
        for (String state : new String[]{"apply","already","extra_form","check","auto","applied","submitted","approved","in_use"}) {
            save(auth, "{\"services\":[{\"id\":\"dev_rehab_voucher\",\"status\":\"" + state + "\"}]}")
                    .andExpect(status().isOk());
            read(auth).andExpect(jsonPath("$.data.services[0].status").value(state));
        }
        save(auth, "{\"services\":[{\"id\":\"dev_rehab_voucher\",\"status\":null}]}")
                .andExpect(status().isOk());
        read(auth).andExpect(jsonPath("$.data.services[0].status").isEmpty());
    }

    @Test void 신청한_날을_저장하고_없으면_null() throws Exception {
        save(auth, """
                {"services":[{"id":"dev_rehab_voucher","status":"submitted","submitted_at":"2026-11-12"},
                {"id":"parking_sticker","status":"already"}]}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.services[0].submitted_at").value("2026-11-12"));
        read(auth).andExpect(jsonPath("$.data.services[0].submitted_at").value("2026-11-12"))
                .andExpect(jsonPath("$.data.services[1].submitted_at").isEmpty());

        save(auth, "{\"services\":[{\"id\":\"dev_rehab_voucher\",\"submitted_at\":\"11월 12일\"}]}")
                .andExpect(status().isBadRequest());
    }

    @Test void 미등록_서비스와_임의상태_중복_null원소를_저장하지_않는다() throws Exception {
        for (String invalid : new String[]{
                "{\"services\":[{\"id\":\"missing_service\",\"status\":\"apply\"}]}",
                "{\"services\":[{\"id\":\"activity_support\",\"status\":\"unexpected\"}]}",
                "{\"services\":[{\"id\":\"activity_support\",\"status\":\"\"}]}",
                "{\"services\":[{\"id\":\"activity_support\"},{\"id\":\"activity_support\"}]}",
                "{\"services\":[null]}", "{\"services\":null}", "{}"}) {
            assertRejectedWithoutChange(invalid);
        }
    }

    @Test void 잘못된_프로필을_저장하지_않는다() throws Exception {
        for (String invalid : new String[]{
                SNAPSHOT.replace("\"age\":4", "\"age\":-1"),
                SNAPSHOT.replace("\"age\":4,", ""),
                SNAPSHOT.replace("\"household\":4", "\"household\":0"),
                SNAPSHOT.replace("autism", "fake_type"), SNAPSHOT.replace("백암면", "없는동"),
                SNAPSHOT.replace("severe", "fake_severity"), SNAPSHOT.replace("le120", "fake_band"),
                SNAPSHOT.replace("neither", "fake_benefit"), SNAPSHOT.replace("daycare", "fake_tag"),
                SNAPSHOT.replace("\"reg_items\":[]", "\"reg_items\":[\"fake_item\"]")}) {
            assertRejectedWithoutChange(invalid);
        }
    }

    @Test void 미확인_조건은_그대로_복원한다() throws Exception {
        save(auth, SNAPSHOT.replace("autism", "unknown").replace("severe", "unknown")
                .replace("neither", "unknown").replace("le120", "unknown"))
                .andExpect(status().isOk());
        read(auth).andExpect(jsonPath("$.data.profile.income_band").value("unknown"))
                .andExpect(jsonPath("$.data.services[0].status").value("submitted"));
    }

    @Test void 인증없거나_탈퇴한_회원은_저장할_수_없다() throws Exception {
        mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content(SNAPSHOT))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/me").header("Authorization",auth)).andExpect(status().isOk());
        save(auth,SNAPSHOT).andExpect(status().isUnauthorized());
    }

    private void assertRejectedWithoutChange(String invalid) throws Exception {
        save(auth, SNAPSHOT).andExpect(status().isOk());
        String before = read(auth).andReturn().getResponse().getContentAsString();
        save(auth, invalid).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        String after = read(auth).andReturn().getResponse().getContentAsString();
        assertThat(after).isEqualTo(before);
    }
}
