
    create table ReferentialEntity (
        id varchar(255) not null,
        type varchar(255),
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
	SELECT id, 'ORGANIZATION', content, version FROM organizationentity;
	
    INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'PERSON', content, version FROM personentity;
	
    INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'STATUSOFUSE', content, version FROM statusofuseentity;
	
    INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'LICENSE', content, version FROM licenseentity;
	
	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'ROLE', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "Role" ]' in content) > 0;
	
	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'LANGUAGE', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "Language" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'ANNOTATIONLEVEL', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "AnnotationLevel" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'CORPORASTYLE', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaStyle" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'CORPORATYPE', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'DATATYPE', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaDataType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'FILEENCODING', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaFileEncoding", "ToolFileEncoding" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'FILEFORMAT', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaFormat" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'FILEFORMAT', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "LexiconFormat" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'FILEFORMAT', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "ToolInputData", "ToolOutputData" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'FILEFORMAT', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaFormat", "ToolInputData", "ToolOutputData" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'FILEFORMAT', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaFormat", "LexiconFormat", "ToolInputData", "ToolOutputData" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'LANGUAGETYPE', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaLanguageType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'LANGUAGETYPE', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "LexiconLanguageType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'LANGUAGETYPE', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "CorporaLanguageType", "LexiconLanguageType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'LEXICONANNOTATION', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "LexiconInputType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'LEXICONANNOTATION', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "LexiconDescriptionType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'LEXICONANNOTATION', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "LexiconDescriptionType", "LexiconInputType" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'OPERATINGSYSTEM', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "OperatingSystem" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'PROGRAMMINGLANGUAGE', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "ProgrammingLanguage" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'TOOLFUNCTIONALITY', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "ToolFunctionality" ]' in content) > 0;

	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'TOOLSUPPORT', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "ToolSupport" ]' in content) > 0;
	
	INSERT INTO referentialentity (id, type, content, version)
	SELECT id, 'ORGANIZATION', content, version FROM termentity 
	WHERE position('"compatibilities" : [ "Sponsor" ]' in content) > 0;
	
	DROP TABLE referentielentity;
	DROP TABLE organizationentity;
	DROP TABLE personentity;
	DROP TABLE statusofuseentity;
	DROP TABLE licenseentity;
	DROP TABLE termentity;
