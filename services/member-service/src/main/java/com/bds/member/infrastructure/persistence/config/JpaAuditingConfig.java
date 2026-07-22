package com.bds.member.infrastructure.persistence.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * @EnableJpaAuditing이 @SpringBootApplication 클래스에 직접 있으면
 * @WebMvcTest 슬라이스 테스트에서도 적용되어, JPA가 로드되지 않은 컨텍스트에서
 * jpaMappingContext를 찾지 못해 컨텍스트 로딩이 깨진다. 별도 설정 클래스로 분리해
 * 컴포넌트 스캔 대상에서만 활성화되도록 한다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
