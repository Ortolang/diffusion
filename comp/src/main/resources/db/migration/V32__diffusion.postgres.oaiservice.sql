
    create table Record (
        id varchar(255) not null,
        identifier varchar(255) not null,
        metadataPrefix varchar(255),
        lastModificationDate int8 not null,
        xml text,
        version int8 not null,
        primary key (id)
    );

    create table Set (
        spec varchar(255) not null,
        name varchar(255),
        version int8 not null,
        primary key (spec)
    );

    create table SetRecord (
        id varchar(255) not null,
        setSpec varchar(255),
        recordId varchar(255),
        version int8 not null,
        primary key (id)
    );

