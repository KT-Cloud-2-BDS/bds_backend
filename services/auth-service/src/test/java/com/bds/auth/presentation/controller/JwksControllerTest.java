package com.bds.auth.presentation.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bds.auth.application.JwksService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = JwksController.class, excludeAutoConfiguration = OAuth2ClientWebSecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("JwksController 단위 테스트")
class JwksControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwksService jwksService;

    @Test
    @DisplayName("GET /oauth2/jwks는 공개키 셋을 JWK Set JSON 형식으로 반환한다")
    void jwks_조회_성공() throws Exception {
        JWKSet jwkSet = new JWKSet(new RSAKeyGenerator(2048).keyID("test-kid").generate().toPublicJWK());
        given(jwksService.getPublicKeySet()).willReturn(jwkSet);

        mockMvc.perform(get("/oauth2/jwks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.keys").isArray())
            .andExpect(jsonPath("$.keys[0].kid").value("test-kid"));
    }
}
