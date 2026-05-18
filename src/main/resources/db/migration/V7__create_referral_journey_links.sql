alter table referrals
    add column journey_code varchar(60);

alter table referrals
    add column parent_referral_id bigint;

update referrals
set journey_code = concat('JRNY-LEGACY-', id)
where journey_code is null;

alter table referrals
    modify column journey_code varchar(60) not null;

alter table referrals
    add constraint fk_referrals_parent_referral foreign key (parent_referral_id) references referrals (id);

create index idx_referrals_journey_code on referrals (journey_code);
create index idx_referrals_parent_referral on referrals (parent_referral_id);
