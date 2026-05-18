create table departments (
    id bigint not null auto_increment,
    created_at timestamp not null,
    updated_at timestamp not null,
    hospital_id bigint not null,
    name varchar(120) not null,
    code varchar(40) not null,
    enabled bit not null,
    primary key (id),
    constraint fk_departments_hospital foreign key (hospital_id) references hospitals (id),
    constraint uk_departments_hospital_name unique (hospital_id, name),
    constraint uk_departments_hospital_code unique (hospital_id, code)
);

create table doctors (
    id bigint not null auto_increment,
    created_at timestamp not null,
    updated_at timestamp not null,
    hospital_id bigint not null,
    department_id bigint,
    first_name varchar(80) not null,
    last_name varchar(80) not null,
    email varchar(150) not null,
    phone_number varchar(40),
    specialty varchar(120),
    enabled bit not null,
    primary key (id),
    constraint fk_doctors_hospital foreign key (hospital_id) references hospitals (id),
    constraint fk_doctors_department foreign key (department_id) references departments (id),
    constraint uk_doctors_hospital_email unique (hospital_id, email)
);

alter table referrals
    add column assigned_department_id bigint;

alter table referrals
    add column assigned_doctor_id bigint;

alter table referrals
    add constraint fk_referrals_assigned_department foreign key (assigned_department_id) references departments (id);

alter table referrals
    add constraint fk_referrals_assigned_doctor foreign key (assigned_doctor_id) references doctors (id);

create index idx_departments_hospital on departments (hospital_id);
create index idx_doctors_hospital on doctors (hospital_id);
create index idx_doctors_department on doctors (department_id);
create index idx_referrals_assigned_department on referrals (assigned_department_id);
create index idx_referrals_assigned_doctor on referrals (assigned_doctor_id);
