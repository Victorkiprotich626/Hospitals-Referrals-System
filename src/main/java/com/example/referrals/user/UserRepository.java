package com.example.referrals.user;

import com.example.referrals.common.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    @Query("""
        select distinct u
        from AppUser u
        join fetch u.roles r
        left join fetch u.hospital h
        left join fetch u.department d
        left join fetch u.doctorProfile dp
        where lower(u.email) = lower(:email)
        """)
    Optional<AppUser> findAuthenticationUserByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
