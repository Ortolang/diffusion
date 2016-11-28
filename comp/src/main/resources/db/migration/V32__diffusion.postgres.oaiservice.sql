
    create table Record (
        id varchar(255) not null,
        identifier varchar(255) not null,
        metadataPrefix varchar(255),
        lastModificationDate int8 not null,
        xml text,
        version int8 not null,
        primary key (id)
    );

    create table Record_sets (
        Record_id varchar(255) not null,
        sets varchar(8000)
    );

    create table Set (
        spec varchar(255) not null,
        name varchar(255),
        version int8 not null,
        primary key (spec)
    );
