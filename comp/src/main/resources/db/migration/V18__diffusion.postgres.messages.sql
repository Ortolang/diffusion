	create table Message (
        id varchar(255) not null,
        body text,
        date timestamp,
        parent varchar(255),
        thread varchar(255),
        title varchar(1000),
        version int8 not null,
        primary key (id)
    );

    create table Message_attachments (
        Message_id varchar(255) not null,
        hash varchar(255),
        name varchar(255),
        size int8 not null,
        type varchar(255),
        primary key (Message_id, size)
    );

    create table Thread (
        id varchar(255) not null,
        description text,
        lastActivity timestamp,
        name varchar(1000),
        version int8 not null,
        workspace varchar(255),
        primary key (id)
    );

    create index messageDateIndex on Message (date);

    create index messageThreadIndex on Message (thread);

    create index threadLastActivityIndex on Thread (lastActivity);

    create index threadWorkspaceIndex on Thread (workspace);

    alter table Message_attachments 
        add constraint FK_9xow20lsikl69n9tfi4iw6q6v 
        foreign key (Message_id) 
        references Message;

        
        