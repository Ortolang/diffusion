	create table Event (
        id int8 not null,
        date timestamp,
        fromObject varchar(255),
        objectType varchar(255),
        serializedArgs TEXT,
        throwedBy varchar(255),
        type varchar(255),
        primary key (id)
    );

    create table EventFeed (
        id varchar(255) not null,
        description varchar(255),
        name varchar(255),
        size int4 not null,
        version int8 not null,
        primary key (id)
    );

    create table EventFeed_filters (
        EventFeed_id varchar(255) not null,
        eventTypeRE varchar(255),
        fromObjectRE varchar(255),
        id varchar(255),
        objectTypeRE varchar(255),
        throwedByRE varchar(255)
    );
    
    alter table Workspace 
    	add column eventfeed varchar(255);
    
    create index eventDateIndex on Event (date);

    alter table EventFeed_filters 
        add constraint FK_kxev2hwswpdpqsy1dd7d9pmf0 
        foreign key (EventFeed_id) 
        references EventFeed;

    create sequence hibernate_sequence;