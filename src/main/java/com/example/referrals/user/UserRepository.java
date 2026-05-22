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

    @Query("""
        select distinct u
        from AppUser u
        join fetch u.roles r
        left join fetch u.hospital h
        where r = :role
        order by h.name asc, u.firstName asc, u.lastName asc
        """)
    List<AppUser> findAllByRole(RoleName role);

    @Query("""
        select distinct u
        from AppUser u
        join fetch u.roles r
        left join fetch u.department d
        left join fetch u.doctorProfile dp
        where u.hospital.id = :hospitalId
          and r in :roles
        order by u.firstName asc, u.lastName asc
        """)
    List<AppUser> findAllByHospitalIdAndRoles(Long hospitalId, Set<RoleName> roles);

    @Query("""
        select distinct u
        from AppUser u
        join fetch u.roles r
        left join fetch u.department d
        left join fetch u.doctorProfile dp
        where dp.id = :doctorProfileId
          and r in :roles
        order by u.firstName asc, u.lastName asc
        """)
    List<AppUser> findAllByDoctorProfileIdAndRoles(Long doctorProfileId, Set<RoleName> roles);

    long countByHospitalId(Long hospitalId);

    long countByHospitalIdAndEnabledTrue(Long hospitalId);
}
