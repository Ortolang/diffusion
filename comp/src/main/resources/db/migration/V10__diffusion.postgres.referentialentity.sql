
    create table ReferentialEntity (
        id varchar(255) not null,
        type int4,
        content text,
        version int8 not null,
        primary key (id)
    );

    UPDATE registryentry 
	SET identifier = replace(identifier, 'term', 'entity')
	WHERE identifier like '/referential/term/%';
    
	UPDATE registryentry 
	SET identifier = replace(identifier, 'organization', 'entity')
	WHERE identifier like '/referential/organization/%';
	
	UPDATE registryentry 
	SET identifier = replace(identifier, 'person', 'entity')
	WHERE identifier like '/referential/person/%';
	
	UPDATE registryentry 
	SET identifier = replace(identifier, 'statusofuse', 'entity')
	WHERE identifier like '/referential/statusofuse/%';
		
	UPDATE registryentry 
	SET identifier = replace(identifier, 'license', 'entity')
	WHERE identifier like '/referential/license/%';
	
    INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 0, content, version FROM organizationentity;
	
    INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 1, content, version FROM personentity;
	
    INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 2, content, version FROM statusofuseentity;
	
    INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 3, content, version FROM licenseentity;
	
	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 4, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "Role" ]' in content) > 0;
	
	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 5, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "Language" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 6, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "AnnotationLevel" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 7, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaStyle" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 8, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 9, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaDataType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 10, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaFileEncoding", "ToolFileEncoding" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 11, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaFormat" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 11, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "LexiconFormat" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 11, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "ToolInputData", "ToolOutputData" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 11, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaFormat", "ToolInputData", "ToolOutputData" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 11, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaFormat", "LexiconFormat", "ToolInputData", "ToolOutputData" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 12, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaLanguageType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 12, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "LexiconLanguageType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 12, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaLanguageType", "LexiconLanguageType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 13, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "LexiconInputType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 13, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "LexiconDescriptionType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 13, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "LexiconDescriptionType", "LexiconInputType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 14, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "OperatingSystem" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 15, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "ProgrammingLanguage" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 16, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "ToolFunctionality" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 17, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "ToolSupport" ]' in content) > 0;
	
	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 0, content, version FROM termentity 
	WHERE position('"compatibilities" : [ "Sponsor" ]' in content) > 0;
	
	DROP TABLE referentielentity;
	DROP TABLE organizationentity;
	DROP TABLE personentity;
	DROP TABLE statusofuseentity;
	DROP TABLE licenseentity;
	DROP TABLE termentity;
