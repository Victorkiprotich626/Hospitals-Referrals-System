alter table referral_attachments
    add column attachment_type varchar(40);

alter table referral_attachments
    add column note varchar(600);

alter table referral_attachments
    add column uploaded_by_role_name varchar(80);

update referral_attachments
set attachment_type = 'OTHER'
where attachment_type is null;

alter table referral_attachments
    modify column attachment_type varchar(40) not null;
