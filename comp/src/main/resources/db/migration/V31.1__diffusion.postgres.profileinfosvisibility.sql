UPDATE profile_infos
SET visibility = 0
WHERE name = 'idhal' OR name = 'orcid' OR name = 'viaf' OR name = 'myidref' OR name = 'linkedin' OR name = 'viadeo';

UPDATE profile_infos
SET visibility = 2
WHERE name = 'language' OR name = 'settings';