package com.bds.backend.auth.domain.repository;

import com.bds.backend.auth.domain.entity.AuthLocal;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AuthLocalRepository extends JpaRepository<AuthLocal, Long> {

}
