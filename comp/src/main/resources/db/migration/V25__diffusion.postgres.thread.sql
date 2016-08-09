ALTER TABLE Thread
  DROP COLUMN description;
  
ALTER TABLE Thread
  DROP COLUMN name;
  
ALTER TABLE Message
  DROP COLUMN title;
  
ALTER TABLE Thread
  ADD question varchar(256);
  
ALTER TABLE Thread
  ADD answer varchar(256);
  
ALTER TABLE Thread
  ADD COLUMN title varchar(256);

