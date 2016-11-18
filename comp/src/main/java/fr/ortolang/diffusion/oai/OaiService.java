package fr.ortolang.diffusion.oai;

import fr.ortolang.diffusion.OrtolangService;
import fr.ortolang.diffusion.oai.entity.Record;

public interface OaiService extends OrtolangService {

    String SERVICE_NAME = "oai";

    public Record readRecord(String key) throws RecordNotFoundException;
    public Record createRecord(String key, String metadataPrefix, long lastModificationDate, String xml);
    public void deleteRecord(String key) throws RecordNotFoundException;
}
