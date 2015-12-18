  alter table MetadataFormat
    add column indexable boolean not NULL DEFAULT TRUE;