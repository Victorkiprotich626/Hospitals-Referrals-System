create table patients (
    id bigint not null auto_increment,
    created_at timestamp not null,
    updated_at timestamp not null,
    hospital_id bigint not null,
    patient_number varchar(40) not null,
    first_name varchar(80) not null,
    last_name varchar(80) not null,
    gender varchar(20) not null,
    date_of_birth date not null,
    phone_number varchar(40),
    national_id varchar(40),
    primary key (id),
    constraint fk_patients_hospital foreign key (hospital_id) references hospitals (id),
    constraint uk_patients_hospital_number unique (hospital_id, patient_number)
);

create table referrals (
    id bigint not null auto_increment,
    created_at timestamp not null,
    updated_at timestamp not null,
    reference_number varchar(50) not null,
    patient_id bigint not null,
    from_hospital_id bigint not null,
    to_hospital_id bigint not null,
    status varchar(30) not null,
    priority varchar(20) not null,
    subject varchar(180) not null,
    referral_reason varchar(1200) not null,
    clinical_summary varchar(2500),
    receiving_department varchar(150),
    primary key (id),
    constraint uk_referrals_reference unique (reference_number),
    constraint fk_referrals_patient foreign key (patient_id) references patients (id),
    constraint fk_referrals_from_hospital foreign key (from_hospital_id) references hospitals (id),
    constraint fk_referrals_to_hospital foreign key (to_hospital_id) references hospitals (id)
);

create table referral_events (
    id bigint not null auto_increment,
    created_at timestamp not null,
    updated_at timestamp not null,
    referral_id bigint not null,
    event_type varchar(30) not null,
    previous_status varchar(30),
    new_status varchar(30),
    actor_name varchar(160) not null,
    actor_hospital_name varchar(160),
    details varchar(2000) not null,
    primary key (id),
    constraint fk_referral_events_referral foreign key (referral_id) references referrals (id) on delete cascade
);

create index idx_patients_hospital on patients (hospital_id);
create index idx_referrals_patient on referrals (patient_id);
create index idx_referrals_from_hospital on referrals (from_hospital_id);
create index idx_referrals_to_hospital on referrals (to_hospital_id);
create index idx_referrals_status on referrals (status);
create index idx_referral_events_referral on referral_events (referral_id);
