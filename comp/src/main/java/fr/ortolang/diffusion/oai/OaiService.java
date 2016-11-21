package fr.ortolang.diffusion.oai;

import java.util.List;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.oai.entity.Record;

public interface OaiService extends OrtolangService {

    String SERVICE_NAME = "oai";

    public List<Record> listRecordsByIdentifier(String identifier) throws RecordNotFoundException;
    public List<Record> listRecordsByMetadataPrefix(String metadataPrefix, Long from, Long until) throws RecordNotFoundException, OaiServiceException;
    public Record findRecord(String identifier, String metadataPrefix) throws RecordNotFoundException;
    public Record readRecord(String id) throws RecordNotFoundException;
    public Record createRecord(String identifier, String metadataPrefix, long lastModificationDate, String xml);
    public void deleteRecord(String id) throws RecordNotFoundException;
}
