create table referral_attachments (
    id bigint not null auto_increment,
    created_at timestamp not null,
    updated_at timestamp not null,
    referral_id bigint not null,
    original_file_name varchar(255) not null,
    stored_file_name varchar(255) not null,
    content_type varchar(120),
    file_size bigint not null,
    uploaded_by_name varchar(160) not null,
    primary key (id),
    constraint fk_referral_attachments_referral foreign key (referral_id) references referrals (id) on delete cascade
);

create index idx_referral_attachments_referral on referral_attachments (referral_id);
