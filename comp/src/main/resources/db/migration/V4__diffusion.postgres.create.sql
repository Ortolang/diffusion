    create table AuthorisationPolicy (
        id varchar(255) not null,
        owner varchar(255),
        rulesContent text,
        primary key (id)
    );

    create table AuthorisationPolicyTemplate (
        name varchar(255) not null,
        description varchar(255),
        template varchar(255),
        primary key (name)
    );

    create table Collection (
        id varchar(255) not null,
        clock int4 not null,
        metadatasContent text,
        name varchar(255),
        root boolean not null,
        version int8 not null,
        primary key (id)
    );

    create table Collection_segments (
        Collection_id varchar(255) not null,
        segments varchar(8000)
    );

    create table DataObject (
        id varchar(255) not null,
        clock int4 not null,
        metadatasContent text,
        mimeType varchar(255),
        name varchar(255),
        size int8 not null,
        stream varchar(255),
        version int8 not null,
        primary key (id)
    );

    create table Form (
        id varchar(255) not null,
        definition text,
        name varchar(255),
        version int8 not null,
        primary key (id)
    );

    create table Link (
        id varchar(255) not null,
        clock int4 not null,
        metadatasContent text,
        name varchar(255),
        target varchar(255),
        version int8 not null,
        primary key (id)
    );

    create table MetadataFormat (
        id varchar(255) not null,
        description varchar(2500),
        form varchar(255),
        mimeType varchar(255),
        name varchar(255),
        schema varchar(255),
        serial int4 not null,
        size int8 not null,
        version int8 not null,
        primary key (id)
    );

    create table MetadataObject (
        id varchar(255) not null,
        contentType varchar(255),
        format varchar(255),
        name varchar(255),
        size int8 not null,
        stream varchar(255),
        target varchar(255),
        version int8 not null,
        primary key (id)
    );

    create table Process (
        id varchar(255) not null,
        activity varchar(255),
        initier varchar(255),
        log text,
        name varchar(255),
        progress int4 not null,
        start int8 not null,
        state int4,
        stop int8 not null,
        type varchar(255),
        version int8 not null,
        workspace varchar(255),
        primary key (id)
    );

    create table Profile (
        id varchar(255) not null,
        email varchar(255),
        emailHash varchar(255),
        emailVerified boolean not null,
        emailVisibility int4,
        familyName varchar(255),
        friends varchar(255),
        givenName varchar(255),
        groupsList text,
        secret varchar(255),
        status int4,
        version int8 not null,
        primary key (id)
    );

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

    create table Profile_keys (
        Profile_id varchar(255) not null,
        key varchar(2500),
        password varchar(2500)
    );

    create table ReferentielEntity (
        id varchar(255) not null,
        content text,
        name varchar(255),
        status varchar(255),
        type int4,
        version int8 not null,
        primary key (id)
    );

    create table RegistryEntry (
        key varchar(255) not null,
        author varchar(255),
        children varchar(255),
        creationDate int8 not null,
        deleted boolean not null,
        hidden boolean not null,
        identifier varchar(255),
        lastModificationDate int8 not null,
        lock varchar(255),
        parent varchar(255),
        propertiesContent text,
        publicationStatus varchar(255),
        version int8 not null,
        primary key (key)
    );

    create table RemoteProcess (
        id varchar(255) not null,
        activity varchar(255),
        initier varchar(255),
        log text,
        progress int4 not null,
        start int8 not null,
        state int4,
        stop int8 not null,
        toolJobId varchar(255),
        toolKey varchar(255),
        toolName varchar(255),
        version int8 not null,
        primary key (id)
    );

    create table StatisticValue (
        name varchar(255) not null,
        timestamp int8 not null,
        value int8 not null,
        primary key (name, timestamp)
    );

    create table Workspace (
        id varchar(255) not null,
        alias varchar(255),
        changed boolean not null,
        clock int4 not null,
        head varchar(255),
        members varchar(255),
        name varchar(255),
        snapshotsContent text,
        tagsContent text,
        type varchar(255),
        version int8 not null,
        primary key (id)
    );

    create table WorkspaceAlias (
        id int8 not null,
        primary key (id)
    );

    create table "GROUP" (
        id varchar(255) not null,
        description varchar(2500),
        membersList text,
        name varchar(255),
        version int8 not null,
        primary key (id)
    );

    create table handles (
        handle bytea not null,
        idx int4 not null,
        admin_read boolean,
        admin_write boolean,
        data bytea,
        key varchar(255),
        pub_read boolean,
        pub_write boolean,
        refs text,
        timestamp int4,
        ttl int4,
        ttl_type int2,
        type bytea,
        primary key (handle, idx)
    );

    create table nas (
        na bytea not null,
        primary key (na)
    );

    create index UK_pm0pof2ncp68yq82tb498mfwd on Link (target);

    create index UK_qau89io9hls4vb0j9flmnvbry on MetadataObject (target);

    create index UK_1y5xufstwuf5398odn1vl3ykw on Process (initier, state);

    create index UK_8vv92rpgi9suidc96s1b88rw1 on RegistryEntry (identifier);

    create index UK_e8d1rgq9atuhoyan68niihj67 on RemoteProcess (initier, state);

    alter table Workspace 
        add constraint UK_48cyeq9y05tbu0dgn64iswitn  unique (alias);

    create index UK_m242bwr2hjd7k1lcy2dnsx1x9 on handles (handle, data);

    alter table Collection_segments 
        add constraint FK_qtkd8pouce5ufvwfwj4bgsk8u 
        foreign key (Collection_id) 
        references Collection;

    alter table Profile_infos 
        add constraint FK_5dmu1n4w0bx2vaucrqr4mog87 
        foreign key (Profile_id) 
        references Profile;

    alter table Profile_keys 
        add constraint FK_dq11bmpucl4gg88ldggue1qvr 
        foreign key (Profile_id) 
        references Profile;

    create sequence SEQ_WSALIAS_PK;
