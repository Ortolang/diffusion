package fr.ortolang.diffusion.oai;

import java.util.List;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.oai.entity.Record;
import fr.ortolang.diffusion.oai.entity.Set;

public interface OaiService extends OrtolangService {

    String SERVICE_NAME = "oai";

    public List<Record> listRecordsByIdentifier(String identifier) throws RecordNotFoundException;
    public List<Record> listRecordsBySet(String set) throws RecordNotFoundException;
    public List<Record> listRecordsByMetadataPrefix(String metadataPrefix, Long from, Long until) throws RecordNotFoundException, OaiServiceException;
    public List<Record> listRecordsByMetadataPrefixAndSetspec(String metadataPrefix, String setSpec, Long from, Long until) throws RecordNotFoundException, OaiServiceException;
    public Record findRecord(String identifier, String metadataPrefix) throws RecordNotFoundException;
    public Record readRecord(String id) throws RecordNotFoundException;
    public Record createRecord(String identifier, String metadataPrefix, long lastModificationDate, String xml);
    public Record createRecord(String identifier, String metadataPrefix, long lastModificationDate, String xml, java.util.Set<String> sets);
    public Record updateRecord(String id, long lastModificationDate, String xml) throws RecordNotFoundException;
    public void deleteRecord(String id) throws RecordNotFoundException;
    public List<Set> listSets();
    public Set findSet(String spec) throws SetNotFoundException;
    public Set createSet(String spec, String name) throws SetAlreadyExistsException;
    public Set readSet(String spec) throws SetNotFoundException;
    public Set updateSet(String spec, String name) throws SetNotFoundException;
    public void deleteSet(String spec) throws SetNotFoundException;
	public void buildRecordsForWorkspace(String wskey, String snapshot) throws OaiServiceException;
}
