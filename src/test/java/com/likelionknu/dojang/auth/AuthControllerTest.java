package com.likelionknu.dojang.auth;

import com.likelionknu.dojang.auth.client.KakaoClient;
import com.likelionknu.dojang.auth.client.KakaoClient.KakaoProfile;
import com.likelionknu.dojang.auth.exception.AuthException;
import com.likelionknu.dojang.common.response.ErrorCode;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean KakaoClient kakaoClient;

    private static final String PROGRESS = """
            {"profile":{"age":4,"disability_type":"autism","severity":"severe","dong":"백암면","household":4},
             "services":[{"id":"dev_rehab_voucher","status":"applied"},{"id":"parking_sticker","status":"apply"}]}
            """;

    private ResultActions login(String code) throws Exception {
        return mvc.perform(post("/api/v1/auth/kakao").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"));
    }

    private String token(long kakaoId, String code) throws Exception {
        given(kakaoClient.fetchProfile(eq(code), anyString())).willReturn(new KakaoProfile(kakaoId, null));
        String body = login(code).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.data.access_token");
    }

    @Test
    void 처음_로그인하면_회원이_생기고_다시_하면_같은_회원() throws Exception {
        given(kakaoClient.fetchProfile(eq("first"), anyString())).willReturn(new KakaoProfile(1001L, "김멋사"));
        login("first").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").isString())
                .andExpect(jsonPath("$.data.token_type").value("Bearer"))
                .andExpect(jsonPath("$.data.expires_in").value(604800))
                .andExpect(jsonPath("$.data.is_new").value(true))
                .andExpect(jsonPath("$.data.nickname").value("김멋사"))
                .andExpect(jsonPath("$.data.is_demo").value(false));
        login("first").andExpect(jsonPath("$.data.is_new").value(false));
        verify(kakaoClient, org.mockito.Mockito.times(2)).fetchProfile("first", "http://localhost:5173/oauth/kakao");
    }

    @Test
    void 저장하고_불러오기() throws Exception {
        String auth = token(1002L, "save");
        mvc.perform(get("/api/v1/me/progress").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile").isEmpty())
                .andExpect(jsonPath("$.data.services.length()").value(0))
                .andExpect(jsonPath("$.data.updated_at").isEmpty());

        mvc.perform(put("/api/v1/me/progress").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(PROGRESS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated_at").isString());

        mvc.perform(get("/api/v1/me/progress").header("Authorization", auth))
                .andExpect(jsonPath("$.data.profile.age").value(4))
                .andExpect(jsonPath("$.data.profile.dong").value("백암면"))
                .andExpect(jsonPath("$.data.services[0].id").value("dev_rehab_voucher"))
                .andExpect(jsonPath("$.data.services[0].status").value("applied"))
                .andExpect(jsonPath("$.data.services.length()").value(2));
    }

    @Test
    void 탈퇴하면_데이터가_지워지고_토큰도_못_쓴다() throws Exception {
        String auth = token(1003L, "bye");
        mvc.perform(put("/api/v1/me/progress").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(PROGRESS));
        mvc.perform(delete("/api/v1/me").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"));
        mvc.perform(get("/api/v1/me/progress").header("Authorization", auth))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E401"));

        given(kakaoClient.fetchProfile(eq("again"), anyString())).willReturn(new KakaoProfile(1003L, null));
        login("again").andExpect(jsonPath("$.data.is_new").value(true));
    }

    @Test
    void 토큰이_없거나_잘못되면_401() throws Exception {
        mvc.perform(get("/api/v1/me/progress"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("E401"))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        mvc.perform(get("/api/v1/me/progress").header("Authorization", "Bearer abc.def.ghi"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/me").header("Authorization", "Basic xxx"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void 카카오_실패와_잘못된_요청() throws Exception {
        given(kakaoClient.fetchProfile(eq("expired"), anyString()))
                .willThrow(new AuthException(ErrorCode.UNAUTHORIZED, "카카오 인증에 실패했습니다. 다시 로그인해 주세요."));
        login("expired").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("카카오 인증에 실패했습니다. 다시 로그인해 주세요."));

        given(kakaoClient.fetchProfile(eq("down"), anyString())).willThrow(new AuthException(ErrorCode.KAKAO_FAILED));
        login("down").andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("E502"));

        login("").andExpect(status().isBadRequest());

        String auth = token(1004L, "bad");
        mvc.perform(put("/api/v1/me/progress").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"services\":[{\"id\":\"DROP TABLE\"}]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/me/progress").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 체험_로그인은_부를_때마다_새_임시_회원() throws Exception {
        String body = mvc.perform(post("/api/v1/auth/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").isString())
                .andExpect(jsonPath("$.data.is_demo").value(true))
                .andExpect(jsonPath("$.data.nickname").isEmpty())
                .andReturn().getResponse().getContentAsString();
        String auth = "Bearer " + JsonPath.read(body, "$.data.access_token");
        mvc.perform(put("/api/v1/me/progress").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content(PROGRESS))
                .andExpect(status().isOk());

        String again = mvc.perform(post("/api/v1/auth/demo")).andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/v1/me/progress").header("Authorization", "Bearer " + JsonPath.read(again, "$.data.access_token")))
                .andExpect(jsonPath("$.data.services.length()").value(0));
    }

    @Test
    void 처음_로그인이_동시에_와도_회원은_하나() throws Exception {
        given(kakaoClient.fetchProfile(eq("race"), anyString())).willReturn(new KakaoProfile(1005L, null));
        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(5);
        java.util.List<java.util.concurrent.Future<Integer>> results = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) {
            results.add(pool.submit(() -> login("race").andReturn().getResponse().getStatus()));
        }
        for (java.util.concurrent.Future<Integer> r : results) {
            org.assertj.core.api.Assertions.assertThat(r.get()).isEqualTo(200);
        }
        pool.shutdown();
        login("race").andExpect(jsonPath("$.data.is_new").value(false));
    }

    @Test
    void JSON이_아니면_400() throws Exception {
        String auth = token(1006L, "media");
        mvc.perform(put("/api/v1/me/progress").header("Authorization", auth).content("x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));
    }

    @Test
    void 브라우저_사전요청은_토큰_없이_통과() throws Exception {
        mvc.perform(options("/api/v1/me/progress")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "PUT")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk());
    }
}
