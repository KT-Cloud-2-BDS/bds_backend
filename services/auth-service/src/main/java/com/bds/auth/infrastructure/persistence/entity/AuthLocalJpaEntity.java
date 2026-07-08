package com.bds.auth.infrastructure.persistence.entity;

import com.bds.auth.domain.entity.AuthLocal;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "auth_local")
public class AuthLocalJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 225)
    private String password;


    @Column(nullable = false)
    private Long authId;

    public static AuthLocalJpaEntity from(AuthLocal authLocal) {
        return AuthLocalJpaEntity.builder()
            .id(authLocal.getId())
            .password(authLocal.getPassword())
            .authId(authLocal.getAuthId())
            .build();
    }
}
