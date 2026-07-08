package com.bds.auth.infrastructure.persistence.entity;


import com.bds.auth.domain.entity.Auth;
import com.bds.auth.domain.entity.enums.Role;
import com.bds.auth.domain.entity.enums.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "auth")
public class AuthJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public static AuthJpaEntity from(Auth auth) {
        return AuthJpaEntity.builder()
            .id(auth.getId())
            .email(auth.getEmail())
            .status(auth.getStatus())
            .role(auth.getRole())
            .build();
    }

}
