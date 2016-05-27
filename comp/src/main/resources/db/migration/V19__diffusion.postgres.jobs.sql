create table Job (
  id int8 not null,
  action varchar(255),
  target varchar(255),
  timestamp int8 not null,
  type varchar(255),
  primary key (id)
);

create table Job_parameters (
  Job_id int8 not null,
  parameters varchar(255),
  parameters_KEY varchar(255),
  primary key (Job_id, parameters_KEY)
);

alter table Job_parameters
add constraint FK_bldcln9mms1bu1meoc4k6ditt
foreign key (Job_id)
references Job;