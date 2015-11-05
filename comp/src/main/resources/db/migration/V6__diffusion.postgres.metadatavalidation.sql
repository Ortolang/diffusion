  alter table MetadataFormat
    add column validationNeeded boolean not NULL DEFAULT TRUE;