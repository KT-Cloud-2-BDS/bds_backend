package com.bds.chat.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
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
       .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
       .build();
       }
}