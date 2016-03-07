  	ALTER TABLE ReferentialEntity
    ADD COLUMN boost bigint NOT null DEFAULT 1;
    
    UPDATE ReferentialEntity 
	SET boost = 4
	WHERE type = 'LANGUAGE' AND position('"rank"' in content) > 0;