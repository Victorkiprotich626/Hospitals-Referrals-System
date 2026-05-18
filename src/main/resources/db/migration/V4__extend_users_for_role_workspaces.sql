alter table app_users
    add column department_id bigint;

alter table app_users
    add column doctor_profile_id bigint;

alter table app_users
    add constraint fk_app_users_department foreign key (department_id) references departments (id);

alter table app_users
    add constraint fk_app_users_doctor_profile foreign key (doctor_profile_id) references doctors (id);

create index idx_app_users_department on app_users (department_id);
create index idx_app_users_doctor_profile on app_users (doctor_profile_id);
