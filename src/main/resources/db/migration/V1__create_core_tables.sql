create table hospitals (
    id bigint not null auto_increment,
    created_at timestamp not null,
    updated_at timestamp not null,
    name varchar(120) not null,
    code varchar(40) not null,
    contact_email varchar(120),
    contact_phone varchar(40),
    address varchar(255),
    enabled bit not null,
    primary key (id),
    constraint uk_hospitals_name unique (name),
    constraint uk_hospitals_code unique (code)
);

create table app_users (
    id bigint not null auto_increment,
    created_at timestamp not null,
    updated_at timestamp not null,
    first_name varchar(80) not null,
    last_name varchar(80) not null,
    email varchar(150) not null,
    password_hash varchar(255) not null,
    enabled bit not null,
    account_locked bit not null,
    hospital_id bigint null,
    primary key (id),
    constraint uk_app_users_email unique (email),
    constraint fk_app_users_hospital foreign key (hospital_id) references hospitals (id)
);

create table user_roles (
    user_id bigint not null,
    role_name varchar(50) not null,
    constraint fk_user_roles_user foreign key (user_id) references app_users (id) on delete cascade
);

create index idx_app_users_hospital_id on app_users (hospital_id);
create index idx_user_roles_role_name on user_roles (role_name);
