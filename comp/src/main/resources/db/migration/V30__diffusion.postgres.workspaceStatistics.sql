CREATE TABLE WorkspaceStatisticValue (
  name varchar(255) not null,
  timestamp int8 not null,
  visits int8 not null,
  uniqueVisitors int8 not null,
  hits int8 not null,
  downloads int8 not null,
  singleDownloads int8 not null,
  primary key (name, timestamp)
);