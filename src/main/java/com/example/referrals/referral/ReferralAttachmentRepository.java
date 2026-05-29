package com.example.referrals.referral;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReferralAttachmentRepository extends JpaRepository<ReferralAttachment, Long> {

    List<ReferralAttachment> findAllByReferralIdOrderByCreatedAtDesc(Long referralId);

    @Query("""
        select a
        from ReferralAttachment a
        join fetch a.referral r
        left join fetch r.assignedDoctor ad
        left join fetch r.assignedDepartment ap
        left join fetch r.fromHospital fh
        left join fetch r.toHospital th
        where a.id = :attachmentId
        """)
    Optional<ReferralAttachment> findByIdWithReferral(Long attachmentId);
}
