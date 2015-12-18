
    create table OrganizationEntity (
        id varchar(255) not null,
        content text,
        version int8 not null,
        primary key (id)
    );

    create table PersonEntity (
        id varchar(255) not null,
        content text,
        organization varchar(255),
        version int8 not null,
        primary key (id)
    );
    
    create table StatusOfUseEntity (
        id varchar(255) not null,
        content text,
        version int8 not null,
        primary key (id)
    );

    create table TermEntity (
        id varchar(255) not null,
        content text,
        version int8 not null,
        primary key (id)
    );

    create table LicenseEntity (
        id varchar(255) not null,
        content text,
        statusOfUse varchar(255),
        version int8 not null,
        primary key (id)
    );
