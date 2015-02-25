create table Profile_infos (
        Profile_id varchar(255) not null,
        name varchar(255),
        source varchar(255),
        type int4,
        value varchar(7500),
        visibility int4,
        infos_KEY varchar(255),
        primary key (Profile_id, infos_KEY)
    );

alter table Profile_infos 
        add constraint FK_5dmu1n4w0bx2vaucrqr4mog87 
        foreign key (Profile_id) 
        references Profile;
