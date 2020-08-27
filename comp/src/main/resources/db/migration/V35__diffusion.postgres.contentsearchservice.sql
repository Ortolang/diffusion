
    create table ContentSearchResource (
        id varchar(255) not null,
        workspace varchar(255) not null,
        pid varchar(255),
        title varchar(255),
        description varchar(255),
        landingPageURI varchar(255),
        version int8 not null,
        primary key (id)
    );

    create table ContentSearchResource_documents (
        ContentSearchResource_id varchar(255) not null,
        documents varchar(255)
    );
