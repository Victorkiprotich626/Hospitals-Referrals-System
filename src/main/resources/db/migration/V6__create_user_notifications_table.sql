create table user_notifications (
    id bigint not null auto_increment,
    created_at timestamp not null,
    updated_at timestamp not null,
    recipient_user_id bigint not null,
    referral_id bigint,
    type varchar(40) not null,
    title varchar(180) not null,
    message varchar(1200) not null,
    link varchar(255),
    read_at timestamp,
    primary key (id),
    constraint fk_user_notifications_recipient foreign key (recipient_user_id) references app_users (id) on delete cascade,
    constraint fk_user_notifications_referral foreign key (referral_id) references referrals (id) on delete set null
);

create index idx_user_notifications_recipient on user_notifications (recipient_user_id);
create index idx_user_notifications_read on user_notifications (recipient_user_id, read_at);
