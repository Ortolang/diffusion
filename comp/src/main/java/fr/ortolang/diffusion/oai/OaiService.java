package fr.ortolang.diffusion.oai;

import java.util.List;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.oai.entity.Record;
import fr.ortolang.diffusion.oai.entity.Set;
import fr.ortolang.diffusion.oai.entity.SetRecord;

public interface OaiService extends OrtolangService {

    String SERVICE_NAME = "oai";

    public List<Record> listRecordsByIdentifier(String identifier) throws RecordNotFoundException;
    public List<Record> listRecordsByMetadataPrefix(String metadataPrefix, Long from, Long until) throws RecordNotFoundException, OaiServiceException;
    public Record findRecord(String identifier, String metadataPrefix) throws RecordNotFoundException;
    public Record readRecord(String id) throws RecordNotFoundException;
    public Record createRecord(String identifier, String metadataPrefix, long lastModificationDate, String xml);
    public Record updateRecord(String id, long lastModificationDate, String xml) throws RecordNotFoundException;
    public void deleteRecord(String id) throws RecordNotFoundException;
    public List<Set> listSets();
    public Set findSet(String spec) throws SetNotFoundException;
    public Set createSet(String spec, String name) throws SetAlreadyExistsException;
    public Set readSet(String spec) throws SetNotFoundException;
    public Set updateSet(String spec, String name) throws SetNotFoundException;
    public void deleteSet(String spec) throws SetNotFoundException;
    public List<SetRecord> listSetRecords(String spec);
    public SetRecord createSetRecord(String setSpec, String recordId);
    public SetRecord readSetRecord(String id) throws SetRecordNotFoundException;
    public SetRecord updateSetRecord(String id, String setSpec) throws SetRecordNotFoundException;
    public void deleteSetRecord(String id) throws SetRecordNotFoundException;
}
