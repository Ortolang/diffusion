
    create table Record (
        key varchar(255) not null,
        metadataPrefix varchar(255),
        lastModificationDate int8 not null,
        xml text,
        version int8 not null,
        primary key (key)
    );

    create table Set (
        id varchar(255) not null,
        name varchar(255),
        version int8 not null,
        primary key (id)
    );

    create table SetRecord (
        id varchar(255) not null,
        keyset varchar(255),
        record varchar(255),
        version int8 not null,
        primary key (id)
    );

