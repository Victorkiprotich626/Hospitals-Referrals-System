package com.example.referrals.referral;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReferralEventRepository extends JpaRepository<ReferralEvent, Long> {

    List<ReferralEvent> findAllByReferralIdOrderByCreatedAtDesc(Long referralId);

    List<ReferralEvent> findAllByReferralIdAndEventTypeOrderByCreatedAtDesc(Long referralId, ReferralEventType eventType);

    @Query("""
        select e
        from ReferralEvent e
        join fetch e.referral r
        join fetch r.fromHospital fh
        join fetch r.toHospital th
        where r.journeyCode = :journeyCode
          and exists (
            select 1
            from Referral journeyReferral
            where journeyReferral.journeyCode = r.journeyCode
              and (journeyReferral.fromHospital.id = :hospitalId or journeyReferral.toHospital.id = :hospitalId)
          )
        order by e.createdAt desc
        """)
    List<ReferralEvent> findJourneyTimelineVisibleToHospital(String journeyCode, Long hospitalId);
}
