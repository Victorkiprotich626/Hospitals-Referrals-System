package com.example.referrals.referral;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    @Query("""
        select r
        from Referral r
        join fetch r.patient p
        join fetch r.fromHospital fh
        join fetch r.toHospital th
        left join fetch r.assignedDepartment ad
        left join fetch r.assignedDoctor ado
        left join fetch r.parentReferral pr
        where exists (
            select 1
            from Referral journeyReferral
            where journeyReferral.journeyCode = r.journeyCode
              and (journeyReferral.fromHospital.id = :hospitalId or journeyReferral.toHospital.id = :hospitalId)
        )
        order by r.updatedAt desc
        """)
    List<Referral> findAllVisibleToHospital(Long hospitalId);

    @Query("""
        select r
        from Referral r
        join fetch r.patient p
        join fetch r.fromHospital fh
        join fetch r.toHospital th
        left join fetch r.assignedDepartment ad
        left join fetch r.assignedDoctor ado
        left join fetch r.parentReferral pr
        where r.id = :referralId
          and exists (
            select 1
            from Referral journeyReferral
            where journeyReferral.journeyCode = r.journeyCode
              and (journeyReferral.fromHospital.id = :hospitalId or journeyReferral.toHospital.id = :hospitalId)
        )
        """)
    Optional<Referral> findVisibleById(Long referralId, Long hospitalId);

    @Query("""
        select r
        from Referral r
        join fetch r.patient p
        join fetch r.fromHospital fh
        join fetch r.toHospital th
        left join fetch r.assignedDepartment ad
        left join fetch r.assignedDoctor ado
        left join fetch r.parentReferral pr
        where r.journeyCode = :journeyCode
          and exists (
            select 1
            from Referral journeyReferral
            where journeyReferral.journeyCode = r.journeyCode
              and (journeyReferral.fromHospital.id = :hospitalId or journeyReferral.toHospital.id = :hospitalId)
        )
        order by r.createdAt asc
        """)
    List<Referral> findJourneyVisibleToHospital(String journeyCode, Long hospitalId);

    boolean existsByPatientId(Long patientId);

    boolean existsByAssignedDepartmentId(Long departmentId);

    boolean existsByAssignedDoctorId(Long doctorId);

    @Query("""
        select count(r)
        from Referral r
        where exists (
            select 1
            from Referral journeyReferral
            where journeyReferral.journeyCode = r.journeyCode
              and (journeyReferral.fromHospital.id = :hospitalId or journeyReferral.toHospital.id = :hospitalId)
        )
          and r.status not in :closedStatuses
        """)
    long countOpenVisibleToHospital(Long hospitalId, Set<ReferralStatus> closedStatuses);

    @Query("""
        select count(r)
        from Referral r
        where r.toHospital.id = :hospitalId
          and r.status in :activeStatuses
        """)
    long countIncomingByStatuses(Long hospitalId, Set<ReferralStatus> activeStatuses);

    @Query("""
        select r
        from Referral r
        join fetch r.patient p
        join fetch r.fromHospital fh
        join fetch r.toHospital th
        left join fetch r.assignedDepartment ad
        left join fetch r.assignedDoctor ado
        where ado.id = :doctorId
        order by r.updatedAt desc
        """)
    List<Referral> findAllAssignedToDoctor(Long doctorId);

    @Query("""
        select r
        from Referral r
        join fetch r.patient p
        join fetch r.fromHospital fh
        join fetch r.toHospital th
        left join fetch r.assignedDepartment ad
        left join fetch r.assignedDoctor ado
        where r.id = :referralId and ado.id = :doctorId
        """)
    Optional<Referral> findAssignedToDoctor(Long referralId, Long doctorId);

    @Query("""
        select count(r)
        from Referral r
        where r.assignedDoctor.id = :doctorId
          and r.status not in :closedStatuses
        """)
    long countOpenAssignedToDoctor(Long doctorId, Set<ReferralStatus> closedStatuses);
}
