package com.bds.chat.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
  @EnableWebSecurity
   public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
       // REST는 API 게이트웨이에서 인증, WebSocket은 STOMP 채널 인터셉터에서 인증
            .csrf(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                    // WebSocket 핸드셰이크: 익명 허용 설계이므로 개방 (인증은 STOMP CONNECT에서)
                    .requestMatchers("/ws/chat/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    // 그 외 REST(메시지 동기화 조회 등): ALB 직행 우회 대비 JWT 필수
                    .anyRequest().authenticated())
            .oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()))
            .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.auth.jwks-uri}") String jwksUri,
                                 @Value("${app.auth.issuer:}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        if (!issuer.isBlank()) {
            decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        }
        return decoder;
    }
}